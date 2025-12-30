package com.autoclicker.app.util

import android.graphics.Bitmap
import android.util.LruCache

/**
 * LRU кэш для изображений для оптимизации использования памяти
 * Автоматически освобождает старые изображения при нехватке памяти
 */
object ImageCache {
    
    // Используем 1/8 доступной памяти для кэша
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    
    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            // Размер в килобайтах
            return bitmap.byteCount / 1024
        }
        
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: Bitmap,
            newValue: Bitmap?
        ) {
            // Освобождаем память при удалении из кэша
            if (evicted && oldValue != newValue) {
                oldValue.recycle()
            }
        }
    }
    
    /**
     * Добавить изображение в кэш
     */
    fun put(key: String, bitmap: Bitmap) {
        if (get(key) == null) {
            memoryCache.put(key, bitmap)
        }
    }
    
    /**
     * Получить изображение из кэша
     */
    fun get(key: String): Bitmap? {
        return memoryCache.get(key)
    }
    
    /**
     * Удалить изображение из кэша
     */
    fun remove(key: String) {
        memoryCache.remove(key)?.recycle()
    }
    
    /**
     * Очистить весь кэш
     */
    fun clear() {
        memoryCache.evictAll()
    }
    
    /**
     * Получить информацию о кэше
     */
    fun getInfo(): CacheInfo {
        return CacheInfo(
            size = memoryCache.size(),
            maxSize = memoryCache.maxSize(),
            hitCount = memoryCache.hitCount(),
            missCount = memoryCache.missCount(),
            evictionCount = memoryCache.evictionCount()
        )
    }
    
    data class CacheInfo(
        val size: Int,
        val maxSize: Int,
        val hitCount: Int,
        val missCount: Int,
        val evictionCount: Int
    ) {
        val hitRate: Float
            get() = if (hitCount + missCount > 0) {
                hitCount.toFloat() / (hitCount + missCount)
            } else 0f
    }
}

