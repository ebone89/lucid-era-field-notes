package com.lucidera.investigations.ui.components

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.io.Serializable
import java.util.Locale

class SpeechToTextLauncher(
    private val launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    private val onTagSet: (Any?) -> Unit
) {
    fun launch(intent: Intent, tag: Any? = null) {
        onTagSet(tag)
        launcher.launch(intent)
    }
}

@Composable
fun rememberSpeechToTextLauncher(
    onResult: (String, Any?) -> Unit
): SpeechToTextLauncher {
    val context = LocalContext.current
    var pendingTag by remember { mutableStateOf<Any?>(null) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                onResult(spoken, pendingTag)
            } else {
                Toast.makeText(context, "No speech captured.", Toast.LENGTH_SHORT).show()
            }
        }
        pendingTag = null
    }

    return remember(launcher) {
        SpeechToTextLauncher(launcher) { pendingTag = it }
    }
}

fun createSpeechIntent(): Intent =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
    }

fun appendDictation(existing: String, incoming: String): String =
    if (existing.isBlank()) incoming else "$existing $incoming"

@Composable
fun DictationOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onDictate: () -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false
) {
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        label = label,
        singleLine = singleLine,
        trailingIcon = {
            IconButton(onClick = onDictate) {
                Icon(Icons.Outlined.Mic, contentDescription = "Dictate")
            }
        }
    )
}
