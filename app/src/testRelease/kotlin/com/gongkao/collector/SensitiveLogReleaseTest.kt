package com.gongkao.collector

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class SensitiveLogReleaseTest {
    @Test
    fun `release build never evaluates or emits sensitive debug message`() {
        var evaluated = false
        ShadowLog.clear()

        SensitiveLog.debug {
            evaluated = true
            "question-body api-key complete-ai-response"
        }

        assertFalse(DEBUG_LOGS_ENABLED)
        assertFalse(evaluated)
        assertTrue(ShadowLog.getLogs().isEmpty())
    }
}
