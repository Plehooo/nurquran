package com.nurquran.app.data

import org.junit.Assert.*
import org.junit.Test

class HadithJsonParserTest {
    @Test fun parsesBooksArrayAndEnvelope() {
        val books = """[{"name":"Fixture Book","slug":"fixture-book","total":2}]"""
        assertEquals("fixture-book", HadithJsonParser.parseBooks(books).single().slug)

        val wrapped = """{"data":[{"name":"Fixture Book","slug":"fixture-book","total":2}]}"""
        assertEquals(2, HadithJsonParser.parseBooks(wrapped).single().total)
    }

    @Test fun parsesPaginatedFixture() {
        val json = """{
          "name":"Fixture Book","slug":"fixture-book","total":2,
          "pagination":{"totalItems":2,"currentPage":1,"pageSize":20,"totalPages":1,"pages":[1]},
          "items":[{"number":1,"arab":"AR_FIXTURE","id":"ID_FIXTURE"}]
        }"""
        val page = HadithJsonParser.parsePage(json, expectedPage = 1)
        assertEquals("fixture-book", page.slug)
        assertEquals("ID_FIXTURE", page.items.single().translation)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyPage() {
        HadithJsonParser.parsePage(
            """{"name":"Fixture Book","slug":"fixture-book","total":0,"items":[]}""",
            expectedPage = 1
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsWrongPaginationPage() {
        HadithJsonParser.parsePage(
            """{"name":"Fixture Book","slug":"fixture-book","total":1,"pagination":{"currentPage":2},"items":[{"number":1,"id":"fixture"}]}""",
            expectedPage = 1
        )
    }
}
