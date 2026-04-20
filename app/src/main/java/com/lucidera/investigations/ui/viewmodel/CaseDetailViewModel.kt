package com.lucidera.investigations.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lucidera.investigations.data.AttachmentDraft
import com.lucidera.investigations.data.EntityDraft
import com.lucidera.investigations.data.InvestigationRepository
import com.lucidera.investigations.data.LeadDraft
import com.lucidera.investigations.data.LeadStatus
import com.lucidera.investigations.data.local.entity.CaseAttachmentEntity
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
    val entities: List<EntityProfileEntity> = emptyList(),
    val attachments: List<CaseAttachmentEntity> = emptyList()
)

class CaseDetailViewModel(
    private val repository: InvestigationRepository,
    private val caseId: Long
) : ViewModel() {
    val uiState = combine(
        repository.observeCase(caseId),
        repository.observeLeads(caseId),
        repository.observeEntities(caseId),
        repository.observeAttachments(caseId)
    ) { case, leads, entities, attachments ->
            CaseDetailUiState(case = case, leads = leads, entities = entities, attachments = attachments)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CaseDetailUiState())

    fun addLead(draft: LeadDraft) {
        viewModelScope.launch { runCatching { repository.addLead(caseId, draft) } }
    }

    fun addEntity(draft: EntityDraft) {
        viewModelScope.launch { runCatching { repository.addEntity(caseId, draft) } }
    }

    fun addAttachment(draft: AttachmentDraft) {
        viewModelScope.launch { runCatching { repository.addAttachment(caseId, draft) } }
    }

    fun updateLeadStatus(leadId: Long, status: LeadStatus) {
        viewModelScope.launch { runCatching { repository.updateLeadStatus(leadId, status) } }
    }

    fun deleteCase() {
        viewModelScope.launch { runCatching { repository.deleteCase(caseId) } }
    }
}

class CaseDetailViewModelFactory(
    private val repository: InvestigationRepository,
    private val caseId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CaseDetailViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return modelClass.cast(CaseDetailViewModel(repository, caseId))
    }
}
