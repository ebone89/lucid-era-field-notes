package com.lucidera.investigations.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lucidera.investigations.data.InvestigationRepository
import com.lucidera.investigations.data.local.entity.InvestigationCaseEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val activeCases: Int = 0,
    val openLeads: Int = 0,
    val verifiedLeads: Int = 0,
    val entitiesTracked: Int = 0,
    val recentCases: List<InvestigationCaseEntity> = emptyList()
)

class DashboardViewModel(repository: InvestigationRepository) : ViewModel() {
    val uiState = combine(
        repository.allCases,
        repository.openLeadCount,
        repository.verifiedLeadCount,
        repository.entityCount
    ) { cases, openLeads, verifiedLeads, entityCount ->
            DashboardUiState(
                activeCases = cases.count(),
                openLeads = openLeads,
                verifiedLeads = verifiedLeads,
                entitiesTracked = entityCount,
                recentCases = cases.take(5)
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}

class DashboardViewModelFactory(
    private val repository: InvestigationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DashboardViewModel(repository) as T
    }
}
