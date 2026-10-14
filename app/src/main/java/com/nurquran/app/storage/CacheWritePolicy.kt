package com.nurquran.app.storage

object CacheWritePolicy {
    fun shouldWrite(json: String?, sourceValidated: Boolean): Boolean {
        if (!sourceValidated || json.isNullOrBlank()) return false
        val trimmed = json.trim()
        return trimmed != "null" && trimmed != "[]" && trimmed != "{}"
    }
}
