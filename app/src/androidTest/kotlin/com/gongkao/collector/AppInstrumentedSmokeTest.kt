package com.gongkao.collector

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppInstrumentedSmokeTest {
    @Test
    fun packageNameMatchesApplication() {
        assertEquals(
            "com.gongkao.collector.debug",
            InstrumentationRegistry.getInstrumentation().targetContext.packageName,
        )
    }
}
