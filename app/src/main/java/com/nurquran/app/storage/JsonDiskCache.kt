package com.nurquran.app.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.reflect.Type

/** Small private-app cache. Writes only validated non-empty payloads using a temp-file replacement attempt. */
class JsonDiskCache(context: Context) {
    private val dir = File(context.filesDir, "api-cache").apply { mkdirs() }
    private val gson = Gson()

    suspend inline fun <reified T> read(key: String): T? = read(key, object : TypeToken<T>() {}.type)

    suspend fun <T> read(key: String, type: Type): T? = withContext(Dispatchers.IO) {
        val file = cacheFile(key)
        if (!file.isFile || file.length() <= 1L) return@withContext null
        runCatching { gson.fromJson<T>(file.readText(Charsets.UTF_8), type) }.getOrNull()
    }

    suspend fun write(key: String, value: Any, valid: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        if (!valid) return@withContext false
        val json = runCatching { gson.toJson(value) }.getOrNull() ?: return@withContext false
        if (!CacheWritePolicy.shouldWrite(json, sourceValidated = valid)) return@withContext false
        val file = cacheFile(key)
        val temp = File(dir, "${file.name}.tmp")
        return@withContext runCatching {
            temp.writeText(json, Charsets.UTF_8)
            if (!temp.renameTo(file)) {
                file.writeText(json, Charsets.UTF_8)
                temp.delete()
            }
            true
        }.getOrDefault(false)
    }

    suspend fun deleteAll(): Boolean = withContext(Dispatchers.IO) {
        dir.listFiles()?.forEach { it.delete() }
        true
    }

    private fun cacheFile(key: String): File {
        val safeName = key.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(dir, "$safeName.json")
    }
}
