package com.setotower.downloader.util

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

// Bump the key so older app builds cannot feed an incompatible snapshot into
// the Movies screen after an APK update.
private const val CINEB_HOME_CACHE = "cineb_home_snapshot_v2"
private const val CINEB_HOME_CACHE_TTL_MS = 30 * 60 * 1000L

data class CinebMedia(
    val id: Int,
    val title: String,
    val type: String,
    val overview: String = "",
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val releaseDate: String = "",
    val voteAverage: Double = 0.0,
    val voteCount: Int = 0,
    val runtime: Int = 0,
    val seasons: Int = 0
) {
    val posterUrl: String get() = posterPath?.let { "https://image.tmdb.org/t/p/w342$it" }.orEmpty()
    val backdropUrl: String get() = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }.orEmpty()
    val cinebUrl: String
        get() = "https://cineb.setotower.com/${if (type == "tv") "series" else "movie"}/${slugify(title)}-$id/"
}

data class CinebSource(val name: String, val label: String, val encodedUrl: String)

data class CinebHomeSnapshot(
    val fetchedAt: Long,
    val groups: List<List<CinebMedia>>
)

object CinebCatalogCache {
    @Volatile private var memory: CinebHomeSnapshot? = null

    fun read(context: Context): CinebHomeSnapshot? {
        memory?.let { return it }
        val raw = context.getSharedPreferences("cineb_cache", Context.MODE_PRIVATE)
            .getString(CINEB_HOME_CACHE, null)
            ?: return null
        return runCatching {
            Gson().fromJson(raw, CinebHomeSnapshot::class.java).takeIf { it.groups.size >= 5 }
        }
            .getOrNull()
            ?.also { memory = it }
    }

    fun write(context: Context, snapshot: CinebHomeSnapshot) {
        memory = snapshot
        context.getSharedPreferences("cineb_cache", Context.MODE_PRIVATE)
            .edit()
            .putString(CINEB_HOME_CACHE, Gson().toJson(snapshot))
            .apply()
    }

    fun isFresh(snapshot: CinebHomeSnapshot): Boolean =
        System.currentTimeMillis() - snapshot.fetchedAt < CINEB_HOME_CACHE_TTL_MS
}

object CinebCatalogApi {
    private const val BASE_URL = "https://cineb.setotower.com/DualEngineAPI.php"
    private val client = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun discover(type: String, page: Int = 1): List<CinebMedia> = request("discover", mapOf("type" to type, "page" to page.toString())).mediaResults(type)

    suspend fun prefetchHome(context: Context): CinebHomeSnapshot? = withContext(Dispatchers.IO) {
        CinebCatalogCache.read(context)?.takeIf { CinebCatalogCache.isFresh(it) }?.let { return@withContext it }
        runCatching {
            val popular = discover("movie")
            val tv = discover("tv")
            val trending = discover("movie", 2)
            val recommended = (popular.drop(4) + tv.take(10)).distinctBy { it.id to it.type }
            CinebHomeSnapshot(
                fetchedAt = System.currentTimeMillis(),
                groups = listOf(popular, recommended, trending, tv, popular + tv)
            ).also { CinebCatalogCache.write(context, it) }
        }.getOrNull()
    }

    suspend fun search(query: String): List<CinebMedia> = request("search", mapOf("query" to query)).mediaResults("movie")

    suspend fun details(media: CinebMedia): CinebMedia = request("details", mapOf("id" to media.id.toString(), "type" to media.type)).toMedia(media.type)

    suspend fun sources(media: CinebMedia, season: Int = 1, episode: Int = 1): List<CinebSource> = withContext(Dispatchers.IO) {
        val json = request("stream_sources", mapOf("id" to media.id.toString(), "type" to media.type, "s" to season.toString(), "e" to episode.toString()))
        json.array("servers")?.mapNotNull { item ->
            val server = item.asJsonObject
            val encoded = server.string("data") ?: return@mapNotNull null
            CinebSource(
                name = server.string("name") ?: "Source",
                label = server.string("label") ?: server.string("name") ?: "Source",
                encodedUrl = encoded
            )
        }.orEmpty()
    }

