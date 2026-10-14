package com.nurquran.app.data

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken

/** Defensively parses the documented API envelopes and refuses empty/mismatched pagination payloads. */
object HadithJsonParser {
    private val gson = Gson()
    private val booksType = object : TypeToken<List<HadithBook>>() {}.type

    fun parseBooks(rawJson: String): List<HadithBook> {
        val root = JsonParser.parseString(rawJson)
        val value = unwrap(root)
        require(value.isJsonArray) { "Format daftar kitab bukan array" }
        val books = gson.fromJson<List<HadithBook>>(value, booksType)
        require(!books.isNullOrEmpty() && books.all { it.name.isNotBlank() && it.slug.isNotBlank() && it.total >= 0 }) {
            "Daftar kitab kosong/tidak valid"
        }
        return books
    }

    fun parsePage(rawJson: String, expectedPage: Int): HadithPage {
        val root = JsonParser.parseString(rawJson)
        val value = unwrap(root)
        require(value.isJsonObject) { "Format halaman hadis bukan object" }
        val result = gson.fromJson(value, HadithPage::class.java)
        require(result != null && result.slug.isNotBlank() && result.items.isNotEmpty()) { "Halaman hadis kosong/tidak valid" }
        require(result.items.all { !it.arabic.isNullOrBlank() || !it.translation.isNullOrBlank() }) { "Isi hadis tidak memiliki teks" }
        require(result.pagination == null || result.pagination.currentPage == expectedPage) { "Nomor halaman API tidak cocok" }
        return result
    }

    private fun unwrap(root: JsonElement): JsonElement {
        if (root.isJsonObject) {
            val objectRoot = root.asJsonObject
            val data = objectRoot.get("data")
            if (data != null && (data.isJsonArray || data.isJsonObject)) return data
        }
        return root
    }
}
