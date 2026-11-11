package com.setotower.downloader.util

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Short-lived, bounded cache for native playback.
 *
 * Media3 downloads the requested ranges/segments into this cache and reads
 * them locally thereafter. It is deliberately capped and lives under the
 * app cache directory, so Android may reclaim it and the player can remove
 * it when the viewing session ends.
 */
object LocalPlaybackCache {
    private const val DIRECTORY_NAME = "setotower-player-cache"
    private const val MAX_BYTES = 64L * 1024L * 1024L

    @Volatile
    private var cache: SimpleCache? = null

    @Synchronized
    private fun getCache(context: Context): SimpleCache {
        cache?.let { return it }
        val directory = File(context.cacheDir, DIRECTORY_NAME)
        return SimpleCache(
            directory,
            LeastRecentlyUsedCacheEvictor(MAX_BYTES),
            StandaloneDatabaseProvider(context.applicationContext)
        ).also { cache = it }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun dataSourceFactory(context: Context): DataSource.Factory {
        val upstream = DefaultDataSource.Factory(
            context,
            DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
        )
        val cache = getCache(context.applicationContext)
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstream)
            .setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(cache))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    @Synchronized
    fun clear(context: Context) {
        cache?.let {
            runCatching { it.release() }
            cache = null
        }
        File(context.cacheDir, DIRECTORY_NAME).deleteRecursively()
    }
}
