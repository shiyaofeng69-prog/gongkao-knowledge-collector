package com.gongkao.collector.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseInstrumentedSmokeTest {
    @Test
    fun databaseCanBeConstructedOnDevice() {
        val database = GongkaoDatabase.create(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            name = "gate00-device-test.db",
        )
        assertNotNull(database)
        database.close()
    }
}
