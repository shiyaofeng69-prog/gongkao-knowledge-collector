package com.gongkao.collector

import android.util.Log

object SensitiveLog {
    private const val TAG = "GongkaoCollector"

    fun debug(message: () -> String) {
        if (DEBUG_LOGS_ENABLED) {
            Log.d(TAG, message())
        }
    }
}
