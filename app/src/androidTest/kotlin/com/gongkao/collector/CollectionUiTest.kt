package com.gongkao.collector

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gongkao.collector.data.db.SourceRecordEntity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyCollectionExplainsHowToAddTheFirstSource() {
        composeRule.setContent {
            MaterialTheme {
                CollectorApp(
                    page = AppPage.COLLECTION,
                    sources = emptyList(),
                    selectedSource = null,
                    statusMessage = null,
                    onShowCollection = {},
                    onShowIntake = {},
                    onOpenSource = {},
                    onPaste = {},
                    onImageSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("还没有收集内容").assertIsDisplayed()
        composeRule.onNodeWithText("添加").assertIsDisplayed()
    }

    @Test
    fun tappingACollectionItemOpensItsOriginalTextDetail() {
        val source = textSource()
        composeRule.setContent {
            var page by remember { mutableStateOf(AppPage.COLLECTION) }
            var selected by remember { mutableStateOf<SourceRecordEntity?>(null) }
            MaterialTheme {
                CollectorApp(
                    page = page,
                    sources = listOf(source),
                    selectedSource = selected,
                    statusMessage = null,
                    onShowCollection = { page = AppPage.COLLECTION },
                    onShowIntake = { page = AppPage.INTAKE },
                    onOpenSource = {
                        selected = it
                        page = AppPage.DETAIL
                    },
                    onPaste = {},
                    onImageSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("第一道测试题").performClick()
        composeRule.onNodeWithText("原始内容").assertIsDisplayed()
        composeRule.onNodeWithText("第一道测试题\n第二行题干").assertIsDisplayed()
        composeRule.onNodeWithText("返回收集箱").assertIsDisplayed()
    }

    private fun textSource() = SourceRecordEntity(
        id = "source-ui-1",
        kind = "TEXT",
        originalText = "第一道测试题\n第二行题干",
        imagePath = null,
        sha256 = null,
        mimeType = "text/plain",
        status = "SAVED",
        createdAt = 1L,
        deletedAt = null,
    )
}
