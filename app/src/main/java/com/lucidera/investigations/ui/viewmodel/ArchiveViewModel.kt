package com.lucidera.investigations.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lucidera.investigations.data.InvestigationRepository
import com.lucidera.investigations.data.network.WaybackLookupResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArchiveUiState(
    val isLoading: Boolean = false,
    val result: WaybackLookupResult? = null,
    val errorMessage: String? = null
)

class ArchiveViewModel(
    private val repository: InvestigationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    fun lookupArchive(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, result = null, errorMessage = null) }
            runCatching {
                repository.lookupArchive(url)
            }.onSuccess { result ->
                _uiState.update { ArchiveUiState(result = result) }
            }.onFailure { error ->
                _uiState.update { ArchiveUiState(errorMessage = error.message ?: "Lookup failed.") }
            }
        }
    }
}

class ArchiveViewModelFactory(
    private val repository: InvestigationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ArchiveViewModel(repository) as T
    }
}
