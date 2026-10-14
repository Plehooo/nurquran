package com.nurquran.app.data

/** Pure pagination helpers so retry/page-advance behavior can be unit-tested without networking. */
object HadithPagingLogic {
    /** Returns the next one-based page, or null when a known final page/short page has been reached. */
    fun nextPage(currentPage: Int, totalPages: Int?, receivedCount: Int, pageSize: Int): Int? {
        if (currentPage < 1 || receivedCount < 0 || pageSize < 1) return null
        val knownTotal = totalPages?.takeIf { it > 0 }
        return if (knownTotal != null) {
            if (currentPage < knownTotal) currentPage + 1 else null
        } else {
            if (receivedCount >= pageSize) currentPage + 1 else null
        }
    }

    /** Merge retry/page payloads without repeating an item already present in the visible list. */
    fun mergeUnique(existing: List<HadithItem>, incoming: List<HadithItem>): List<HadithItem> {
        val merged = LinkedHashMap<String, HadithItem>()
        (existing + incoming).forEach { item ->
            val key = item.number?.let { "number:$it" }
                ?: "text:${item.arabic.orEmpty()}||${item.translation.orEmpty()}"
            if (key != "text:||" && !merged.containsKey(key)) merged[key] = item
        }
        return merged.values.toList()
    }
}
