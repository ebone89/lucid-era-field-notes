package com.lucidera.investigations.ui.screens

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucidera.investigations.data.CaseDraft
import com.lucidera.investigations.data.CaseStatus
import com.lucidera.investigations.data.local.entity.InvestigationCaseEntity
import com.lucidera.investigations.ui.components.LucidEraBrandHeader
import com.lucidera.investigations.ui.viewmodel.CasesViewModel

@Composable
fun CasesScreen(
    viewModel: CasesViewModel,
    onCaseSelected: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

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
        Button(onClick = { showDialog = true }) {
            Text("Add Case")
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
                OutlinedTextField(value = caseCode, onValueChange = { caseCode = it }, label = { Text("Case code") })
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Working summary") })
                OutlinedTextField(
                    value = essentialQuestion,
                    onValueChange = { essentialQuestion = it },
                    label = { Text("Essential question") }
                )
                OutlinedTextField(
                    value = primarySubject,
                    onValueChange = { primarySubject = it },
                    label = { Text("Primary subject") }
                )
                OutlinedTextField(
                    value = classification,
                    onValueChange = { classification = it },
                    label = { Text("Classification") }
                )
                OutlinedTextField(
                    value = leadInvestigator,
                    onValueChange = { leadInvestigator = it },
                    label = { Text("Lead") }
                )
                OutlinedTextField(
                    value = publicationThreshold,
                    onValueChange = { publicationThreshold = it },
                    label = { Text("Publication threshold") }
                )
                TextButton(onClick = { showAdvanced = !showAdvanced }) {
                    Text(if (showAdvanced) "Hide advanced fields" else "Show advanced fields")
                }
                if (showAdvanced) {
                    OutlinedTextField(
                        value = caseFolderName,
                        onValueChange = { caseFolderName = it },
                        label = { Text("Case folder") }
                    )
                    OutlinedTextField(
                        value = masterNoteName,
                        onValueChange = { masterNoteName = it },
                        label = { Text("Master note") }
                    )
                    OutlinedTextField(
                        value = savePath,
                        onValueChange = { savePath = it },
                        label = { Text("Vault path") }
                    )
                }
            }
        }
    )
}
