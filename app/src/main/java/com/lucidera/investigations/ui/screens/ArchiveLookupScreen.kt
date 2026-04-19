package com.lucidera.investigations.ui.screens

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                subtitle = "Use this before you start taking notes on a page. The archive link is the evidence trail.",
                compact = true
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
                Text("Look up snapshot")
            }

            when {
                state.isLoading -> Box(modifier = Modifier.fillMaxWidth()) { CircularProgressIndicator() }
                state.errorMessage != null -> {
                    Card {
                        Text(
                            text = state.errorMessage ?: "",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                state.result != null -> {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Snapshot Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            ArchiveResultRow(label = "Original URL", value = state.result?.originalUrl.orEmpty())
                            ArchiveResultRow(label = "Archive URL", value = state.result?.archiveUrl.orEmpty())
                            ArchiveResultRow(label = "Timestamp", value = state.result?.timestamp.orEmpty())
                            ArchiveResultRow(label = "Status", value = state.result?.status.orEmpty())
                        }
                    }
                }
            }
        }
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
