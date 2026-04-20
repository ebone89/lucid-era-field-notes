package com.lucidera.investigations.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucidera.investigations.data.local.entity.InvestigationCaseEntity
import com.lucidera.investigations.ui.components.LucidEraBrandHeader
import com.lucidera.investigations.ui.viewmodel.ArchiveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveLookupScreen(
    viewModel: ArchiveViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var url by remember { mutableStateOf("https://search.sunbiz.org/") }
    var caseSaveMode by remember { mutableStateOf<CaseSaveMode?>(null) }
    val context = LocalContext.current

    LaunchedEffect(state.savedMessage) {
        state.savedMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearSavedMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Archive") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LucidEraBrandHeader(
                title = "Archive Lookup",
                subtitle = "Find a saved copy of the page, then send it straight into a case.",
                compact = true
            )
            Text(
                "Paste the live URL here. If a snapshot exists, you can save it into a case as a source.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = url,
                onValueChange = { url = it },
                label = { Text("Public URL") },
                singleLine = true
            )
            Button(
                onClick = { viewModel.lookupArchive(url) },
                enabled = !state.isLoading && url.isNotBlank()
            ) {
                Text("Find snapshot")
            }
            if (url.isNotBlank()) {
                TextButton(onClick = { caseSaveMode = CaseSaveMode.LiveUrl }) {
                    Text("Save live URL to case")
                }
            }

            when {
                state.isLoading -> Box(modifier = Modifier.fillMaxWidth()) { CircularProgressIndicator() }
                state.errorMessage != null -> {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Wayback did not return a snapshot for this page right now.",
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "You can still save the live URL to a case, or paste an archive link manually if you found one elsewhere.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(
                                onClick = { caseSaveMode = CaseSaveMode.LiveUrl },
                                enabled = state.cases.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (state.cases.isEmpty()) "No cases available" else "Save live URL to case")
                            }
                            TextButton(
                                onClick = { caseSaveMode = CaseSaveMode.ManualArchive },
                                enabled = state.cases.isNotEmpty()
                            ) {
                                Text("Paste archive URL manually")
                            }
                        }
                    }
                }
                state.result != null -> {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Snapshot found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            ArchiveResultRow(label = "Original URL", value = state.result?.originalUrl.orEmpty())
                            ArchiveResultRow(label = "Archive URL", value = state.result?.archiveUrl.orEmpty())
                            ArchiveResultRow(label = "Timestamp", value = state.result?.timestamp.orEmpty())
                            ArchiveResultRow(label = "Status", value = state.result?.status.orEmpty())
                            Button(
                                onClick = { caseSaveMode = CaseSaveMode.Snapshot },
                                enabled = state.cases.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (state.cases.isEmpty()) "No cases available" else "Add to case")
                            }
                        }
                    }
                }
            }
        }
    }

    if (caseSaveMode != null) {
        AddSourceToCaseDialog(
            cases = state.cases,
            liveUrl = state.result?.originalUrl ?: url,
            initialArchiveUrl = if (caseSaveMode == CaseSaveMode.ManualArchive) "" else state.result?.archiveUrl.orEmpty(),
            allowArchiveEdit = caseSaveMode != CaseSaveMode.LiveUrl,
            title = when (caseSaveMode) {
                CaseSaveMode.Snapshot -> "Add snapshot to case"
                CaseSaveMode.ManualArchive -> "Add source with archive link"
                CaseSaveMode.LiveUrl -> "Save live URL to case"
                null -> ""
            },
            onDismiss = { caseSaveMode = null },
            onSave = { caseId, liveUrl, archiveUrl, note ->
                viewModel.addSourceToCase(
                    caseId = caseId,
                    sourceUrl = liveUrl,
                    archiveUrl = archiveUrl,
                    note = note
                )
                caseSaveMode = null
            }
        )
    }
}

@Composable
private fun ArchiveResultRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun AddSourceToCaseDialog(
    cases: List<InvestigationCaseEntity>,
    liveUrl: String,
    initialArchiveUrl: String,
    allowArchiveEdit: Boolean,
    title: String,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, String) -> Unit
) {
    var selectedCaseId by remember(cases) { mutableStateOf(cases.firstOrNull()?.id) }
    var editableLiveUrl by remember(liveUrl) { mutableStateOf(liveUrl) }
    var archiveUrl by remember(initialArchiveUrl) { mutableStateOf(initialArchiveUrl) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val caseId = selectedCaseId ?: return@TextButton
                    onSave(caseId, editableLiveUrl, archiveUrl, note)
                },
                enabled = selectedCaseId != null && editableLiveUrl.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text("Choose the case you want to add this source to.")
                OutlinedTextField(
                    value = editableLiveUrl,
                    onValueChange = { editableLiveUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Live URL") }
                )
                if (allowArchiveEdit) {
                    OutlinedTextField(
                        value = archiveUrl,
                        onValueChange = { archiveUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Archive URL") }
                    )
                }
                cases.forEach { caseItem ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCaseId = caseItem.id }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "${caseItem.caseCode} ${if (selectedCaseId == caseItem.id) "• selected" else ""}".trim(),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selectedCaseId == caseItem.id) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                caseItem.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note for this source") }
                )
            }
        }
    )
}

private enum class CaseSaveMode {
    Snapshot,
    LiveUrl,
    ManualArchive
}
