package com.lucidera.investigations.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lucidera.investigations.R
import com.lucidera.investigations.data.AttachmentDraft
import com.lucidera.investigations.data.AttachmentType
import com.lucidera.investigations.data.ConfidenceLevel
import com.lucidera.investigations.data.EntityDraft
import com.lucidera.investigations.data.EntityType
import com.lucidera.investigations.data.LeadDraft
import com.lucidera.investigations.data.LeadStatus
import com.lucidera.investigations.data.export.CasePackageExporter
import com.lucidera.investigations.data.export.MarkdownShareHelper
import com.lucidera.investigations.data.export.ObsidianMarkdownExporter
import com.lucidera.investigations.data.local.entity.CaseAttachmentEntity
import com.lucidera.investigations.data.local.entity.EntityProfileEntity
import com.lucidera.investigations.data.local.entity.LeadEntity
import com.lucidera.investigations.ui.components.DictationOutlinedTextField
import com.lucidera.investigations.ui.components.LucidEraBrandHeader
import com.lucidera.investigations.ui.components.appendDictation
import com.lucidera.investigations.ui.components.createSpeechIntent
import com.lucidera.investigations.ui.components.rememberSpeechToTextLauncher
import com.lucidera.investigations.ui.viewmodel.CaseDetailViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
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
    var pendingPhotoDraft by remember { mutableStateOf<PendingPhotoDraft?>(null) }
    var pendingPackageExport by remember { mutableStateOf<ExportPackage?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    if (state.case == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("This case is no longer available.")
        }
        return
    }

    val caseItem = state.case
        ?: return
    val markdown = remember(caseItem, state.leads, state.entities, state.attachments) {
        ObsidianMarkdownExporter.buildCaseMarkdown(caseItem, state.leads, state.entities, state.attachments)
    }
    val sessionLogMarkdown = remember(caseItem, state.leads, state.entities, state.attachments) {
        ObsidianMarkdownExporter.buildSessionLogMarkdown(caseItem, state.leads, state.entities, state.attachments)
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
    val packageExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val export = pendingPackageExport
        pendingPackageExport = null
        if (uri == null || export == null) {
            return@rememberLauncherForActivityResult
        }
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                CasePackageExporter.writeCasePackage(
                    context = context,
                    outputStream = output,
                    markdownFileName = export.markdownFileName,
                    markdown = export.markdown,
                    attachments = export.attachments
                )
            } ?: error("Could not open export target.")
        }.onSuccess {
            Toast.makeText(context, "${export.label} exported.", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, it.message ?: "Export failed.", Toast.LENGTH_LONG).show()
        }
    }
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            persistGalleryImage(context, uri)?.let { storedUri ->
                pendingPhotoDraft = PendingPhotoDraft(uri = storedUri, attachmentType = AttachmentType.GALLERY)
            } ?: Toast.makeText(context, "Could not store selected image.", Toast.LENGTH_LONG).show()
        }
    }
    val cameraCapture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let { uri ->
                pendingPhotoDraft = PendingPhotoDraft(uri = uri, attachmentType = AttachmentType.CAMERA)
            }
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
                                text = { Text("Export Case Package") },
                                onClick = {
                                    val fileName = CasePackageExporter.buildPackageFileName(caseItem)
                                    pendingPackageExport = ExportPackage(
                                        label = "Case package",
                                        fileName = fileName,
                                        markdownFileName = caseItem.masterNoteName,
                                        markdown = markdown,
                                        attachments = state.attachments
                                    )
                                    showOverflowMenu = false
                                    packageExportLauncher.launch(fileName)
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
                                text = { Text("Share Case Package") },
                                onClick = {
                                    showOverflowMenu = false
                                    MarkdownShareHelper.shareCasePackage(
                                        context = context,
                                        fileName = caseItem.masterNoteName,
                                        markdown = markdown,
                                        attachments = state.attachments,
                                        chooserTitle = "Share case package"
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
                    subtitle = "Use the phone for field capture, then fold the verified material back into the case file.",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val file = createCaseImageFile(context)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            cameraUri = uri
                            cameraCapture.launch(uri)
                        }
                    ) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                        Text(" Camera")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            galleryPicker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                        Text(" Gallery")
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
            item {
                Text("Attachments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            if (state.attachments.isEmpty()) {
                item {
                    Text(
                        "No photos or attachments logged for this case yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(state.attachments, key = { it.id }) { attachment ->
                AttachmentCard(attachment)
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
                Text("This removes the case, its leads, its entities, and its saved attachment records from the app.")
            }
        )
    }

    if (pendingPhotoDraft != null) {
        AddPhotoCaptionDialog(
            onDismiss = { pendingPhotoDraft = null },
            onSave = { caption ->
                val pending = pendingPhotoDraft ?: return@AddPhotoCaptionDialog
                viewModel.addAttachment(
                    AttachmentDraft(
                        uri = pending.uri.toString(),
                        fileName = pending.fileName,
                        caption = caption,
                        attachmentType = pending.attachmentType
                    )
                )
                pendingPhotoDraft = null
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
private fun AttachmentCard(attachment: CaseAttachmentEntity) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AsyncImage(
                model = attachment.uri,
                contentDescription = attachment.caption.ifBlank { attachment.fileName },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.lucid_era_logo),
                placeholder = painterResource(id = R.drawable.lucid_era_logo)
            )
            Text(attachment.fileName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (attachment.caption.isNotBlank()) {
                Text(attachment.caption)
            }
            Text(
                "Source: ${attachment.attachmentType.name.lowercase().replaceFirstChar(Char::uppercase)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    val context = LocalContext.current
    var dictationTarget by remember { mutableStateOf(DictationTarget.LEAD_SUMMARY) }
    val speechLauncher = rememberSpeechToTextLauncher(context) { result ->
        when (dictationTarget) {
            DictationTarget.LEAD_URL -> sourceUrl = appendDictation(sourceUrl, result)
            DictationTarget.LEAD_ARCHIVE_URL -> archiveUrl = appendDictation(archiveUrl, result)
            DictationTarget.LEAD_SUMMARY -> summary = appendDictation(summary, result)
            DictationTarget.LEAD_NAME -> sourceName = appendDictation(sourceName, result)
            else -> Unit
        }
    }

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
                DictationOutlinedTextField(
                    value = sourceName,
                    onValueChange = { sourceName = it },
                    label = "Source or lead name",
                    onDictate = {
                        dictationTarget = DictationTarget.LEAD_NAME
                        speechLauncher.launch(createSpeechIntent())
                    }
                )
                DictationOutlinedTextField(
                    value = sourceUrl,
                    onValueChange = { sourceUrl = it },
                    label = "Live URL",
                    onDictate = {
                        dictationTarget = DictationTarget.LEAD_URL
                        speechLauncher.launch(createSpeechIntent())
                    }
                )
                DictationOutlinedTextField(
                    value = archiveUrl,
                    onValueChange = { archiveUrl = it },
                    label = "Archive URL",
                    onDictate = {
                        dictationTarget = DictationTarget.LEAD_ARCHIVE_URL
                        speechLauncher.launch(createSpeechIntent())
                    }
                )
                DictationOutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = "Why it matters",
                    onDictate = {
                        dictationTarget = DictationTarget.LEAD_SUMMARY
                        speechLauncher.launch(createSpeechIntent())
                    }
                )
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
    val context = LocalContext.current
    var dictationTarget by remember { mutableStateOf(DictationTarget.ENTITY_SUMMARY) }
    val speechLauncher = rememberSpeechToTextLauncher(context) { result ->
        when (dictationTarget) {
            DictationTarget.ENTITY_NAME -> name = appendDictation(name, result)
            DictationTarget.ENTITY_SUMMARY -> summary = appendDictation(summary, result)
            DictationTarget.ENTITY_IDENTIFIER -> identifier = appendDictation(identifier, result)
            else -> Unit
        }
    }

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
                DictationOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Entity name",
                    onDictate = {
                        dictationTarget = DictationTarget.ENTITY_NAME
                        speechLauncher.launch(createSpeechIntent())
                    }
                )
                DictationOutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = "Identifier",
                    onDictate = {
                        dictationTarget = DictationTarget.ENTITY_IDENTIFIER
                        speechLauncher.launch(createSpeechIntent())
                    }
                )
                DictationOutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = "Why this entity matters",
                    onDictate = {
                        dictationTarget = DictationTarget.ENTITY_SUMMARY
                        speechLauncher.launch(createSpeechIntent())
                    }
                )
            }
        }
    )
}

