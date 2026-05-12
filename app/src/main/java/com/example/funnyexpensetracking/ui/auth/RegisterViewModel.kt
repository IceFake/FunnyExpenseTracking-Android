package com.example.funnyexpensetracking.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.funnyexpensetracking.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RegisterUiState {
    data object Idle : RegisterUiState
    data object Loading : RegisterUiState
    data object Success : RegisterUiState
    data class Error(val message: String) : RegisterUiState
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun register(email: String, nickname: String, password: String) {
        when {
            email.isBlank() -> _uiState.value = RegisterUiState.Error("请输入邮箱")
            password.length < 6 -> _uiState.value = RegisterUiState.Error("密码至少 6 位")
            nickname.trim().length < 2 -> _uiState.value = RegisterUiState.Error("昵称至少 2 个字")
            else -> viewModelScope.launch {
                _uiState.value = RegisterUiState.Loading
                authRepository.register(email.trim(), password, nickname.trim())
                    .onSuccess { _uiState.value = RegisterUiState.Success }
                    .onFailure { e ->
                        _uiState.value = RegisterUiState.Error(e.message ?: "注册失败")
                    }
            }
        }
    }

    fun resetState() {
        _uiState.value = RegisterUiState.Idle
    }
}
