package com.lucidera.investigations.ui.components

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.util.Locale

@Composable
fun rememberSpeechToTextLauncher(
    context: Context,
    onResult: (String) -> Unit
) = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    if (result.resultCode == android.app.Activity.RESULT_OK) {
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            onResult(spoken)
        } else {
            Toast.makeText(context, "No speech captured.", Toast.LENGTH_SHORT).show()
        }
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
    label: String,
    onDictate: () -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false
) {
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        trailingIcon = {
            IconButton(onClick = onDictate) {
                Icon(Icons.Outlined.Mic, contentDescription = "Dictate $label")
            }
        }
    )
}
