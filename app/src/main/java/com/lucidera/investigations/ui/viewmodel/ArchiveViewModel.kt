package com.lucidera.investigations.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lucidera.investigations.data.InvestigationRepository
import com.lucidera.investigations.data.LeadDraft
import com.lucidera.investigations.data.LeadStatus
import com.lucidera.investigations.data.local.entity.InvestigationCaseEntity
import com.lucidera.investigations.data.network.WaybackLookupResult
import java.net.URI
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArchiveUiState(
    val isLoading: Boolean = false,
    val result: WaybackLookupResult? = null,
    val errorMessage: String? = null,
    val cases: List<InvestigationCaseEntity> = emptyList(),
    val savedMessage: String? = null
)

class ArchiveViewModel(
    private val repository: InvestigationRepository
) : ViewModel() {
    private val lookupState = MutableStateFlow(ArchiveUiState())
    val uiState = combine(lookupState, repository.allCases) { state, cases ->
        state.copy(cases = cases)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArchiveUiState())

    fun lookupArchive(url: String) {
        viewModelScope.launch {
            lookupState.update { it.copy(isLoading = true, result = null, errorMessage = null, savedMessage = null) }
            runCatching {
                repository.lookupArchive(url)
            }.onSuccess { result ->
                lookupState.update { it.copy(isLoading = false, result = result, errorMessage = null) }
            }.onFailure { error ->
                lookupState.update {
                    it.copy(
                        isLoading = false,
                        result = null,
                        errorMessage = error.message ?: "Could not find a saved snapshot."
                    )
                }
            }
        }
    }

    fun addSnapshotToCase(caseId: Long, result: WaybackLookupResult, note: String = "") {
        addSourceToCase(
            caseId = caseId,
            sourceUrl = result.originalUrl,
            archiveUrl = result.archiveUrl,
            note = note
        )
    }

    fun addSourceToCase(
        caseId: Long,
        sourceUrl: String,
        archiveUrl: String = "",
        note: String = ""
    ) {
        viewModelScope.launch {
            val sourceName = buildSourceName(sourceUrl)
            val summary = listOf(
                if (archiveUrl.isNotBlank()) {
                    "Saved source from $sourceName with an archive link."
                } else {
                    "Saved source from $sourceName."
                },
                note.trim().takeIf { it.isNotBlank() }
            ).joinToString(" ")

            runCatching {
                repository.addLead(
                    caseId = caseId,
                    draft = LeadDraft(
                        sourceName = sourceName,
                        sourceUrl = sourceUrl,
                        archiveUrl = archiveUrl,
                        summary = summary,
                        status = LeadStatus.OPEN
                    )
                )
            }.onSuccess {
                lookupState.update { it.copy(savedMessage = "Saved to case.") }
            }.onFailure { error ->
                lookupState.update { it.copy(savedMessage = error.message ?: "Could not save that source.") }
            }
        }
    }

    fun clearSavedMessage() {
        lookupState.update { it.copy(savedMessage = null) }
    }

    private fun buildSourceName(url: String): String {
        val host = runCatching { URI(url).host.orEmpty() }.getOrDefault("")
        return host.removePrefix("www.").ifBlank { "Archived source" }
    }
}

class ArchiveViewModelFactory(
    private val repository: InvestigationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ArchiveViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return modelClass.cast(ArchiveViewModel(repository))
    }
}
