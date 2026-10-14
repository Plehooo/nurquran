package com.nurquran.app.data

import org.junit.Assert.*
import org.junit.Test

class HadithPagingLogicTest {
    @Test fun knownLastPageDoesNotAllowExtraRequestEvenWhenFull() {
        assertNull(HadithPagingLogic.nextPage(currentPage = 1, totalPages = 1, receivedCount = 20, pageSize = 20))
        assertEquals(2, HadithPagingLogic.nextPage(currentPage = 1, totalPages = 3, receivedCount = 20, pageSize = 20))
    }

    @Test fun absentPageMetadataUsesShortPageAsEndMarker() {
        assertEquals(2, HadithPagingLogic.nextPage(1, null, 20, 20))
        assertNull(HadithPagingLogic.nextPage(2, null, 7, 20))
    }

    @Test fun retryPayloadDoesNotDuplicateKnownHadithNumbers() {
        val first = HadithItem(number = 1, arabic = "AR_FIXTURE_1", translation = "ID_FIXTURE_1")
        val next = HadithItem(number = 2, arabic = "AR_FIXTURE_2", translation = "ID_FIXTURE_2")
        assertEquals(listOf(first, next), HadithPagingLogic.mergeUnique(listOf(first), listOf(first, next)))
    }

    @Test fun malformedEmptyItemsAreIgnoredByMerge() {
        val item = HadithItem(number = 1, translation = "ID_FIXTURE")
        assertEquals(listOf(item), HadithPagingLogic.mergeUnique(emptyList(), listOf(item, HadithItem())))
    }
}
