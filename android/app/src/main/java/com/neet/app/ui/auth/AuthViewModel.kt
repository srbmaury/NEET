package com.neet.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neet.app.data.AuthRepository
import com.neet.app.data.AuthResult
import com.neet.app.data.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signup(email: String, password: String, onSuccess: () -> Unit) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val result = authRepository.signup(email, password)) {
                is AuthResult.Success -> {
                    // A signup on a device with existing local history shouldn't strand it —
                    // push it up right away. Best-effort: a hiccup here doesn't block login
                    // success, "Sync Now" on the Progress tab covers a retry.
                    syncRepository.push()
                    _uiState.value = AuthUiState.Idle
                    onSuccess()
                }
                is AuthResult.Failure -> _uiState.value = AuthUiState.Error(result.message)
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val result = authRepository.login(email, password)) {
                is AuthResult.Success -> {
                    // Pull first on login — merges server data into local Room via the existing
                    // non-destructive id-based insert logic, same as a manual restore.
                    syncRepository.pull()
                    _uiState.value = AuthUiState.Idle
                    onSuccess()
                }
                is AuthResult.Failure -> _uiState.value = AuthUiState.Error(result.message)
            }
        }
    }
}

class AuthViewModelFactory(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AuthViewModel(authRepository, syncRepository) as T
    }
}
