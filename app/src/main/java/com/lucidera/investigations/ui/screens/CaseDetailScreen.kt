package com.lucidera.investigations.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
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
import kotlinx.coroutines.launch
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
    var leadStatusFilter by remember { mutableStateOf<LeadStatus?>(null) }
    var pendingExport by remember { mutableStateOf<ExportDocument?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingPhotoDraft by remember { mutableStateOf<PendingPhotoDraft?>(null) }
    var pendingPackageExport by remember { mutableStateOf<ExportPackage?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var leadToEdit by remember { mutableStateOf<LeadEntity?>(null) }
    var leadToDelete by remember { mutableStateOf<LeadEntity?>(null) }
    var entityToEdit by remember { mutableStateOf<EntityProfileEntity?>(null) }
    var entityToDelete by remember { mutableStateOf<EntityProfileEntity?>(null) }
    var attachmentToEdit by remember { mutableStateOf<CaseAttachmentEntity?>(null) }
    var attachmentToDelete by remember { mutableStateOf<CaseAttachmentEntity?>(null) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearUserMessage()
        }
    }

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
            val exif = extractExifData(context, uri)
            persistGalleryImage(context, uri)?.let { storedUri ->
                pendingPhotoDraft = PendingPhotoDraft(
                    uri = storedUri,
                    attachmentType = AttachmentType.GALLERY,
                    mimeType = resolveMimeType(context, uri) ?: "image/*",
                    gpsLat = exif.gpsLat,
                    gpsLon = exif.gpsLon,
                    capturedAt = exif.capturedAt,
                    deviceModel = exif.deviceModel
                )
            } ?: Toast.makeText(context, "Could not store selected image.", Toast.LENGTH_LONG).show()
        }
    }
    val cameraCapture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let { uri ->
                val exif = extractExifData(context, uri)
                pendingPhotoDraft = PendingPhotoDraft(
                    uri = uri,
                    attachmentType = AttachmentType.CAMERA,
                    mimeType = "image/jpeg",
                    gpsLat = exif.gpsLat,
                    gpsLon = exif.gpsLon,
                    capturedAt = exif.capturedAt,
                    deviceModel = exif.deviceModel
                )
            }
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                    subtitle = "Log sources, entities, and field photos. Export to the vault when the session wraps.",
                    compact = true
                )
            }
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Case overview", fontWeight = FontWeight.Bold)
                        Text("Investigator: ${caseItem.leadInvestigator}")
                        Text("Classification: ${caseItem.classification}")
                        Text("Folder: ${caseItem.caseFolderName}")
                        Text("Master note: ${caseItem.masterNoteName}")
                        Text("Folder path: ${caseItem.savePath}", style = MaterialTheme.typography.bodySmall)
                        Text("Essential question", fontWeight = FontWeight.Bold)
                        Text(caseItem.essentialQuestion)
                        Text("Primary subject", fontWeight = FontWeight.Bold)
                        Text(caseItem.primarySubject)
                        Text("Summary so far", fontWeight = FontWeight.Bold)
                        Text(caseItem.summary)
                        Text("Ready to publish when", fontWeight = FontWeight.Bold)
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
                        Text("Add Source")
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
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Camera")
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
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery")
                    }
                }
            }
            item {
                Text("Sources", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            item {
                val allStatuses = listOf(null) + LeadStatus.entries
                ActionChipRow {
                    allStatuses.forEach { status ->
                        val label = status?.name?.lowercase()?.replaceFirstChar(Char::uppercase) ?: "All"
                        TextButton(onClick = { leadStatusFilter = status }) {
                            Text(
                                label,
                                color = if (leadStatusFilter == status) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            val filteredLeads = if (leadStatusFilter == null) state.leads
                                else state.leads.filter { it.status == leadStatusFilter }
            if (filteredLeads.isEmpty()) {
                item {
                    Text(
                        if (leadStatusFilter == null) "No sources logged yet."
                        else "No ${leadStatusFilter!!.name.lowercase()} sources.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(filteredLeads, key = { "lead_${it.id}" }) { lead ->
                LeadCard(
                    lead = lead,
                    onVerify = { viewModel.updateLeadStatus(lead.id, LeadStatus.VERIFIED) },
                    onArchive = { viewModel.updateLeadStatus(lead.id, LeadStatus.ARCHIVED) },
                    onEdit = { leadToEdit = lead },
                    onDelete = { leadToDelete = lead }
                )
            }
            item {
                Text("Entities", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            if (state.entities.isEmpty()) {
                item {
                    Text(
                        "No entities added yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(state.entities, key = { "entity_${it.id}" }) { entity ->
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
                    },
                    onEdit = { entityToEdit = entity },
                    onDelete = { entityToDelete = entity }
                )
            }
            item {
                Text("Attachments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            if (state.attachments.isEmpty()) {
                item {
                    Text(
                        "No photos added to this case yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(state.attachments, key = { "attachment_${it.id}" }) { attachment ->
                AttachmentCard(
                    attachment = attachment,
                    onEdit = { attachmentToEdit = attachment },
                    onDelete = { attachmentToDelete = attachment }
                )
            }
        }
    }

    if (showLeadDialog) {
        AddLeadDialog(
            onDismiss = { showLeadDialog = false },
            title = "Add Source",
            onFetchArchive = viewModel::fetchArchiveUrl,
            onSave = {
                viewModel.addLead(it)
                showLeadDialog = false
            }
        )
    }

    if (showEntityDialog) {
        AddEntityDialog(
            onDismiss = { showEntityDialog = false },
            title = "Add Entity",
            onSave = {
                viewModel.addEntity(it)
                showEntityDialog = false
            }
        )
    }

    val editingLead = leadToEdit
    if (editingLead != null) {
        AddLeadDialog(
            initialLead = editingLead,
            title = "Edit Source",
            onFetchArchive = viewModel::fetchArchiveUrl,
            onDismiss = { leadToEdit = null },
            onSave = {
                viewModel.updateLead(editingLead, it)
                leadToEdit = null
            }
        )
    }

    if (leadToDelete != null) {
        AlertDialog(
            onDismissRequest = { leadToDelete = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLead(leadToDelete?.id ?: return@TextButton)
                        leadToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { leadToDelete = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Delete source?") },
            text = { Text("This removes the source from the case.") }
        )
    }

    val editingEntity = entityToEdit
    if (editingEntity != null) {
        AddEntityDialog(
            initialEntity = editingEntity,
            title = "Edit Entity",
            onDismiss = { entityToEdit = null },
            onSave = {
                viewModel.updateEntity(editingEntity, it)
                entityToEdit = null
            }
        )
    }

    if (entityToDelete != null) {
        AlertDialog(
            onDismissRequest = { entityToDelete = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEntity(entityToDelete?.id ?: return@TextButton)
                        entityToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { entityToDelete = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Delete entity?") },
            text = { Text("This removes the entity from the case.") }
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
                Text("This removes the case and everything saved under it in the app.")
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
                        mimeType = pending.mimeType,
                        caption = caption,
                        attachmentType = pending.attachmentType,
                        gpsLat = pending.gpsLat,
                        gpsLon = pending.gpsLon,
                        capturedAt = pending.capturedAt,
                        deviceModel = pending.deviceModel
                    )
                )
                pendingPhotoDraft = null
            }
        )
    }

    val editingAttachment = attachmentToEdit
    if (editingAttachment != null) {
        EditAttachmentDialog(
            attachment = editingAttachment,
            onDismiss = { attachmentToEdit = null },
            onSave = { caption ->
                viewModel.updateAttachmentCaption(editingAttachment.id, caption)
                attachmentToEdit = null
            }
        )
    }

    if (attachmentToDelete != null) {
        AlertDialog(
            onDismissRequest = { attachmentToDelete = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAttachment(attachmentToDelete?.id ?: return@TextButton)
                        attachmentToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { attachmentToDelete = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Delete attachment?") },
            text = { Text("This removes the attachment record from the app.") }
        )
    }
}

@Composable
private fun LeadCard(
    lead: LeadEntity,
    onVerify: () -> Unit,
    onArchive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(lead.sourceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(lead.summary)
            if (lead.sourceUrl.isNotBlank()) {
                Text("Source: ${lead.sourceUrl}", style = MaterialTheme.typography.bodySmall)
            }
            if (lead.archiveUrl.isNotBlank()) {
                Text("Archive: ${lead.archiveUrl}", style = MaterialTheme.typography.bodySmall)
            }
            if (lead.tags.isNotBlank()) {
                Text("Tags: ${lead.tags}", style = MaterialTheme.typography.bodySmall)
            }
            Text("Logged: ${formatDate(lead.collectedAt)}", style = MaterialTheme.typography.bodySmall)
            Text(
                "Status: ${lead.status.name.lowercase().replaceFirstChar(Char::uppercase)}",
                style = MaterialTheme.typography.bodySmall,
                color = when (lead.status) {
                    LeadStatus.VERIFIED -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            ActionChipRow {
                TextButton(onClick = onVerify) {
                    Text("Mark Verified")
                }
                TextButton(onClick = onArchive) {
                    Text("Archive")
                }
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
                TextButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun EntityCard(
    entity: EntityProfileEntity,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(entity.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "${entity.entityType.name.lowercase().replaceFirstChar(Char::uppercase)} · ${entity.confidence.name.lowercase().replaceFirstChar(Char::uppercase)}",
                color = if (entity.confidence == ConfidenceLevel.VERIFIED) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(entity.summary)
            if (entity.aliases.isNotBlank()) {
                Text("Aliases: ${entity.aliases}", style = MaterialTheme.typography.bodySmall)
            }
            Text("Identifier: ${entity.identifier}", style = MaterialTheme.typography.bodySmall)
            ActionChipRow {
                TextButton(onClick = onExport) {
                    Text("Export Note")
                }
                TextButton(onClick = onShare) {
                    Text("Share Note")
                }
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
                TextButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun AttachmentCard(
    attachment: CaseAttachmentEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                "Captured via: ${attachment.attachmentType.name.lowercase().replaceFirstChar(Char::uppercase)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (attachment.mimeType.isNotBlank()) {
                Text(
                    "Type: ${attachment.mimeType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (attachment.gpsLat != null && attachment.gpsLon != null) {
                Text(
                    "GPS: ${"%.6f".format(attachment.gpsLat)}, ${"%.6f".format(attachment.gpsLon)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (attachment.capturedAt != null) {
                Text(
                    "Captured: ${formatDateTime(attachment.capturedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ActionChipRow {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Text(" Edit")
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Text(" Delete")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionChipRow(
    content: @Composable () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        content()
    }
}

@Composable
private fun AddLeadDialog(
    initialLead: LeadEntity? = null,
    title: String,
    onFetchArchive: suspend (String) -> String?,
    onDismiss: () -> Unit,
    onSave: (LeadDraft) -> Unit
) {
    var sourceName by remember(initialLead?.id) { mutableStateOf(initialLead?.sourceName.orEmpty()) }
    var sourceUrl by remember(initialLead?.id) { mutableStateOf(initialLead?.sourceUrl.orEmpty()) }
    var archiveUrl by remember(initialLead?.id) { mutableStateOf(initialLead?.archiveUrl.orEmpty()) }
    var tags by remember(initialLead?.id) { mutableStateOf(initialLead?.tags.orEmpty()) }
    var summary by remember(initialLead?.id) { mutableStateOf(initialLead?.summary.orEmpty()) }
    var isFetchingArchive by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
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
                            tags = tags,
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
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DictationOutlinedTextField(
                    value = sourceName,
                    onValueChange = { sourceName = it },
                    label = "Source name",
                    onDictate = {
                        dictationTarget = DictationTarget.LEAD_NAME
                        speechLauncher.launch(createSpeechIntent())
                    }
                )
                DictationOutlinedTextField(
                    value = sourceUrl,
                    onValueChange = { sourceUrl = it },
                    label = "Source URL",
                    onDictate = {
                        dictationTarget = DictationTarget.LEAD_URL
                        speechLauncher.launch(createSpeechIntent())
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    DictationOutlinedTextField(
                        value = archiveUrl,
                        onValueChange = { archiveUrl = it },
                        label = "Archive URL",
                        modifier = Modifier.weight(1f),
                        onDictate = {
                            dictationTarget = DictationTarget.LEAD_ARCHIVE_URL
                            speechLauncher.launch(createSpeechIntent())
                        }
                    )
                    IconButton(
                        onClick = {
                            if (sourceUrl.isNotBlank() && !isFetchingArchive) {
                                isFetchingArchive = true
                                scope.launch {
                                    val result = onFetchArchive(sourceUrl)
                                    if (result != null) archiveUrl = result
                                    else Toast.makeText(context, "No archive found.", Toast.LENGTH_SHORT).show()
                                    isFetchingArchive = false
                                }
                            }
                        },
                        enabled = sourceUrl.isNotBlank() && !isFetchingArchive
                    ) {
                        Icon(Icons.Outlined.DownloadDone, contentDescription = "Fetch archive URL")
                    }
                }
                DictationOutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = "Summary",
                    onDictate = {
                        dictationTarget = DictationTarget.LEAD_SUMMARY
                        speechLauncher.launch(createSpeechIntent())
                    }
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags") }
                )
            }
        }
    )
}

@Composable
private fun AddEntityDialog(
    initialEntity: EntityProfileEntity? = null,
    title: String,
    onDismiss: () -> Unit,
    onSave: (EntityDraft) -> Unit
) {
    var name by remember(initialEntity?.id) { mutableStateOf(initialEntity?.name.orEmpty()) }
    var entityType by remember(initialEntity?.id) {
        mutableStateOf(initialEntity?.entityType ?: EntityType.ORGANIZATION)
    }
    var confidence by remember(initialEntity?.id) {
        mutableStateOf(initialEntity?.confidence ?: ConfidenceLevel.PROBABLE)
    }
    var aliases by remember(initialEntity?.id) { mutableStateOf(initialEntity?.aliases.orEmpty()) }
    var summary by remember(initialEntity?.id) { mutableStateOf(initialEntity?.summary.orEmpty()) }
    var identifier by remember(initialEntity?.id) {
        mutableStateOf(initialEntity?.identifier?.ifBlank { "Unknown" } ?: "Unknown")
    }
    val context = LocalContext.current
    var dictationTarget by remember { mutableStateOf(DictationTarget.ENTITY_SUMMARY) }
    val speechLauncher = rememberSpeechToTextLauncher(context) { result ->
        when (dictationTarget) {
            DictationTarget.ENTITY_NAME -> name = appendDictation(name, result)
            DictationTarget.ENTITY_SUMMARY -> summary = appendDictation(summary, result)
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
                            entityType = entityType,
                            confidence = confidence,
                            aliases = aliases,
                            summary = summary,
                            identifier = identifier
                        )
                    )
                },
                enabled = name.isNotBlank() && summary.isNotBlank()
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DictationOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Name",
                    onDictate = {
                        dictationTarget = DictationTarget.ENTITY_NAME
                        speechLauncher.launch(createSpeechIntent())
                    }
                )
                ActionChipRow {
                    EntityType.entries.forEach { type ->
                        TextButton(onClick = { entityType = type }) {
                            Text(
                                text = type.name.lowercase().replaceFirstChar(Char::uppercase),
                                color = if (entityType == type) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
                ActionChipRow {
                    ConfidenceLevel.entries.forEach { level ->
                        TextButton(onClick = { confidence = level }) {
                            Text(
                                text = level.name.lowercase().replaceFirstChar(Char::uppercase),
                                color = if (confidence == level) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
                Text("Role", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ActionChipRow {
                    listOf("Unknown", "Primary Subject", "Person of Interest", "Associate", "Witness", "Source", "Suspect", "Vehicle", "Location", "Organization").forEach { role ->
                        TextButton(onClick = { identifier = role }) {
                            Text(
                                role,
                                color = if (identifier == role) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = aliases,
                    onValueChange = { aliases = it },
                    label = { Text("Aliases") }
                )
                DictationOutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = "Summary",
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
        title = { Text("Add Photo Caption") },
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

@Composable
private fun EditAttachmentDialog(
    attachment: CaseAttachmentEntity,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var caption by remember(attachment.id) { mutableStateOf(attachment.caption) }

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
        title = { Text("Edit Attachment") },
        text = {
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Caption or note") }
            )
        }
    )
}

private fun formatDate(timestamp: Long): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(timestamp))

private fun formatDateTime(timestamp: Long): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
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
    val attachmentType: AttachmentType,
    val mimeType: String,
    val gpsLat: Double? = null,
    val gpsLon: Double? = null,
    val capturedAt: Long? = null,
    val deviceModel: String? = null
) {
    val fileName: String = uri.lastPathSegment?.substringAfterLast('/') ?: "case_attachment.jpg"
}

private data class ExifData(
    val gpsLat: Double?,
    val gpsLon: Double?,
    val capturedAt: Long?,
    val deviceModel: String?
)

private fun extractExifData(context: android.content.Context, uri: Uri): ExifData {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            val latLon = exif.latLong
            val gpsLat = latLon?.getOrNull(0)?.toDouble()
            val gpsLon = latLon?.getOrNull(1)?.toDouble()
            val dateTimeStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            val capturedAt = dateTimeStr?.let { parseExifDateTime(it) }
            val deviceModel = exif.getAttribute(ExifInterface.TAG_MODEL)?.takeIf { it.isNotBlank() }
            ExifData(gpsLat, gpsLon, capturedAt, deviceModel)
        } ?: ExifData(null, null, null, null)
    }.getOrDefault(ExifData(null, null, null, null))
}

private fun resolveMimeType(
    context: android.content.Context,
    uri: Uri
): String? = context.contentResolver.getType(uri)

private fun parseExifDateTime(dateTimeStr: String): Long? =
    runCatching {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
        java.time.LocalDateTime.parse(dateTimeStr, formatter)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()

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
