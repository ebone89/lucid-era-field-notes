package com.lucidera.investigations.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.lucidera.investigations.data.export.CasePackageExporter
import com.lucidera.investigations.data.local.CryptoManager
import com.lucidera.investigations.ui.components.LucidEraBrandHeader
import com.lucidera.investigations.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenCases: () -> Unit,
    onOpenArchive: () -> Unit,
    onCaseSelected: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showPanicDialog by remember { mutableStateOf(false) }

    val exportAllLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.exportAllCases(context, it) }
    }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LucidEraBrandHeader(
                    title = "Lucid Era Field Notes",
                    subtitle = "Active cases, lead counts, and recent activity at a glance."
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Active Cases",
                        value = state.activeCases.toString()
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Open Leads",
                        value = state.openLeads.toString()
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Verified Leads",
                        value = state.verifiedLeads.toString()
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Entity Profiles",
                        value = state.entitiesTracked.toString()
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(onClick = onOpenCases, modifier = Modifier.weight(1f)) {
                        Text("Cases")
                    }
                    Button(onClick = onOpenArchive, modifier = Modifier.weight(1f)) {
                        Text("Archive")
                    }
                    Button(
                        onClick = { exportAllLauncher.launch(CasePackageExporter.buildVaultPackageFileName()) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export All")
                    }
                }
            }
            item {
                Button(
                    onClick = { showPanicDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Outlined.Security, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Panic Mode (Nuke Data)")
                }
            }
            item {
                Text(
                    text = "Recent Cases",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            items(state.recentCases, key = { it.id }) { caseItem ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCaseSelected(caseItem.id) }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(caseItem.caseCode, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
                        Text(caseItem.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(caseItem.summary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (showPanicDialog) {
            AlertDialog(
                onDismissRequest = { showPanicDialog = false },
                title = { Text("CONFIRM DATA DESTRUCTION") },
                text = { Text("This will PERMANENTLY delete all cases, sources, and media. The database will be nuked and the app will close. This cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            CryptoManager.nukeEverything(context)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("NUKE EVERYTHING")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPanicDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
