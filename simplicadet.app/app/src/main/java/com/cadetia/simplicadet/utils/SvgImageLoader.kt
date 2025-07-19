package com.cadetia.simplicadet.utils

import android.content.Context
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class SvgImageLoader {
    companion object {
        @Volatile
        private var INSTANCE: ImageLoader? = null

        @JvmStatic
        fun get(context: Context): ImageLoader {
            return INSTANCE ?: synchronized(this) {
                val instance = create(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        @JvmStatic
        private fun create(context: Context): ImageLoader = ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("svg_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()
            }
            .crossfade(150)
            .build()
    }

}