package com.gongkao.collector

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntakeUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun blankPasteShowsClearMessage() {
        composeRule.onNodeWithText("保存文字").performClick()
        composeRule.onNodeWithText("没有可保存的文字，请先粘贴内容").assertIsDisplayed()
    }

    @Test
    fun multipleShareIsRejectedWithPhaseOneMessage() {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.handleIncomingIntent(Intent(Intent.ACTION_SEND_MULTIPLE).setType("image/png"))
        }
        composeRule.onNodeWithText("一期每次只能导入一张图片").assertIsDisplayed()
    }
}
