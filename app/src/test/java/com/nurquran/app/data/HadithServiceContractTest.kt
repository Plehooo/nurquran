package com.nurquran.app.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HadithServiceContractTest {
    @Test fun pageAndLimitAreAlwaysExplicitInRequest() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse().setResponseCode(200).setBody(
                    """{"name":"Fixture Book","slug":"fixture-book","total":1,"pagination":{"totalItems":1,"currentPage":3,"pageSize":20,"totalPages":3,"pages":[1,2,3]},"items":[{"number":41,"arab":"AR_FIXTURE","id":"ID_FIXTURE"}]}"""
                )
            )
            val service = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(HadithService::class.java)

            val payload = service.getPage("fixture-book", page = 3, limit = 20)
            val request = server.takeRequest()
            assertEquals("/hadith/fixture-book?page=3&limit=20", request.path)
            assertEquals("fixture-book", HadithJsonParser.parsePage(payload.toString(), 3).slug)
        } finally {
            server.shutdown()
        }
    }
}
