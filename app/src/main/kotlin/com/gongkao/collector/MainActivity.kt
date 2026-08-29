package com.gongkao.collector

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.gongkao.collector.data.intake.IntakeError
import com.gongkao.collector.data.intake.IntakeResult
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val statusMessage = mutableStateOf<Int?>(null)

    private val intakeRepository
        get() = (application as GongkaoApplication).container.sourceIntakeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                IntakeScreen(
                    statusMessage = statusMessage.value,
                    onPaste = ::importText,
                    onImageSelected = ::importImage,
                )
            }
        }
        if (savedInstanceState == null) handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun importText(text: String) {
        lifecycleScope.launch {
            showResult(intakeRepository.importText(text))
        }
    }

    private fun importImage(uri: Uri) {
        lifecycleScope.launch {
            val result = intakeRepository.importImage(contentResolver.getType(uri)) {
                contentResolver.openInputStream(uri)
            }
            showResult(result)
        }
    }

    internal fun handleIncomingIntent(incoming: Intent?) {
        when (incoming?.action) {
            Intent.ACTION_SEND_MULTIPLE -> statusMessage.value = R.string.intake_multiple_not_supported
            Intent.ACTION_SEND -> when {
                incoming.type?.startsWith("image/") == true -> {
                    incoming.streamUri()?.let(::importImage)
                        ?: run { statusMessage.value = R.string.intake_unreadable }
                }
                incoming.type == "text/plain" -> importText(incoming.getStringExtra(Intent.EXTRA_TEXT).orEmpty())
                else -> statusMessage.value = R.string.intake_unsupported_mime
            }
        }
    }

    private fun showResult(result: IntakeResult) {
        statusMessage.value = when (result) {
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
    }

    @Suppress("DEPRECATION")
    private fun Intent.streamUri(): Uri? = if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        getParcelableExtra(Intent.EXTRA_STREAM)
    }
}

@Composable
private fun IntakeScreen(
    statusMessage: Int?,
    onPaste: (String) -> Unit,
    onImageSelected: (Uri) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri?.let(onImageSelected)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.intake_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.intake_subtitle),
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = text,
                onValueChange = { text = it },
                minLines = 6,
                label = { Text(stringResource(R.string.intake_text_label)) },
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { scope.launch { onPaste(text) } },
            ) {
                Text(stringResource(R.string.intake_save_text))
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
            ) {
                Text(stringResource(R.string.intake_pick_image))
            }
            statusMessage?.let { message ->
                Text(
                    text = stringResource(message),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
