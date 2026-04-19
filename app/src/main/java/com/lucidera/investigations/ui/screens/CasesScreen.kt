package com.lucidera.investigations.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.lucidera.investigations.data.CaseDraft
import com.lucidera.investigations.data.CaseStatus
import com.lucidera.investigations.data.import.ObsidianCaseImporter
import com.lucidera.investigations.data.local.entity.InvestigationCaseEntity
import com.lucidera.investigations.ui.components.LucidEraBrandHeader
import com.lucidera.investigations.ui.components.DictationOutlinedTextField
import com.lucidera.investigations.ui.components.appendDictation
import com.lucidera.investigations.ui.components.createSpeechIntent
import com.lucidera.investigations.ui.components.rememberSpeechToTextLauncher
import com.lucidera.investigations.ui.viewmodel.CasesViewModel

@Composable
fun CasesScreen(
    viewModel: CasesViewModel,
    onCaseSelected: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        importCaseFromMarkdown(context, uri)?.let { draft ->
            viewModel.addCase(draft)
            Toast.makeText(context, "${draft.caseCode} imported.", Toast.LENGTH_SHORT).show()
        } ?: Toast.makeText(context, "Could not import that case note.", Toast.LENGTH_LONG).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LucidEraBrandHeader(
            title = "Investigation Cases",
            subtitle = "Each case should map cleanly to the vault: one folder, one master note, one clear question.",
            compact = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { showDialog = true }) {
                Text("Add Case")
            }
            Button(onClick = { importLauncher.launch(arrayOf("text/*", "application/octet-stream")) }) {
                Text("Import Note")
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.cases, key = { it.id }) { caseItem ->
                CaseCard(caseItem = caseItem, onCaseSelected = onCaseSelected)
            }
        }
    }

    if (showDialog) {
        AddCaseDialog(
            onDismiss = { showDialog = false },
            onSave = {
                viewModel.addCase(it)
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
            Text("Status: ${caseItem.status.name}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCaseDialog(
    onDismiss: () -> Unit,
    onSave: (CaseDraft) -> Unit
) {
    var caseCode by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var essentialQuestion by remember { mutableStateOf("") }
    var primarySubject by remember { mutableStateOf("") }
    var classification by remember { mutableStateOf("Internal working document") }
    var leadInvestigator by remember { mutableStateOf("Ethan Bradley") }
    var summary by remember { mutableStateOf("") }
    var caseFolderName by remember { mutableStateOf("") }
    var masterNoteName by remember { mutableStateOf("") }
    var savePath by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }
    var publicationThreshold by remember { mutableStateOf("Three independent, non-circular sources before treating a claim as established.") }
    val context = LocalContext.current
    var dictationTarget by remember { mutableStateOf(CaseDictationTarget.CASE_CODE) }
    val speechLauncher = rememberSpeechToTextLauncher(context) { result ->
        when (dictationTarget) {
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
        title = { Text("Add Case") },
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
                    label = "Case code",
                    singleLine = true,
                    onDictate = {
                        dictationTarget = CaseDictationTarget.CASE_CODE
                        speechLauncher.launch(createSpeechIntent())
                    }
                )
                DictationOutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Title",
                    onDictate = {
                        dictationTarget = CaseDictationTarget.TITLE
                        speechLauncher.launch(createSpeechIntent())
                    }
                )
                DictationOutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = "Working summary",
                    onDictate = {
                        dictationTarget = CaseDictationTarget.SUMMARY
                        speechLauncher.launch(createSpeechIntent())
                    }
                )
                DictationOutlinedTextField(
                    value = essentialQuestion,
                    onValueChange = { essentialQuestion = it },
                    label = "Essential question",
                    onDictate = {
                        dictationTarget = CaseDictationTarget.ESSENTIAL_QUESTION
                        speechLauncher.launch(createSpeechIntent())
                    }
                )
                DictationOutlinedTextField(
                    value = primarySubject,
                    onValueChange = { primarySubject = it },
                    label = "Primary subject",
                    onDictate = {
                        dictationTarget = CaseDictationTarget.PRIMARY_SUBJECT
                        speechLauncher.launch(createSpeechIntent())
                    }
                )
                DictationOutlinedTextField(
                    value = classification,
                    onValueChange = { classification = it },
                    label = "Classification",
                    onDictate = {
                        dictationTarget = CaseDictationTarget.CLASSIFICATION
                        speechLauncher.launch(createSpeechIntent())
                    }
                )
                DictationOutlinedTextField(
                    value = leadInvestigator,
                    onValueChange = { leadInvestigator = it },
                    label = "Lead",
                    onDictate = {
                        dictationTarget = CaseDictationTarget.LEAD
                        speechLauncher.launch(createSpeechIntent())
                    }
                )
                DictationOutlinedTextField(
                    value = publicationThreshold,
                    onValueChange = { publicationThreshold = it },
                    label = "Publication threshold",
                    onDictate = {
                        dictationTarget = CaseDictationTarget.PUBLICATION_THRESHOLD
                        speechLauncher.launch(createSpeechIntent())
                    }
                )
                TextButton(onClick = { showAdvanced = !showAdvanced }) {
                    Text(if (showAdvanced) "Hide advanced fields" else "Show advanced fields")
                }
                if (showAdvanced) {
                    DictationOutlinedTextField(
                        value = caseFolderName,
                        onValueChange = { caseFolderName = it },
                        label = "Case folder",
                        onDictate = {
                            dictationTarget = CaseDictationTarget.CASE_FOLDER
                            speechLauncher.launch(createSpeechIntent())
                        }
                    )
                    DictationOutlinedTextField(
                        value = masterNoteName,
                        onValueChange = { masterNoteName = it },
                        label = "Master note",
                        onDictate = {
                            dictationTarget = CaseDictationTarget.MASTER_NOTE
                            speechLauncher.launch(createSpeechIntent())
                        }
                    )
                    DictationOutlinedTextField(
                        value = savePath,
                        onValueChange = { savePath = it },
                        label = "Vault path",
                        onDictate = {
                            dictationTarget = CaseDictationTarget.SAVE_PATH
                            speechLauncher.launch(createSpeechIntent())
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