@Composable
private fun AddPhotoCaptionDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    val context = LocalContext.current
    val speechLauncher = rememberSpeechToTextLauncher(context) { result ->
        caption = appendDictation(caption, result)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(caption) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Add photo note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DictationOutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = "Caption or note",
                    onDictate = { speechLauncher.launch(createSpeechIntent()) }
                )
            }
        }
    )
}

private fun formatDate(timestamp: Long): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(timestamp))

private fun createCaseImageFile(context: android.content.Context): File {
    val dir = File(context.filesDir, "case_images").apply { mkdirs() }
    return File(dir, "case_${System.currentTimeMillis()}.jpg")
}

private fun persistGalleryImage(
    context: android.content.Context,
    sourceUri: Uri
): Uri? {
    val extension = sourceUri
        .lastPathSegment
        ?.substringAfterLast('.', "")
        ?.takeIf { it.isNotBlank() }
        ?: "jpg"
    val targetFile = File(
        File(context.filesDir, "case_images").apply { mkdirs() },
        "gallery_${System.currentTimeMillis()}.$extension"
    )

    return runCatching {
        context.contentResolver.openInputStream(sourceUri).useToCopy(targetFile)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", targetFile)
    }.getOrNull()
}

private fun InputStream?.useToCopy(targetFile: File) {
    val input = this ?: error("Could not open image stream.")
    input.use { source ->
        FileOutputStream(targetFile).use { output ->
            source.copyTo(output)
        }
    }
}

private data class PendingPhotoDraft(
    val uri: Uri,
    val attachmentType: AttachmentType
) {
    val fileName: String = uri.lastPathSegment?.substringAfterLast('/') ?: "case_attachment.jpg"
}

private enum class DictationTarget {
    LEAD_NAME,
    LEAD_URL,
    LEAD_ARCHIVE_URL,
    LEAD_SUMMARY,
    ENTITY_NAME,
    ENTITY_IDENTIFIER,
    ENTITY_SUMMARY
}

private data class ExportDocument(
    val label: String,
    val fileName: String,
    val contents: String
)

private data class ExportPackage(
    val label: String,
    val fileName: String,
    val markdownFileName: String,
    val markdown: String,
    val attachments: List<CaseAttachmentEntity>
)
