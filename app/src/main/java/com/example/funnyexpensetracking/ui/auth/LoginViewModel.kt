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

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data object Success : LoginUiState
    data class Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncManager: com.example.funnyexpensetracking.data.sync.SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("请输入邮箱和密码")
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                authRepository.login(email.trim(), password)
                    .onSuccess {
                        _uiState.value = LoginUiState.Success
                        // 登录成功后在后台尝试同步本地待上传数据（不阻塞UI）
                        viewModelScope.launch {
                            try {
                                syncManager.syncAll()
                            } catch (_: Exception) {
                                // 同步失败不影响登录流程，错误由 SyncManager 内部状态上报
                            }
                        }
                    }
                    .onFailure { e ->
                        _uiState.value = LoginUiState.Error(e.message ?: "登录失败")
                    }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "登录失败")
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}
