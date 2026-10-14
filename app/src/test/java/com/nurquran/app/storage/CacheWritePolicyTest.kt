package com.nurquran.app.storage

import org.junit.Assert.*
import org.junit.Test

class CacheWritePolicyTest {
    @Test fun onlyWritesValidatedNonEmptyPayloads() {
        assertTrue(CacheWritePolicy.shouldWrite("{\"valid\":true}", sourceValidated = true))
        assertFalse(CacheWritePolicy.shouldWrite("[]", sourceValidated = true))
        assertFalse(CacheWritePolicy.shouldWrite("{}", sourceValidated = true))
        assertFalse(CacheWritePolicy.shouldWrite("null", sourceValidated = true))
        assertFalse(CacheWritePolicy.shouldWrite("{\"valid\":true}", sourceValidated = false))
        assertFalse(CacheWritePolicy.shouldWrite("  ", sourceValidated = true))
        assertFalse(CacheWritePolicy.shouldWrite(null, sourceValidated = true))
    }
}
