package com.lucidera.investigations.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.lucidera.investigations.data.ConfidenceLevel
import com.lucidera.investigations.data.EntityDraft
import com.lucidera.investigations.data.EntityType
import com.lucidera.investigations.data.LeadDraft
import com.lucidera.investigations.data.LeadStatus
import com.lucidera.investigations.data.export.MarkdownShareHelper
import com.lucidera.investigations.data.export.ObsidianMarkdownExporter
import com.lucidera.investigations.data.local.entity.EntityProfileEntity
import com.lucidera.investigations.data.local.entity.LeadEntity
import com.lucidera.investigations.ui.components.LucidEraBrandHeader
import com.lucidera.investigations.ui.viewmodel.CaseDetailViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseDetailScreen(
    viewModel: CaseDetailViewModel,
    onBack: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenCases: () -> Unit,
    onDelete: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLeadDialog by remember { mutableStateOf(false) }
    var showEntityDialog by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<ExportDocument?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (state.case == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Case not found.")
        }
        return
    }

    val caseItem = state.case
        ?: return
    val markdown = remember(caseItem, state.leads, state.entities) {
        ObsidianMarkdownExporter.buildCaseMarkdown(caseItem, state.leads, state.entities)
    }
    val sessionLogMarkdown = remember(caseItem, state.leads, state.entities) {
        ObsidianMarkdownExporter.buildSessionLogMarkdown(caseItem, state.leads, state.entities)
    }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        val export = pendingExport
        pendingExport = null
        if (uri == null || export == null) {
            return@rememberLauncherForActivityResult
        }

        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(export.contents.toByteArray())
            } ?: error("Could not open export target.")
        }.onSuccess {
            Toast.makeText(context, "${export.label} exported.", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, it.message ?: "Export failed.", Toast.LENGTH_LONG).show()
        }
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(caseItem.caseCode) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onOpenCases) {
                        Text("Cases")
                    }
                    TextButton(onClick = onOpenHome) {
                        Text("Home")
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "More actions")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export Case Note") },
                                onClick = {
                                    pendingExport = ExportDocument(
                                        label = "Case note",
                                        fileName = caseItem.masterNoteName,
                                        contents = markdown
                                    )
                                    showOverflowMenu = false
                                    exportLauncher.launch(caseItem.masterNoteName)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Session Log") },
                                onClick = {
                                    val fileName = ObsidianMarkdownExporter.buildSessionLogFileName(caseItem)
                                    pendingExport = ExportDocument(
                                        label = "Session log",
                                        fileName = fileName,
                                        contents = sessionLogMarkdown
                                    )
                                    showOverflowMenu = false
                                    exportLauncher.launch(fileName)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Case Note") },
                                onClick = {
                                    showOverflowMenu = false
                                    MarkdownShareHelper.shareMarkdown(
                                        context = context,
                                        fileName = caseItem.masterNoteName,
                                        markdown = markdown,
                                        chooserTitle = "Share case note"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Session Log") },
                                onClick = {
                                    showOverflowMenu = false
                                    MarkdownShareHelper.shareMarkdown(
                                        context = context,
                                        fileName = ObsidianMarkdownExporter.buildSessionLogFileName(caseItem),
                                        markdown = sessionLogMarkdown,
                                        chooserTitle = "Share session log"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Case") },
                                onClick = {
                                    showOverflowMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LucidEraBrandHeader(
                    title = caseItem.title,
                    subtitle = "Keep the mobile record aligned with the master note, not separate from it.",
                    compact = true
                )
            }
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Case file", fontWeight = FontWeight.Bold)
                        Text("Lead: ${caseItem.leadInvestigator}")
                        Text("Classification: ${caseItem.classification}")
                        Text("Folder: ${caseItem.caseFolderName}")
                        Text("Master note: ${caseItem.masterNoteName}")
                        Text("Vault path: ${caseItem.savePath}", style = MaterialTheme.typography.bodySmall)
                        Text("Essential question", fontWeight = FontWeight.Bold)
                        Text(caseItem.essentialQuestion)
                        Text("Primary subject", fontWeight = FontWeight.Bold)
                        Text(caseItem.primarySubject)
                        Text("Working summary", fontWeight = FontWeight.Bold)
                        Text(caseItem.summary)
                        Text("Publication threshold", fontWeight = FontWeight.Bold)
                        Text(caseItem.publicationThreshold)
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { showLeadDialog = true }
                    ) {
                        Text("Add Lead")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { showEntityDialog = true }
                    ) {
                        Text("Add Entity")
                    }
                }
            }
            item {
                Text("Lead log", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            items(state.leads, key = { it.id }) { lead ->
                LeadCard(
                    lead = lead,
                    onVerify = { viewModel.updateLeadStatus(lead.id, LeadStatus.VERIFIED) },
                    onArchive = { viewModel.updateLeadStatus(lead.id, LeadStatus.ARCHIVED) }
                )
            }
            item {
                Text("Canonical entities", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            items(state.entities, key = { it.id }) { entity ->
                val entityMarkdown = remember(caseItem, entity) {
                    ObsidianMarkdownExporter.buildEntityMarkdown(entity, caseItem)
                }
                EntityCard(
                    entity = entity,
                    onExport = {
                        val fileName = "${ObsidianMarkdownExporter.buildEntityFileName(entity)}.md"
                        pendingExport = ExportDocument(
                            label = "Entity note",
                            fileName = fileName,
                            contents = entityMarkdown
                        )
                        exportLauncher.launch(fileName)
                    },
                    onShare = {
                        MarkdownShareHelper.shareMarkdown(
                            context = context,
                            fileName = "${ObsidianMarkdownExporter.buildEntityFileName(entity)}.md",
                            markdown = entityMarkdown,
                            chooserTitle = "Share entity note"
                        )
                    }
                )
            }
        }
    }

    if (showLeadDialog) {
        AddLeadDialog(
            onDismiss = { showLeadDialog = false },
            onSave = {
                viewModel.addLead(it)
                showLeadDialog = false
            }
        )
    }

    if (showEntityDialog) {
        AddEntityDialog(
            onDismiss = { showEntityDialog = false },
            onSave = {
                viewModel.addEntity(it)
                showEntityDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCase()
                        showDeleteDialog = false
                        Toast.makeText(context, "Case deleted.", Toast.LENGTH_SHORT).show()
                        onDelete()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Delete case?") },
            text = {
                Text("This removes the case and its local leads and entities from the app.")
            }
        )
    }
}

@Composable
private fun LeadCard(
    lead: LeadEntity,
    onVerify: () -> Unit,
    onArchive: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(lead.sourceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(lead.summary)
            Text("Source URL: ${lead.sourceUrl}", style = MaterialTheme.typography.bodySmall)
            if (lead.archiveUrl.isNotBlank()) {
                Text("Archive URL: ${lead.archiveUrl}", style = MaterialTheme.typography.bodySmall)
            }
            Text("Collected: ${formatDate(lead.collectedAt)}", style = MaterialTheme.typography.bodySmall)
            Text(
                "Status: ${lead.status.name}",
                style = MaterialTheme.typography.bodySmall,
                color = when (lead.status) {
                    LeadStatus.VERIFIED -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onVerify) {
                    Text("Mark Verified")
                }
                TextButton(onClick = onArchive) {
                    Text("Archive Lead")
                }
            }
        }
    }
}

@Composable
private fun EntityCard(
    entity: EntityProfileEntity,
    onExport: () -> Unit,
    onShare: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(entity.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "${entity.entityType.name} · ${entity.confidence.name}",
                color = if (entity.confidence == ConfidenceLevel.VERIFIED) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(entity.summary)
            Text("Identifier: ${entity.identifier}", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onExport) {
                    Text("Export Note")
                }
                TextButton(onClick = onShare) {
                    Text("Share Note")
                }
            }
        }
    }
}

@Composable
private fun AddLeadDialog(
    onDismiss: () -> Unit,
    onSave: (LeadDraft) -> Unit
) {
    var sourceName by remember { mutableStateOf("") }
    var sourceUrl by remember { mutableStateOf("") }
    var archiveUrl by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        LeadDraft(
                            sourceName = sourceName,
                            sourceUrl = sourceUrl,
                            archiveUrl = archiveUrl,
                            summary = summary,
                            status = LeadStatus.OPEN
                        )
                    )
                },
                enabled = sourceName.isNotBlank() && sourceUrl.isNotBlank() && summary.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Add Lead") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = sourceName, onValueChange = { sourceName = it }, label = { Text("Source or lead name") })
                OutlinedTextField(value = sourceUrl, onValueChange = { sourceUrl = it }, label = { Text("Live URL") })
                OutlinedTextField(value = archiveUrl, onValueChange = { archiveUrl = it }, label = { Text("Archive URL") })
                OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Why it matters") })
            }
        }
    )
}

@Composable
private fun AddEntityDialog(
    onDismiss: () -> Unit,
    onSave: (EntityDraft) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var identifier by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        EntityDraft(
                            name = name,
                            entityType = EntityType.ORGANIZATION,
                            confidence = ConfidenceLevel.PROBABLE,
                            summary = summary,
                            identifier = identifier
                        )
                    )
                },
                enabled = name.isNotBlank() && summary.isNotBlank() && identifier.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Add Entity") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Entity name") })
                OutlinedTextField(value = identifier, onValueChange = { identifier = it }, label = { Text("Identifier") })
                OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Why this entity matters") })
            }
        }
    )
}

private fun formatDate(timestamp: Long): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(timestamp))

private data class ExportDocument(
    val label: String,
    val fileName: String,
    val contents: String
)
