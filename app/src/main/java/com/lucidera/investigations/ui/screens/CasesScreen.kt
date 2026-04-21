package com.lucidera.investigations.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucidera.investigations.data.CaseDraft
import com.lucidera.investigations.data.CaseStatus
import com.lucidera.investigations.data.import.ObsidianCaseImporter
import com.lucidera.investigations.data.local.entity.InvestigationCaseEntity
import com.lucidera.investigations.ui.components.DictationOutlinedTextField
import com.lucidera.investigations.ui.components.LucidEraBrandHeader
import com.lucidera.investigations.ui.components.appendDictation
import com.lucidera.investigations.ui.components.createSpeechIntent
import com.lucidera.investigations.ui.components.rememberSpeechToTextLauncher
import com.lucidera.investigations.ui.viewmodel.CasesViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class CaseSortOrder { DATE_DESC, DATE_ASC, CODE }

private fun nextCaseCode(cases: List<InvestigationCaseEntity>): String {
    val max = cases.mapNotNull { c ->
        Regex("^CASE-(\\d+)$").matchEntire(c.caseCode)?.groupValues?.get(1)?.toIntOrNull()
    }.maxOrNull() ?: 0
    return "CASE-%03d".format(max + 1)
}

@Composable
fun CasesScreen(
    viewModel: CasesViewModel,
    onCaseSelected: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(CaseSortOrder.DATE_DESC) }
    val sortedCases = remember(state.cases, sortOrder) {
        when (sortOrder) {
            CaseSortOrder.DATE_DESC -> state.cases.sortedByDescending { it.createdAt }
            CaseSortOrder.DATE_ASC -> state.cases.sortedBy { it.createdAt }
            CaseSortOrder.CODE -> state.cases.sortedBy { it.caseCode }
        }
    }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        importCaseFromMarkdown(context, uri)?.let { draft ->
            viewModel.addCase(draft)
            Toast.makeText(context, "${draft.caseCode} imported.", Toast.LENGTH_SHORT).show()
        } ?: Toast.makeText(context, "Could not import that case note.", Toast.LENGTH_LONG).show()
    }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearUserMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LucidEraBrandHeader(
                    title = "Investigation Cases",
                    subtitle = "All open investigations. Tap a case to add sources, entities, or field photos.",
                    compact = true
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add Case")
                    }
                    Button(
                        onClick = { importLauncher.launch(arrayOf("text/*", "application/octet-stream")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Import Note")
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.setSearchQuery(it)
                    },
                    placeholder = { Text("Search cases…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                viewModel.setSearchQuery("")
                            }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        CaseSortOrder.DATE_DESC to "Newest",
                        CaseSortOrder.DATE_ASC to "Oldest",
                        CaseSortOrder.CODE to "Code"
                    ).forEach { (order, label) ->
                        TextButton(onClick = { sortOrder = order }) {
                            Text(
                                label,
                                color = if (sortOrder == order) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (sortedCases.isEmpty()) {
                item {
                    Text(
                        if (searchQuery.isEmpty()) "No cases yet. Tap Add Case to start one."
                        else "No cases match your search.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            items(sortedCases, key = { it.id }) { caseItem ->
                CaseCard(caseItem = caseItem, onCaseSelected = onCaseSelected)
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showDialog) {
        AddCaseDialog(
            nextCaseCode = nextCaseCode(state.cases),
            onDismiss = { showDialog = false },
            onSave = { draft ->
                viewModel.addCase(draft)
                showDialog = false
            }
        )
    }
}

@Composable
private fun CaseCard(
    caseItem: InvestigationCaseEntity,
    onCaseSelected: (Long) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCaseSelected(caseItem.id) }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                caseItem.caseCode,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(caseItem.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(caseItem.summary, style = MaterialTheme.typography.bodyMedium)
            Text("Primary subject: ${caseItem.primarySubject}", style = MaterialTheme.typography.bodySmall)
            Text("Folder: ${caseItem.caseFolderName}", style = MaterialTheme.typography.bodySmall)
            Text("Status: ${caseItem.status.name.lowercase().replaceFirstChar(Char::uppercase)}", style = MaterialTheme.typography.bodySmall)
            Text("Logged: ${formatDate(caseItem.createdAt)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatDate(millis: Long): String {
    val instant = Instant.ofEpochMilli(millis)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCaseDialog(
    nextCaseCode: String,
    onDismiss: () -> Unit,
    onSave: (CaseDraft) -> Unit
) {
    var caseCode by remember { mutableStateOf(nextCaseCode) }
    var title by remember { mutableStateOf("") }
    var essentialQuestion by remember { mutableStateOf("") }
    var primarySubject by remember { mutableStateOf("") }
    var classification by remember { mutableStateOf("Internal working document") }
    var leadInvestigator by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var caseFolderName by remember { mutableStateOf("") }
    var masterNoteName by remember { mutableStateOf("") }
    var savePath by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }
    var publicationThreshold by remember { mutableStateOf("Wait until the key claim holds up across at least three solid, independent sources.") }
    val speechLauncher = rememberSpeechToTextLauncher { result, target ->
        when (target as? CaseDictationTarget) {
            CaseDictationTarget.CASE_CODE -> caseCode = appendDictation(caseCode, result)
            CaseDictationTarget.TITLE -> title = appendDictation(title, result)
            CaseDictationTarget.SUMMARY -> summary = appendDictation(summary, result)
            CaseDictationTarget.ESSENTIAL_QUESTION -> essentialQuestion = appendDictation(essentialQuestion, result)
            CaseDictationTarget.PRIMARY_SUBJECT -> primarySubject = appendDictation(primarySubject, result)
            CaseDictationTarget.CLASSIFICATION -> classification = appendDictation(classification, result)
            CaseDictationTarget.LEAD -> leadInvestigator = appendDictation(leadInvestigator, result)
            CaseDictationTarget.PUBLICATION_THRESHOLD -> publicationThreshold = appendDictation(publicationThreshold, result)
            CaseDictationTarget.CASE_FOLDER -> caseFolderName = appendDictation(caseFolderName, result)
            CaseDictationTarget.MASTER_NOTE -> masterNoteName = appendDictation(masterNoteName, result)
            CaseDictationTarget.SAVE_PATH -> savePath = appendDictation(savePath, result)
            else -> {}
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        CaseDraft(
                            caseCode = caseCode,
                            title = title,
                            essentialQuestion = essentialQuestion,
                            primarySubject = primarySubject,
                            classification = classification,
                            leadInvestigator = leadInvestigator,
                            summary = summary,
                            caseFolderName = caseFolderName.ifBlank { "${caseCode}_${title.replace(" ", "_")}" },
                            masterNoteName = masterNoteName.ifBlank { "${caseCode}_${title.replace(" ", "_")}.md" },
                            savePath = savePath,
                            publicationThreshold = publicationThreshold,
                            status = CaseStatus.ACTIVE
                        )
                    )
                },
                enabled = caseCode.isNotBlank() && title.isNotBlank() && essentialQuestion.isNotBlank() && summary.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("New Case") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DictationOutlinedTextField(
                    value = caseCode,
                    onValueChange = { caseCode = it },
                    label = { Text("Case code") },
                    singleLine = true,
                    onDictate = {
                        speechLauncher.launch(createSpeechIntent(), CaseDictationTarget.CASE_CODE)
                    }
                )
                DictationOutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    onDictate = {
                        speechLauncher.launch(createSpeechIntent(), CaseDictationTarget.TITLE)
                    }
                )
                DictationOutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Summary so far") },
                    onDictate = {
                        speechLauncher.launch(createSpeechIntent(), CaseDictationTarget.SUMMARY)
                    }
                )
                DictationOutlinedTextField(
                    value = essentialQuestion,
                    onValueChange = { essentialQuestion = it },
                    label = { Text("Essential question") },
                    onDictate = {
                        speechLauncher.launch(createSpeechIntent(), CaseDictationTarget.ESSENTIAL_QUESTION)
                    }
                )
                DictationOutlinedTextField(
                    value = primarySubject,
                    onValueChange = { primarySubject = it },
                    label = { Text("Primary subject") },
                    onDictate = {
                        speechLauncher.launch(createSpeechIntent(), CaseDictationTarget.PRIMARY_SUBJECT)
                    }
                )
                DictationOutlinedTextField(
                    value = classification,
                    onValueChange = { classification = it },
                    label = { Text("Classification") },
                    onDictate = {
                        speechLauncher.launch(createSpeechIntent(), CaseDictationTarget.CLASSIFICATION)
                    }
                )
                DictationOutlinedTextField(
                    value = leadInvestigator,
                    onValueChange = { leadInvestigator = it },
                    label = { Text("Lead") },
                    onDictate = {
                        speechLauncher.launch(createSpeechIntent(), CaseDictationTarget.LEAD)
                    }
                )
                DictationOutlinedTextField(
                    value = publicationThreshold,
                    onValueChange = { publicationThreshold = it },
                    label = { Text("Ready to publish when") },
                    onDictate = {
                        speechLauncher.launch(createSpeechIntent(), CaseDictationTarget.PUBLICATION_THRESHOLD)
                    }
                )
                TextButton(onClick = { showAdvanced = !showAdvanced }) {
                    Text(if (showAdvanced) "Hide extra fields" else "Show extra fields")
                }
                if (showAdvanced) {
                    DictationOutlinedTextField(
                        value = caseFolderName,
                        onValueChange = { caseFolderName = it },
                        label = { Text("Case folder") },
                        onDictate = {
                            speechLauncher.launch(createSpeechIntent(), CaseDictationTarget.CASE_FOLDER)
                        }
                    )
                    DictationOutlinedTextField(
                        value = masterNoteName,
                        onValueChange = { masterNoteName = it },
                        label = { Text("Master note") },
                        onDictate = {
                            speechLauncher.launch(createSpeechIntent(), CaseDictationTarget.MASTER_NOTE)
                        }
                    )
                    DictationOutlinedTextField(
                        value = savePath,
                        onValueChange = { savePath = it },
                        label = { Text("Folder path") },
                        onDictate = {
                            speechLauncher.launch(createSpeechIntent(), CaseDictationTarget.SAVE_PATH)
                        }
                    )
                }
            }
        }
    )
}

private fun importCaseFromMarkdown(
    context: android.content.Context,
    uri: Uri
): CaseDraft? = runCatching {
    val fileName = context.contentResolver.query(uri, null, null, null, null)
        ?.use { cursor ->
            val index = cursor.getColumnIndex("_display_name")
            if (cursor.moveToFirst() && index != -1) cursor.getString(index) else null
        }
        ?: "ImportedCase.md"
    val markdown = context.contentResolver.openInputStream(uri)
        ?.bufferedReader()
        ?.use { it.readText() }
        ?: return null
    ObsidianCaseImporter.importCase(fileName, markdown)
}.getOrNull()

private enum class CaseDictationTarget {
    CASE_CODE,
    TITLE,
    SUMMARY,
    ESSENTIAL_QUESTION,
    PRIMARY_SUBJECT,
    CLASSIFICATION,
    LEAD,
    PUBLICATION_THRESHOLD,
    CASE_FOLDER,
    MASTER_NOTE,
    SAVE_PATH
}
