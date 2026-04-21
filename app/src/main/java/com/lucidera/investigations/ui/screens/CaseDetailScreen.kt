package com.lucidera.investigations.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.foundation.layout.fillMaxHeight
import com.lucidera.investigations.ui.components.AudioPlayer
import com.lucidera.investigations.ui.components.AudioRecorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
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
import com.lucidera.investigations.ui.components.LocationHelper
import com.lucidera.investigations.ui.components.LucidEraBrandHeader
import com.lucidera.investigations.ui.components.appendDictation
import com.lucidera.investigations.ui.components.createSpeechIntent
import com.lucidera.investigations.ui.components.rememberSpeechToTextLauncher
import com.lucidera.investigations.ui.viewmodel.CaseDetailViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
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
    var showLeadDialog by rememberSaveable { mutableStateOf(false) }
    var showEntityDialog by rememberSaveable { mutableStateOf(false) }
    var leadStatusFilter by rememberSaveable { mutableStateOf<LeadStatus?>(null) }
    var sectionSort by rememberSaveable { mutableStateOf(CaseSectionSort.NEWEST) }
    var pendingExport by remember { mutableStateOf<ExportDocument?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var pendingPhotoDraft by remember { mutableStateOf<PendingPhotoDraft?>(null) }
    var pendingPackageExport by remember { mutableStateOf<ExportPackage?>(null) }
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var leadToEditId by rememberSaveable { mutableStateOf<Long?>(null) }
    var leadToDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var entityToEditId by rememberSaveable { mutableStateOf<Long?>(null) }
    var entityToDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var attachmentToEditId by rememberSaveable { mutableStateOf<Long?>(null) }
    var attachmentToDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val context = LocalContext.current
    var isRecording by rememberSaveable { mutableStateOf(false) }
    var audioFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    val recorder = remember { AudioRecorder(context) }
    val player = remember { AudioPlayer(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val dialogStateHolder = rememberSaveableStateHolder()
    val audioFile = audioFilePath?.let(::File)
    val leadToEdit = leadToEditId?.let { id -> state.leads.firstOrNull { it.id == id } }
    val leadToDelete = leadToDeleteId?.let { id -> state.leads.firstOrNull { it.id == id } }
    val entityToEdit = entityToEditId?.let { id -> state.entities.firstOrNull { it.id == id } }
    val entityToDelete = entityToDeleteId?.let { id -> state.entities.firstOrNull { it.id == id } }
    val attachmentToEdit = attachmentToEditId?.let { id -> state.attachments.firstOrNull { it.id == id } }
    val attachmentToDelete = attachmentToDeleteId?.let { id -> state.attachments.firstOrNull { it.id == id } }

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
                    deviceModel = exif.deviceModel,
                    fileHash = calculateFileHash(context, storedUri)
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
                    deviceModel = exif.deviceModel,
                    fileHash = calculateFileHash(context, uri)
                )
            }
        }
    }
    val scannerOptions = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)
        .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
        .setScannerMode(SCANNER_MODE_FULL)
        .build()
    val scanner = GmsDocumentScanning.getClient(scannerOptions)
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanningResult?.pages?.firstOrNull()?.let { page ->
                val uri = page.imageUri
                val exif = extractExifData(context, uri)
                // Document scanner images are already optimized/cropped
                pendingPhotoDraft = PendingPhotoDraft(
                    uri = uri,
                    attachmentType = AttachmentType.CAMERA, // Treat as camera/field capture
                    mimeType = "image/jpeg",
                    gpsLat = exif.gpsLat,
                    gpsLon = exif.gpsLon,
                    capturedAt = exif.capturedAt,
                    deviceModel = exif.deviceModel,
                    fileHash = calculateFileHash(context, uri)
                )
            }
        }
    }
    fun startAudioRecording() {
        val file = createCaseAudioFile(context)
        audioFilePath = file.absolutePath
        if (recorder.startRecording(file)) {
            isRecording = true
        } else {
            audioFilePath = null
            Toast.makeText(context, "Could not start recording.", Toast.LENGTH_LONG).show()
        }
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startAudioRecording()
        } else {
            Toast.makeText(context, "Microphone permission is required to record audio.", Toast.LENGTH_LONG).show()
        }
    }

    var showMapDialog by remember { mutableStateOf(false) }

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
                    val leadsWithLocation = state.leads.filter { it.latitude != null && it.longitude != null }
                    if (leadsWithLocation.isNotEmpty()) {
                        IconButton(onClick = { showMapDialog = true }) {
                            Icon(Icons.Outlined.Map, contentDescription = "View Source Map")
                        }
                    }
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
                                    pendingPackageExport = ExportPackage(
                                        label = "Case package",
                                        fileName = "${caseItem.caseCode}_package.zip",
                                        markdownFileName = caseItem.masterNoteName,
                                        markdown = markdown,
                                        attachments = state.attachments
                                    )
                                    showOverflowMenu = false
                                    packageExportLauncher.launch("${caseItem.caseCode}_package.zip")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Session Log") },
                                onClick = {
                                    val fileName = "Session_Log_${caseItem.caseCode}_${System.currentTimeMillis()}.md"
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
                                text = {
                                    Text(
                                        "Delete Case",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showDeleteDialog = true
                                    showOverflowMenu = false
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
                            runCatching {
                                val file = createCaseImageFile(context)
                                FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                            }.onSuccess { uri ->
                                cameraUri = uri
                                cameraCapture.launch(uri)
                            }.onFailure {
                                Toast.makeText(context, "Camera failed: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Camera")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val activity = context.findActivity()
                            if (activity == null) {
                                Toast.makeText(context, "Scanner is unavailable from this screen.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scanner.getStartScanIntent(activity)
                                .addOnSuccessListener { intentSender ->
                                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Scanner failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    ) {
                        Icon(Icons.Outlined.DocumentScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan Doc")
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
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (!isRecording) {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    startAudioRecording()
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            } else {
                                val stopped = recorder.stopRecording()
                                isRecording = false
                                audioFilePath = null
                                if (!stopped) {
                                    Toast.makeText(context, "Could not finish recording.", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                audioFile?.let { file ->
                                    runCatching {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        PendingPhotoDraft(
                                            uri = uri,
                                            attachmentType = AttachmentType.AUDIO,
                                            mimeType = "audio/mp4",
                                            gpsLat = null,
                                            gpsLon = null,
                                            capturedAt = System.currentTimeMillis(),
                                            deviceModel = android.os.Build.MODEL,
                                            fileHash = calculateFileHash(context, uri)
                                        )
                                    }.onSuccess { draft ->
                                        pendingPhotoDraft = draft
                                    }.onFailure {
                                        Toast.makeText(context, "Could not attach recording.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    ) {
                        if (isRecording) {
                            var seconds by remember { mutableStateOf(0) }
                            var tick by remember { mutableStateOf(0) }
                            LaunchedEffect(Unit) {
                                while (true) {
                                    kotlinx.coroutines.delay(200)
                                    tick++
                                    if (tick % 5 == 0) seconds++
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Stop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Row(
                                    modifier = Modifier.height(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    for (i in 0 until 5) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .fillMaxHeight(0.2f + (Math.random().toFloat() * 0.8f))
                                                .background(MaterialTheme.colorScheme.error)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = java.util.Locale.getDefault().let { locale ->
                                        String.format(locale, "%02d:%02d", seconds / 60, seconds % 60)
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                )
                            }
                        } else {
                            Icon(Icons.Outlined.Mic, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Record")
                        }
                    }
                }
            }
            item {
                Text("Sort By", style = MaterialTheme.typography.labelMedium)
            }
            item {
                ActionChipRow {
                    CaseSectionSort.entries.forEach { sort ->
                        TextButton(onClick = { sectionSort = sort }) {
                            Text(
                                sort.label,
                                color = if (sectionSort == sort) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item {
                Text("Source Status", style = MaterialTheme.typography.labelMedium)
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
            item {
                Text("Sources", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            val filteredLeads = if (leadStatusFilter == null) state.leads
                                else state.leads.filter { it.status == leadStatusFilter }
            val sortedLeads = sortLeads(filteredLeads, sectionSort)
            val sortedEntities = sortEntities(state.entities, sectionSort)
            if (sortedLeads.isEmpty()) {
                item {
                    Text(
                        if (leadStatusFilter == null) "No sources logged yet."
                        else "No ${leadStatusFilter!!.name.lowercase()} sources.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(sortedLeads, key = { "lead_${it.id}" }) { lead ->
                LeadCard(
                    lead = lead,
                    onVerify = { viewModel.updateLeadStatus(lead.id, LeadStatus.VERIFIED) },
                    onArchive = { viewModel.updateLeadStatus(lead.id, LeadStatus.ARCHIVED) },
                    onEdit = { leadToEditId = lead.id },
                    onDelete = { leadToDeleteId = lead.id }
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
            items(sortedEntities, key = { "entity_${it.id}" }) { entity ->
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
                    onEdit = { entityToEditId = entity.id },
                    onDelete = { entityToDeleteId = entity.id }
                )
            }
            item {
                Text("Attachments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            if (state.attachments.isEmpty()) {
                item {
                    Text(
                        "No attachments added to this case yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(state.attachments, key = { "attachment_${it.id}" }) { attachment ->
                val speechLauncher = rememberSpeechToTextLauncher { text, _ ->
                    viewModel.updateTranscription(attachment.id, text)
                }
                AttachmentCard(
                    attachment = attachment,
                    onEdit = { attachmentToEditId = attachment.id },
                    onDelete = { attachmentToDeleteId = attachment.id },
                    onTranscribe = {
                        speechLauncher.launch(createSpeechIntent())
                    },
                    onPlay = {
                        player.playUri(Uri.parse(attachment.uri))
                    }
                )
            }
        }
    }

    if (showLeadDialog) {
        val scope = rememberCoroutineScope()
        dialogStateHolder.SaveableStateProvider("add_lead_dialog") {
            AddLeadDialog(
                onDismiss = { showLeadDialog = false },
                title = "Add Source",
                onFetchArchive = { url ->
                    scope.launch {
                        viewModel.fetchArchiveUrl(url)
                    }
                },
                onSave = {
                    viewModel.addLead(it)
                    showLeadDialog = false
                }
            )
        }
    }

    if (showEntityDialog) {
        dialogStateHolder.SaveableStateProvider("add_entity_dialog") {
            AddEntityDialog(
                onDismiss = { showEntityDialog = false },
                title = "Add Entity",
                onSave = {
                    viewModel.addEntity(it)
                    showEntityDialog = false
                }
            )
        }
    }

    if (leadToEdit != null) {
        val editingLead = leadToEdit
        val scope = rememberCoroutineScope()
        dialogStateHolder.SaveableStateProvider("edit_lead_dialog_${editingLead.id}") {
            AddLeadDialog(
                initialLead = editingLead,
                title = "Edit Source",
                onFetchArchive = { url ->
                    scope.launch {
                        viewModel.fetchArchiveUrl(url)
                    }
                },
                onDismiss = { leadToEditId = null },
                onSave = {
                    viewModel.updateLead(editingLead, it)
                    leadToEditId = null
                }
            )
        }
    }

    if (leadToDelete != null) {
        AlertDialog(
            onDismissRequest = { leadToDeleteId = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLead(leadToDelete.id)
                        leadToDeleteId = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { leadToDeleteId = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Delete source?") },
            text = { Text("This removes the source from the case.") }
        )
    }

    if (entityToEdit != null) {
        val editingEntity = entityToEdit
        dialogStateHolder.SaveableStateProvider("edit_entity_dialog_${editingEntity.id}") {
            AddEntityDialog(
                initialEntity = editingEntity,
                title = "Edit Entity",
                onDismiss = { entityToEditId = null },
                onSave = {
                    viewModel.updateEntity(editingEntity, it)
                    entityToEditId = null
                }
            )
        }
    }

    if (entityToDelete != null) {
        AlertDialog(
            onDismissRequest = { entityToDeleteId = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEntity(entityToDelete.id)
                        entityToDeleteId = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { entityToDeleteId = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Delete entity?") },
            text = { Text("This removes the entity from the case.") }
        )
    }

    if (showMapDialog) {
        val leadsWithLocation = state.leads.filter { it.latitude != null && it.longitude != null }
        if (leadsWithLocation.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { showMapDialog = false },
                confirmButton = {
                    TextButton(onClick = { showMapDialog = false }) {
                        Text("Close")
                    }
                },
                title = { Text("Source Geocoordinates") },
                text = {
                    val firstLoc = leadsWithLocation.first()
                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(
                            LatLng(firstLoc.latitude!!, firstLoc.longitude!!),
                            12f
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    ) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState
                        ) {
                            leadsWithLocation.forEach { lead ->
                                Marker(
                                    state = MarkerState(
                                        position = LatLng(
                                            lead.latitude!!,
                                            lead.longitude!!
                                        )
                                    ),
                                    title = lead.sourceName,
                                    snippet = lead.summary
                                )
                            }
                        }
                    }
                }
            )
        }
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
        dialogStateHolder.SaveableStateProvider("add_photo_caption_dialog") {
            AddPhotoCaptionDialog(
                attachmentType = pendingPhotoDraft!!.attachmentType,
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
                            deviceModel = pending.deviceModel,
                            fileHash = pending.fileHash
                        )
                    )
                    pendingPhotoDraft = null
                }
            )
        }
    }

    if (attachmentToEdit != null) {
        val editingAttachment = attachmentToEdit
        dialogStateHolder.SaveableStateProvider("edit_attachment_dialog_${editingAttachment.id}") {
            EditAttachmentDialog(
                attachment = editingAttachment,
                onDismiss = { attachmentToEditId = null },
                onSave = { caption ->
                    viewModel.updateAttachmentCaption(editingAttachment.id, caption)
                    attachmentToEditId = null
                }
            )
        }
    }

    if (attachmentToDelete != null) {
        AlertDialog(
            onDismissRequest = { attachmentToDeleteId = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAttachment(attachmentToDelete.id)
                        attachmentToDeleteId = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { attachmentToDeleteId = null }) {
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
            if (lead.latitude != null && lead.longitude != null) {
                Text("GPS: ${lead.latitude}, ${lead.longitude}", style = MaterialTheme.typography.bodySmall)
            }
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
                "${entity.entityType.name.lowercase().replaceFirstChar(Char::uppercase)} \u00b7 ${entity.confidence.name.lowercase().replaceFirstChar(Char::uppercase)}",
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
    onDelete: () -> Unit,
    onTranscribe: () -> Unit,
    onPlay: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (attachment.attachmentType != AttachmentType.AUDIO) {
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
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(attachment.fileName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (attachment.caption.isNotBlank()) {
                Text(attachment.caption)
            }
            if (!attachment.transcription.isNullOrBlank()) {
                Text(
                    text = "Transcription:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = attachment.transcription,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
            Text(
                "Captured via: ${attachment.attachmentType.name.lowercase().replaceFirstChar(Char::uppercase)}",
                style = MaterialTheme.typography.bodySmall
            )
            if (attachment.fileHash != null) {
                Text(
                    "SHA-256: ${attachment.fileHash.take(16)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("Saved: ${formatDate(attachment.createdAt)}", style = MaterialTheme.typography.bodySmall)
            if (attachment.capturedAt != null) {
                Text("Image date: ${formatDate(attachment.capturedAt)}", style = MaterialTheme.typography.bodySmall)
            }
            if (attachment.deviceModel != null) {
                Text("Device: ${attachment.deviceModel}", style = MaterialTheme.typography.bodySmall)
            }
            if (attachment.gpsLat != null && attachment.gpsLon != null) {
                Text("GPS: ${attachment.gpsLat}, ${attachment.gpsLon}", style = MaterialTheme.typography.bodySmall)
            }
            ActionChipRow {
                if (attachment.attachmentType == AttachmentType.AUDIO) {
                    TextButton(onClick = onPlay) {
                        Text("Play")
                    }
                    TextButton(onClick = onTranscribe) {
                        Text(if (attachment.transcription.isNullOrBlank()) "Transcribe" else "Re-transcribe")
                    }
                }
                TextButton(onClick = onEdit) {
                    Text("Edit Caption")
                }
                TextButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun ActionChipRow(content: @Composable () -> Unit) {
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun AddLeadDialog(
    initialLead: LeadEntity? = null,
    title: String,
    onFetchArchive: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (LeadDraft) -> Unit
) {
    var sourceName by rememberSaveable(initialLead?.id) { mutableStateOf(initialLead?.sourceName ?: "") }
    var summary by rememberSaveable(initialLead?.id) { mutableStateOf(initialLead?.summary ?: "") }
    var sourceUrl by rememberSaveable(initialLead?.id) { mutableStateOf(initialLead?.sourceUrl ?: "") }
    var archiveUrl by rememberSaveable(initialLead?.id) { mutableStateOf(initialLead?.archiveUrl ?: "") }
    var tags by rememberSaveable(initialLead?.id) { mutableStateOf(initialLead?.tags ?: "") }
    var status by rememberSaveable(initialLead?.id) { mutableStateOf(initialLead?.status ?: LeadStatus.OPEN) }
    var latitude by rememberSaveable(initialLead?.id) { mutableStateOf(initialLead?.latitude) }
    var longitude by rememberSaveable(initialLead?.id) { mutableStateOf(initialLead?.longitude) }
    var isLocating by rememberSaveable(initialLead?.id) { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val speechLauncher = rememberSpeechToTextLauncher { text, target ->
        when (target as? DictationTarget) {
            DictationTarget.LEAD_NAME -> sourceName = appendDictation(sourceName, text)
            DictationTarget.LEAD_SUMMARY -> summary = appendDictation(summary, text)
            DictationTarget.LEAD_URL -> sourceUrl = appendDictation(sourceUrl, text)
            DictationTarget.LEAD_ARCHIVE_URL -> archiveUrl = appendDictation(archiveUrl, text)
            else -> {}
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DictationOutlinedTextField(
                    value = sourceName,
                    onValueChange = { sourceName = it },
                    label = { Text("Source Name") },
                    onDictate = { speechLauncher.launch(createSpeechIntent(), DictationTarget.LEAD_NAME) }
                )
                DictationOutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Summary/Key Takeaway") },
                    onDictate = { speechLauncher.launch(createSpeechIntent(), DictationTarget.LEAD_SUMMARY) }
                )
                DictationOutlinedTextField(
                    value = sourceUrl,
                    onValueChange = { sourceUrl = it },
                    label = { Text("Source URL") },
                    onDictate = { speechLauncher.launch(createSpeechIntent(), DictationTarget.LEAD_URL) }
                )
                if (sourceUrl.isNotBlank()) {
                    TextButton(onClick = { onFetchArchive(sourceUrl) }) {
                        Text("Check Wayback Machine")
                    }
                }
                DictationOutlinedTextField(
                    value = archiveUrl,
                    onValueChange = { archiveUrl = it },
                    label = { Text("Archive URL") },
                    onDictate = { speechLauncher.launch(createSpeechIntent(), DictationTarget.LEAD_ARCHIVE_URL) }
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma separated)") }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(onClick = {
                            scope.launch {
                                isLocating = true
                                val loc = LocationHelper.getCurrentLocation(context)
                                if (loc != null) {
                                    latitude = loc.latitude
                                    longitude = loc.longitude
                                } else {
                                    Toast.makeText(context, "Could not acquire location.", Toast.LENGTH_SHORT).show()
                                }
                                isLocating = false
                            }
                        }) {
                            Icon(Icons.Outlined.MyLocation, contentDescription = "Tag current location")
                        }
                    }
                    Column {
                        if (latitude != null && longitude != null) {
                            Text("Location: ${latitude?.toString()?.take(8)}, ${longitude?.toString()?.take(8)}",
                                style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { latitude = null; longitude = null }) {
                                Text("Clear location", style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            Text("No location tagged", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(LeadDraft(
                        sourceName = sourceName,
                        summary = summary,
                        sourceUrl = sourceUrl,
                        archiveUrl = archiveUrl,
                        status = status,
                        tags = tags,
                        latitude = latitude,
                        longitude = longitude
                    ))
                },
                enabled = sourceName.isNotBlank() && summary.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
    var name by rememberSaveable(initialEntity?.id) { mutableStateOf(initialEntity?.name ?: "") }
    var entityType by rememberSaveable(initialEntity?.id) { mutableStateOf(initialEntity?.entityType ?: EntityType.PERSON) }
    var confidence by rememberSaveable(initialEntity?.id) { mutableStateOf(initialEntity?.confidence ?: ConfidenceLevel.UNCONFIRMED) }
    var summary by rememberSaveable(initialEntity?.id) { mutableStateOf(initialEntity?.summary ?: "") }
    var identifier by rememberSaveable(initialEntity?.id) { mutableStateOf(initialEntity?.identifier ?: "") }
    var aliases by rememberSaveable(initialEntity?.id) { mutableStateOf(initialEntity?.aliases ?: "") }

    val speechLauncher = rememberSpeechToTextLauncher { text, target ->
        when (target as? DictationTarget) {
            DictationTarget.ENTITY_NAME -> name = appendDictation(name, text)
            DictationTarget.ENTITY_SUMMARY -> summary = appendDictation(summary, text)
            DictationTarget.ENTITY_IDENTIFIER -> identifier = appendDictation(identifier, text)
            else -> {}
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DictationOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name/Entity Title") },
                    onDictate = { speechLauncher.launch(createSpeechIntent(), DictationTarget.ENTITY_NAME) }
                )
                OutlinedTextField(
                    value = aliases,
                    onValueChange = { aliases = it },
                    label = { Text("Aliases (comma separated)") }
                )
                DictationOutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text("Unique Identifier (SSN, VIN, ID, etc.)") },
                    onDictate = { speechLauncher.launch(createSpeechIntent(), DictationTarget.ENTITY_IDENTIFIER) }
                )
                DictationOutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Summary") },
                    onDictate = { speechLauncher.launch(createSpeechIntent(), DictationTarget.ENTITY_SUMMARY) }
                )
                Text("Type", style = MaterialTheme.typography.labelMedium)
                ActionChipRow {
                    EntityType.entries.forEach { type ->
                        TextButton(onClick = { entityType = type }) {
                            Text(
                                type.name.lowercase().replaceFirstChar(Char::uppercase),
                                color = if (entityType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text("Confidence", style = MaterialTheme.typography.labelMedium)
                ActionChipRow {
                    ConfidenceLevel.entries.forEach { level ->
                        TextButton(onClick = { confidence = level }) {
                            Text(
                                level.name.lowercase().replaceFirstChar(Char::uppercase),
                                color = if (confidence == level) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(EntityDraft(name, entityType, confidence, summary, identifier, aliases))
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
        }
    )
}

@Composable
private fun AddPhotoCaptionDialog(
    attachmentType: AttachmentType,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var caption by rememberSaveable { mutableStateOf("") }
    val title = attachmentDialogTitle(attachmentType)
    val label = attachmentFieldLabel(attachmentType)
    val confirmText = attachmentSaveLabel(attachmentType)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text(label) }
            )
        },
        confirmButton = {
            Button(onClick = { onSave(caption) }) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
    var caption by rememberSaveable(attachment.id) { mutableStateOf(attachment.caption) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Caption") },
        text = {
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Caption") }
            )
        },
        confirmButton = {
            Button(onClick = { onSave(caption) }) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDate(millis: Long): String {
    val instant = Instant.ofEpochMilli(millis)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

private fun createCaseAudioFile(context: Context): File {
    val dir = File(context.cacheDir, "recordings").apply { mkdirs() }
    return File.createTempFile("REC_", ".mp4", dir)
}

private fun createCaseImageFile(context: android.content.Context): File {
    val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
    return File.createTempFile("IMG_", ".jpg", dir)
}

private enum class CaseSectionSort(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    A_TO_Z("A-Z"),
    Z_TO_A("Z-A")
}

private fun sortLeads(leads: List<LeadEntity>, sort: CaseSectionSort): List<LeadEntity> = when (sort) {
    CaseSectionSort.NEWEST -> leads.sortedByDescending { it.collectedAt }
    CaseSectionSort.OLDEST -> leads.sortedBy { it.collectedAt }
    CaseSectionSort.A_TO_Z -> leads.sortedBy { it.sourceName.lowercase() }
    CaseSectionSort.Z_TO_A -> leads.sortedByDescending { it.sourceName.lowercase() }
}

private fun sortEntities(
    entities: List<EntityProfileEntity>,
    sort: CaseSectionSort
): List<EntityProfileEntity> = when (sort) {
    CaseSectionSort.NEWEST -> entities.sortedByDescending { it.id }
    CaseSectionSort.OLDEST -> entities.sortedBy { it.id }
    CaseSectionSort.A_TO_Z -> entities.sortedBy { it.name.lowercase() }
    CaseSectionSort.Z_TO_A -> entities.sortedByDescending { it.name.lowercase() }
}

private fun attachmentDialogTitle(type: AttachmentType): String = when (type) {
    AttachmentType.AUDIO -> "Add Recording Notes"
    else -> "Add Photo Caption"
}

private fun attachmentFieldLabel(type: AttachmentType): String = when (type) {
    AttachmentType.AUDIO -> "Notes"
    else -> "Caption"
}

private fun attachmentSaveLabel(type: AttachmentType): String = when (type) {
    AttachmentType.AUDIO -> "Save Recording"
    else -> "Save Photo"
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun persistGalleryImage(context: android.content.Context, uri: Uri): Uri? {
    val fileName = "GALLERY_${System.currentTimeMillis()}.jpg"
    val targetFile = File(context.getExternalFilesDir(null), fileName)
    return runCatching {
        context.contentResolver.openInputStream(uri).useToCopy(targetFile)
        Uri.fromFile(targetFile)
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
    val deviceModel: String? = null,
    val fileHash: String? = null
) {
    val fileName: String = uri.lastPathSegment?.substringAfterLast('/') ?: "case_attachment.jpg"
}

private data class ExifData(
    val gpsLat: Double?,
    val gpsLon: Double?,
    val capturedAt: Long?,
    val deviceModel: String?
)

private fun calculateFileHash(context: android.content.Context, uri: Uri): String? = runCatching {
    val digest = MessageDigest.getInstance("SHA-256")
    context.contentResolver.openInputStream(uri)?.use { input ->
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}.getOrNull()

private fun extractExifData(context: android.content.Context, uri: Uri): ExifData {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            val latLon = exif.latLong
            val gpsLat = latLon?.getOrNull(0)
            val gpsLon = latLon?.getOrNull(1)
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
        val formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
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
