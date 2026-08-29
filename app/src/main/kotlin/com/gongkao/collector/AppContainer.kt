package com.gongkao.collector

import android.content.Context
import com.gongkao.collector.data.db.GongkaoDatabase
import com.gongkao.collector.data.intake.SourceIntakeRepository

class AppContainer(context: Context) {
    val database: GongkaoDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        GongkaoDatabase.create(context)
    }

    val sourceIntakeRepository: SourceIntakeRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        SourceIntakeRepository(
            sourceDirectory = context.filesDir.resolve("sources"),
            dao = database.foundationDao(),
        )
    }
}
