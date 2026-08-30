package com.gongkao.collector

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.gongkao.collector.data.db.SourceRecordEntity
import com.gongkao.collector.data.intake.IntakeError
import com.gongkao.collector.data.intake.IntakeResult
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private var page by mutableStateOf(AppPage.COLLECTION)
    private var sources by mutableStateOf(emptyList<SourceRecordEntity>())
    private var selectedSource by mutableStateOf<SourceRecordEntity?>(null)
    private var statusMessage by mutableStateOf<Int?>(null)

    private val container
        get() = (application as GongkaoApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CollectorApp(
                    page = page,
                    sources = sources,
                    selectedSource = selectedSource,
                    statusMessage = statusMessage,
                    onShowCollection = { page = AppPage.COLLECTION },
                    onShowIntake = { page = AppPage.INTAKE },
                    onOpenSource = { source ->
                        selectedSource = source
                        page = AppPage.DETAIL
                    },
                    onPaste = ::importText,
                    onImageSelected = ::importImage,
                )
            }
        }
        if (savedInstanceState == null) handleIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        reloadSources()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun reloadSources() {
        lifecycleScope.launch { sources = container.sourceBrowseRepository.listSources() }
    }

    private fun importText(text: String) {
        lifecycleScope.launch { showResult(container.sourceIntakeRepository.importText(text)) }
    }

    private fun importImage(uri: Uri) {
        lifecycleScope.launch {
            val result = container.sourceIntakeRepository.importImage(contentResolver.getType(uri)) {
                contentResolver.openInputStream(uri)
            }
            showResult(result)
        }
    }

    internal fun handleIncomingIntent(incoming: Intent?) {
        when (incoming?.action) {
            Intent.ACTION_SEND_MULTIPLE -> {
                page = AppPage.INTAKE
                statusMessage = R.string.intake_multiple_not_supported
            }
            Intent.ACTION_SEND -> when {
                incoming.type?.startsWith("image/") == true -> {
                    page = AppPage.INTAKE
                    incoming.streamUri()?.let(::importImage)
                        ?: run { statusMessage = R.string.intake_unreadable }
                }
                incoming.type == "text/plain" -> {
                    page = AppPage.INTAKE
                    importText(incoming.getStringExtra(Intent.EXTRA_TEXT).orEmpty())
                }
                else -> {
                    page = AppPage.INTAKE
                    statusMessage = R.string.intake_unsupported_mime
                }
            }
        }
    }

    private fun showResult(result: IntakeResult) {
        statusMessage = when (result) {
            is IntakeResult.Saved -> R.string.intake_saved
            is IntakeResult.Rejected -> when (result.error) {
                IntakeError.EMPTY_TEXT -> R.string.intake_empty_text
                IntakeError.UNSUPPORTED_MIME -> R.string.intake_unsupported_mime
                IntakeError.MULTIPLE_IMAGES_NOT_SUPPORTED -> R.string.intake_multiple_not_supported
                IntakeError.UNREADABLE_SOURCE,
                IntakeError.INVALID_IMAGE,
                IntakeError.STORAGE_FAILURE,
                -> R.string.intake_unreadable
            }
        }
        if (result is IntakeResult.Saved) {
            reloadSources()
            page = AppPage.COLLECTION
        }
    }

    @Suppress("DEPRECATION")
    private fun Intent.streamUri(): Uri? = if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        getParcelableExtra(Intent.EXTRA_STREAM)
    }
}

internal enum class AppPage { COLLECTION, INTAKE, DETAIL }

@Composable
internal fun CollectorApp(
    page: AppPage,
    sources: List<SourceRecordEntity>,
    selectedSource: SourceRecordEntity?,
    statusMessage: Int?,
    onShowCollection: () -> Unit,
    onShowIntake: () -> Unit,
    onOpenSource: (SourceRecordEntity) -> Unit,
    onPaste: (String) -> Unit,
    onImageSelected: (Uri) -> Unit,
) {
    BackHandler(enabled = page != AppPage.COLLECTION, onBack = onShowCollection)
    Surface(modifier = Modifier.fillMaxSize()) {
        when (page) {
            AppPage.COLLECTION -> CollectionScreen(sources, onShowIntake, onOpenSource)
            AppPage.INTAKE -> IntakeScreen(statusMessage, onShowCollection, onPaste, onImageSelected)
            AppPage.DETAIL -> selectedSource?.let { SourceDetailScreen(it, onShowCollection) }
                ?: CollectionScreen(sources, onShowIntake, onOpenSource)
        }
    }
}

@Composable
private fun CollectionScreen(
    sources: List<SourceRecordEntity>,
    onAdd: () -> Unit,
    onOpenSource: (SourceRecordEntity) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(stringResource(R.string.collection_title), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.collection_count, sources.size))
            }
            Button(onClick = onAdd) { Text(stringResource(R.string.collection_add)) }
        }
        if (sources.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.collection_empty), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.collection_empty_hint))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(sources, key = { it.id }) { source ->
                    SourceCard(source = source, onClick = { onOpenSource(source) })
                }
            }
        }
    }
}

@Composable
private fun SourceCard(source: SourceRecordEntity, onClick: () -> Unit) {
    val title = source.originalText?.lineSequence()?.firstOrNull()?.take(36)
        ?: stringResource(R.string.collection_image_item)
    val detail = if (source.kind == "IMAGE") {
        listOfNotNull(source.mimeType, source.width?.let { width -> "${width}×${source.height}" })
            .joinToString(" · ")
    } else {
        source.originalText.orEmpty()
    }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(source.createdAt)),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun SourceDetailScreen(source: SourceRecordEntity, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedButton(onClick = onBack) { Text(stringResource(R.string.common_back)) }
        Text(stringResource(R.string.detail_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            if (source.kind == "IMAGE") stringResource(R.string.detail_image_source)
            else stringResource(R.string.detail_text_source),
            style = MaterialTheme.typography.labelLarge,
        )
        source.imagePath?.let { path ->
            val bitmap = remember(path, source.decodeSampleSize) {
                BitmapFactory.decodeFile(
                    path,
                    BitmapFactory.Options().apply { inSampleSize = source.decodeSampleSize ?: 1 },
                )
            }
            bitmap?.let {
                Image(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                    bitmap = it.asImageBitmap(),
                    contentDescription = stringResource(R.string.detail_original_image),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        source.originalText?.let { text ->
            Text(stringResource(R.string.detail_original_text), style = MaterialTheme.typography.titleMedium)
            Text(text)
        }
        Text(stringResource(R.string.detail_status, source.status))
        source.duplicateOfSourceId?.let { Text(stringResource(R.string.detail_duplicate)) }
    }
}

@Composable
private fun IntakeScreen(
    statusMessage: Int?,
    onBack: () -> Unit,
    onPaste: (String) -> Unit,
    onImageSelected: (Uri) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri -> uri?.let(onImageSelected) }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedButton(onClick = onBack) { Text(stringResource(R.string.common_back)) }
        Text(stringResource(R.string.intake_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.intake_subtitle), style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = text,
            onValueChange = { text = it },
            minLines = 6,
            label = { Text(stringResource(R.string.intake_text_label)) },
        )
        Button(modifier = Modifier.fillMaxWidth(), onClick = { onPaste(text) }) {
            Text(stringResource(R.string.intake_save_text))
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
        ) {
            Text(stringResource(R.string.intake_pick_image))
        }
        statusMessage?.let { message ->
            Text(stringResource(message), color = MaterialTheme.colorScheme.primary)
        }
    }
}
