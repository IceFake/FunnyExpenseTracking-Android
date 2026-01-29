package com.example.funnyexpensetracking.ui.common
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 基础ViewModel，提供通用的状态管理和事件处理
 */
abstract class BaseViewModel<S : UiState, E : UiEvent> : ViewModel() {

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<E>()
    val uiEvent = _uiEvent.asSharedFlow()

    /**
     * 初始状态
     */
    abstract fun initialState(): S

    /**
     * 更新状态
     */
    protected fun updateState(reducer: S.() -> S) {
        _uiState.value = _uiState.value.reducer()
    }

    /**
     * 发送事件
     */
    protected fun sendEvent(event: E) {
        viewModelScope.launch {
            _uiEvent.emit(event)
        }
    }

    /**
     * 获取当前状态
     */
    protected fun currentState(): S = _uiState.value
}

/**
 * UI状态标记接口
 */
interface UiState

/**
 * UI事件标记接口
 */
interface UiEvent

/**
 * 通用加载状态
 */
enum class LoadingState {
    IDLE,
    LOADING,
    SUCCESS,
    ERROR
}