    /** The short-lived Cineb server pipe consumed by the native Media3 player. */
    fun streamUrl(media: CinebMedia, encodedSource: String, season: Int = 1, episode: Int = 1): String {
        return BASE_URL.toHttpUrl().newBuilder()
            .addQueryParameter("action", "download_stream")
            .addQueryParameter("source", encodedSource)
            .addQueryParameter("type", media.type)
            .addQueryParameter("s", season.toString())
            .addQueryParameter("e", episode.toString())
            .addQueryParameter("title", media.title)
            .build()
            .toString()
    }

    fun decodeSource(encodedSource: String): String? = runCatching {
        Base64.decode(encodedSource, Base64.DEFAULT).toString(Charsets.UTF_8)
    }.getOrNull()?.takeIf { it.startsWith("https://") || it.startsWith("http://") }

    suspend fun seasonEpisodes(media: CinebMedia, season: Int): List<CinebEpisode> = withContext(Dispatchers.IO) {
        val json = request("season_details", mapOf("id" to media.id.toString(), "s" to season.toString()))
        json.array("episodes")?.map { item ->
            val episode = item.asJsonObject
            CinebEpisode(
                number = episode.int("episode_number"),
                title = episode.string("name") ?: "Episode ${episode.int("episode_number")}",
                overview = episode.string("overview").orEmpty(),
                stillPath = episode.string("still_path")
            )
        }.orEmpty()
    }

    private suspend fun request(action: String, params: Map<String, String>): JsonObject = withContext(Dispatchers.IO) {
        val builder = BASE_URL.toHttpUrl().newBuilder().addQueryParameter("action", action)
        params.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        val response = client.newCall(Request.Builder().url(builder.build()).get().build()).execute()
        response.use {
            val body = it.body?.string().orEmpty()
            if (!it.isSuccessful) throw IOException("Cineb returned HTTP ${it.code}.")
            val json = JsonParser.parseString(body).asJsonObject
            if (json.string("status") == "error") throw IOException(json.string("message") ?: "Cineb request failed.")
            json
        }
    }

    private fun JsonObject.mediaResults(fallbackType: String): List<CinebMedia> = array("results")?.map {
        val item = it.asJsonObject
        item.toMedia(item.string("media_type") ?: fallbackType)
    }.orEmpty()

    private fun JsonObject.toMedia(fallbackType: String): CinebMedia = CinebMedia(
        id = int("id"),
        title = string("title") ?: string("name") ?: "Untitled",
        type = string("media_type") ?: fallbackType,
        overview = string("overview").orEmpty(),
        posterPath = string("poster_path"),
        backdropPath = string("backdrop_path"),
        releaseDate = string("release_date") ?: string("first_air_date") ?: "",
        voteAverage = number("vote_average"),
        voteCount = int("vote_count"),
        runtime = int("runtime"),
        seasons = int("number_of_seasons")
    )

    private fun JsonObject.string(key: String): String? = get(key)?.takeUnless { it.isJsonNull }?.asString
    private fun JsonObject.int(key: String): Int = get(key)?.takeUnless { it.isJsonNull }?.asInt ?: 0
    private fun JsonObject.number(key: String): Double = get(key)?.takeUnless { it.isJsonNull }?.asDouble ?: 0.0
    private fun JsonObject.array(key: String): JsonArray? = get(key)?.takeUnless { it.isJsonNull }?.asJsonArray
}

data class CinebEpisode(val number: Int, val title: String, val overview: String, val stillPath: String?)

private fun slugify(value: String): String = value.lowercase()
    .replace(Regex("[^a-z0-9]+"), "-")
    .trim('-')
    .ifBlank { "title" }
