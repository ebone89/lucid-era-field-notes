package com.lucidera.investigations.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lucidera.investigations.data.CaseDraft
import com.lucidera.investigations.data.InvestigationRepository
import com.lucidera.investigations.data.local.entity.InvestigationCaseEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CasesUiState(
    val cases: List<InvestigationCaseEntity> = emptyList()
)

class CasesViewModel(
    private val repository: InvestigationRepository
) : ViewModel() {
    val uiState = repository.allCases
        .map { CasesUiState(cases = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CasesUiState())

    fun addCase(draft: CaseDraft) {
        viewModelScope.launch {
            repository.addCase(draft)
        }
    }
}

class CasesViewModelFactory(
    private val repository: InvestigationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CasesViewModel(repository) as T
    }
}
