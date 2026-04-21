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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CaseDetailUiState(
    val case: InvestigationCaseEntity? = null,
    val leads: List<LeadEntity> = emptyList(),
    val entities: List<EntityProfileEntity> = emptyList(),
    val attachments: List<CaseAttachmentEntity> = emptyList(),
    val userMessage: String? = null
)

class CaseDetailViewModel(
    private val repository: InvestigationRepository,
    private val caseId: Long
) : ViewModel() {
    private val messageState = MutableStateFlow<String?>(null)

    val uiState = combine(
        repository.observeCase(caseId),
        repository.observeLeads(caseId),
        repository.observeEntities(caseId),
        repository.observeAttachments(caseId),
        messageState
    ) { case, leads, entities, attachments, userMessage ->
            CaseDetailUiState(
                case = case,
                leads = leads,
                entities = entities,
                attachments = attachments,
                userMessage = userMessage
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CaseDetailUiState())

    fun addLead(draft: LeadDraft) {
        viewModelScope.launch {
            runCatching { repository.addLead(caseId, draft) }
                .onSuccess { messageState.value = "Source saved." }
                .onFailure { messageState.value = it.message ?: "Could not save source." }
        }
    }

    fun updateLead(lead: LeadEntity, draft: LeadDraft) {
        viewModelScope.launch {
            runCatching { repository.updateLead(lead, draft) }
                .onSuccess { messageState.value = "Source updated." }
                .onFailure { messageState.value = it.message ?: "Could not update source." }
        }
    }

    fun deleteLead(leadId: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteLead(leadId) }
                .onSuccess { messageState.value = "Source deleted." }
                .onFailure { messageState.value = it.message ?: "Could not delete source." }
        }
    }

    fun addEntity(draft: EntityDraft) {
        viewModelScope.launch {
            runCatching { repository.addEntity(caseId, draft) }
                .onSuccess { messageState.value = "Entity saved." }
                .onFailure { messageState.value = it.message ?: "Could not save entity." }
        }
    }

    fun updateEntity(entity: EntityProfileEntity, draft: EntityDraft) {
        viewModelScope.launch {
            runCatching { repository.updateEntity(entity, draft) }
                .onSuccess { messageState.value = "Entity updated." }
                .onFailure { messageState.value = it.message ?: "Could not update entity." }
        }
    }

    fun deleteEntity(entityId: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteEntity(entityId) }
                .onSuccess { messageState.value = "Entity deleted." }
                .onFailure { messageState.value = it.message ?: "Could not delete entity." }
        }
    }

    fun addAttachment(draft: AttachmentDraft) {
        viewModelScope.launch {
            runCatching { repository.addAttachment(caseId, draft) }
                .onSuccess { messageState.value = "Attachment saved." }
                .onFailure { messageState.value = it.message ?: "Could not save attachment." }
        }
    }

    fun updateLeadStatus(leadId: Long, status: LeadStatus) {
        viewModelScope.launch {
            runCatching { repository.updateLeadStatus(leadId, status) }
                .onSuccess {
                    val statusLabel = status.name.lowercase().replaceFirstChar(Char::uppercase)
                    messageState.value = "Source marked $statusLabel."
                }
                .onFailure { messageState.value = it.message ?: "Could not update source." }
        }
    }

    fun updateAttachmentCaption(attachmentId: Long, caption: String) {
        viewModelScope.launch {
            runCatching { repository.updateAttachmentCaption(attachmentId, caption) }
                .onSuccess { messageState.value = "Attachment updated." }
                .onFailure { messageState.value = it.message ?: "Could not update attachment." }
        }
    }

    fun deleteAttachment(attachmentId: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteAttachment(attachmentId) }
                .onSuccess { messageState.value = "Attachment deleted." }
                .onFailure { messageState.value = it.message ?: "Could not delete attachment." }
        }
    }

    fun deleteCase() {
        viewModelScope.launch {
            runCatching { repository.deleteCase(caseId) }
                .onFailure { messageState.value = it.message ?: "Could not delete case." }
        }
    }

    suspend fun fetchArchiveUrl(sourceUrl: String): String? = runCatching {
        repository.lookupArchive(sourceUrl).archiveUrl
    }.getOrNull()

    fun clearUserMessage() {
        messageState.update { null }
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
