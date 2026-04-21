package com.lucidera.investigations.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val LOCK_TIMEOUT_MS = 5 * 60 * 1000L

class AppLockViewModel : ViewModel() {
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var backgroundedAt = 0L

    fun unlock() {
        _isUnlocked.value = true
        backgroundedAt = 0L
    }

    fun onBackground() {
        if (_isUnlocked.value) {
            backgroundedAt = System.currentTimeMillis()
        }
    }

    fun onForeground() {
        if (_isUnlocked.value && backgroundedAt > 0L) {
            if (System.currentTimeMillis() - backgroundedAt > LOCK_TIMEOUT_MS) {
                _isUnlocked.value = false
            }
            backgroundedAt = 0L
        }
    }
}
