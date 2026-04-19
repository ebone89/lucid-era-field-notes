package com.lucidera.investigations.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lucidera.investigations.data.EntityDraft
import com.lucidera.investigations.data.InvestigationRepository
import com.lucidera.investigations.data.LeadDraft
import com.lucidera.investigations.data.LeadStatus
import com.lucidera.investigations.data.local.entity.EntityProfileEntity
import com.lucidera.investigations.data.local.entity.InvestigationCaseEntity
import com.lucidera.investigations.data.local.entity.LeadEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CaseDetailUiState(
    val case: InvestigationCaseEntity? = null,
    val leads: List<LeadEntity> = emptyList(),
    val entities: List<EntityProfileEntity> = emptyList()
)

class CaseDetailViewModel(
    private val repository: InvestigationRepository,
    private val caseId: Long
) : ViewModel() {
    val uiState = combine(
        repository.observeCase(caseId),
        repository.observeLeads(caseId),
        repository.observeEntities(caseId)
    ) { case, leads, entities ->
            CaseDetailUiState(case = case, leads = leads, entities = entities)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CaseDetailUiState())

    fun addLead(draft: LeadDraft) {
        viewModelScope.launch {
            repository.addLead(caseId, draft)
        }
    }

    fun addEntity(draft: EntityDraft) {
        viewModelScope.launch {
            repository.addEntity(caseId, draft)
        }
    }

    fun updateLeadStatus(leadId: Long, status: LeadStatus) {
        viewModelScope.launch {
            repository.updateLeadStatus(leadId, status)
        }
    }

    fun deleteCase() {
        viewModelScope.launch {
            repository.deleteCase(caseId)
        }
    }
}

class CaseDetailViewModelFactory(
    private val repository: InvestigationRepository,
    private val caseId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CaseDetailViewModel(repository, caseId) as T
    }
}
