package com.lucidera.investigations.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lucidera.investigations.data.CaseStatus
import com.lucidera.investigations.data.InvestigationRepository
import com.lucidera.investigations.data.export.CasePackageExporter
import com.lucidera.investigations.data.local.entity.InvestigationCaseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val activeCases: Int = 0,
    val openLeads: Int = 0,
    val verifiedLeads: Int = 0,
    val entitiesTracked: Int = 0,
    val recentCases: List<InvestigationCaseEntity> = emptyList(),
    val userMessage: String? = null
)

class DashboardViewModel(private val repository: InvestigationRepository) : ViewModel() {

    private val messageState = MutableStateFlow<String?>(null)

    val uiState = combine(
        repository.allCases,
        repository.openLeadCount,
        repository.verifiedLeadCount,
        repository.entityCount,
        messageState
    ) { cases, openLeads, verifiedLeads, entityCount, userMessage ->
        DashboardUiState(
            activeCases = cases.count { it.status == CaseStatus.ACTIVE },
            openLeads = openLeads,
            verifiedLeads = verifiedLeads,
            entitiesTracked = entityCount,
            recentCases = cases.take(5),
            userMessage = userMessage
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun exportAllCases(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val cases = repository.allCases.first()
                if (cases.isEmpty()) error("No cases to export.")
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    CasePackageExporter.writeVaultPackage(
                        context = context,
                        outputStream = output,
                        cases = cases,
                        leadsFor = { caseId -> repository.observeLeads(caseId).first() },
                        entitiesFor = { caseId -> repository.observeEntities(caseId).first() },
                        attachmentsFor = { caseId -> repository.observeAttachments(caseId).first() }
                    )
                } ?: error("Could not open export target.")
            }.onSuccess {
                messageState.value = "All cases exported."
            }.onFailure { e ->
                messageState.value = e.message ?: "Export failed."
            }
        }
    }

    fun clearUserMessage() {
        messageState.value = null
    }
}

class DashboardViewModelFactory(
    private val repository: InvestigationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return DashboardViewModel(repository) as T
    }
}
