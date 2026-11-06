package com.setotower.downloader.util

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.setotower.downloader.database.models.ChapterItem
import com.setotower.downloader.database.models.Format
import com.setotower.downloader.database.models.ResultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/** Small client for the public Setotower inspection contract. */
object SetotowerApi {
    private const val BASE_URL = "https://downloader.setotower.com"
    private val client = OkHttpClient.Builder()
        .callTimeout(java.time.Duration.ofSeconds(90))
        .build()
    private val downloadClient = client.newBuilder()
        // A media download can legitimately take longer than the inspection
        // request. Keep connection/read safeguards, but do not cap the whole
        // transfer at 90 seconds.
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun inspectCineb(url: String): ResultItem = withContext(Dispatchers.IO) {
        val endpoint = "$BASE_URL/api/info".toHttpUrl().newBuilder()
            .addQueryParameter("url", url)
            .build()
        val request = Request.Builder().url(endpoint).get().build()
        executeWithRetry(client, request).use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = runCatching { JsonParser.parseString(body).asJsonObject.get("error")?.asString }.getOrNull()
                throw IOException(message ?: "Setotower could not inspect this Cineb link.")
            }
            fromInfoJson(url, JsonParser.parseString(body).asJsonObject)
        }
    }

    suspend fun downloadCineb(
        url: String,
        quality: String,
        destination: File,
        onProgress: (Int) -> Unit
    ): Long = withContext(Dispatchers.IO) {
        val endpoint = "$BASE_URL/api/download".toHttpUrl().newBuilder()
            .addQueryParameter("url", url)
            .addQueryParameter("type", "mp4")
            .addQueryParameter("quality", quality.ifBlank { "best" })
            .build()
        val request = Request.Builder().url(endpoint).get().build()
        executeWithRetry(downloadClient, request).use { response ->
            if (!response.isSuccessful) {
                val message = response.body?.string().orEmpty()
                val detail = runCatching { JsonParser.parseString(message).asJsonObject.get("error")?.asString }.getOrNull()
                throw IOException(detail ?: "Setotower could not download this Cineb link.")
            }
            val body = response.body ?: throw IOException("Setotower returned an empty Cineb response.")
            val contentType = body.contentType()?.toString()?.lowercase().orEmpty()
            if (contentType.contains("mpegurl") || contentType.contains("m3u8") || contentType.contains("text/html")) {
                throw IOException("Setotower returned a playlist or webpage instead of an MP4 video.")
            }
            val total = body.contentLength()
            destination.parentFile?.mkdirs()
            val partial = File(destination.parentFile, "${destination.name}.part")
            var copied = 0L
            var lastProgress = -1
            var complete = false
            try {
                body.byteStream().use { rawInput ->
                    BufferedInputStream(rawInput).use { input ->
                        input.mark(64)
                        val signature = ByteArray(8)
                        input.read(signature)
                        input.reset()
                        val signatureText = signature.toString(Charsets.UTF_8).trimStart()
                        if (signatureText.startsWith("#EXTM3U") || signatureText.startsWith("<html", ignoreCase = true)) {
                            throw IOException("Setotower returned a playlist or webpage instead of an MP4 video.")
                        }
                        FileOutputStream(partial).use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                output.write(buffer, 0, count)
                                copied += count
                                if (total > 0) {
                                    val progress = ((copied * 100) / total).toInt().coerceIn(0, 100)
                                    if (progress != lastProgress) {
                                        lastProgress = progress
                                        onProgress(progress)
                                    }
                                }
                            }
                        }
                    }
                }
                if (copied == 0L) throw IOException("Setotower returned an empty Cineb file.")
                if (destination.exists() && !destination.delete()) {
                    throw IOException("Could not replace the existing Cineb download.")
                }
                if (!partial.renameTo(destination)) {
                    throw IOException("Could not finalize the Cineb download.")
                }
                complete = true
                onProgress(100)
                copied
            } finally {
                if (!complete) partial.delete()
            }
        }
    }

    private suspend fun executeWithRetry(httpClient: OkHttpClient, request: Request): Response {
        var attempt = 0
        while (true) {
            currentCoroutineContext().ensureActive()
            val response = httpClient.newCall(request).execute()
            val retryable = response.code == 429 || response.code == 502 || response.code == 503 || response.code == 504
            if (!retryable || attempt >= 2) return response
            response.close()
            delay(400L shl attempt)
            attempt++
        }
    }

    private fun fromInfoJson(url: String, data: JsonObject): ResultItem {
        val title = data.string("title") ?: "Cineb download"
        val formats = mutableListOf<Format>()
        data.array("video")?.forEach { item ->
            val option = item.asJsonObject
            val value = option.string("value") ?: return@forEach
            val label = option.string("label") ?: "$value MP4"
            formats += Format(
                format_id = value,
                container = "mp4",
                vcodec = "h264",
                acodec = "aac",
                format_note = label
            )
        }
        if (formats.isEmpty()) {
            formats += Format(
                format_id = "best",
                container = "mp4",
                vcodec = "h264",
                acodec = "aac",
                format_note = "Highest available MP4"
            )
        }
        return ResultItem(
            id = 0,
            url = url,
            title = title,
            author = data.string("uploader") ?: "Cineb",
            duration = data.string("duration") ?: "",
            thumb = data.string("thumbnail") ?: "",
            website = "Cineb",
            playlistTitle = "",
            formats = formats,
            urls = "",
            chapters = emptyList<ChapterItem>()
        )
    }

    private fun JsonObject.string(name: String): String? = get(name)?.takeUnless { it.isJsonNull }?.asString
    private fun JsonObject.array(name: String) = get(name)?.takeUnless { it.isJsonNull }?.asJsonArray
}
