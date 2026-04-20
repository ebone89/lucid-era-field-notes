package com.lucidera.investigations.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lucidera.investigations.data.CaseDraft
import com.lucidera.investigations.data.InvestigationRepository
import com.lucidera.investigations.data.local.entity.InvestigationCaseEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CasesUiState(
    val cases: List<InvestigationCaseEntity> = emptyList(),
    val userMessage: String? = null
)

class CasesViewModel(
    private val repository: InvestigationRepository
) : ViewModel() {
    private val messageState = MutableStateFlow<String?>(null)

    val uiState = combine(repository.allCases, messageState) { cases, userMessage ->
        CasesUiState(cases = cases, userMessage = userMessage)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CasesUiState())

    fun addCase(draft: CaseDraft) {
        viewModelScope.launch {
            runCatching {
                repository.addCase(draft)
            }.onSuccess {
                messageState.value = "Case added."
            }.onFailure {
                messageState.value = it.message ?: "Could not add case."
            }
        }
    }

    fun clearUserMessage() {
        messageState.value = null
    }
}

class CasesViewModelFactory(
    private val repository: InvestigationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CasesViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return modelClass.cast(CasesViewModel(repository))
    }
}
