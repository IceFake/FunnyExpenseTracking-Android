package com.example.funnyexpensetracking.ui.common
}
    ERROR
    SUCCESS,
    LOADING,
    IDLE,
enum class LoadingState {
 */
 * 通用加载状态
/**

interface UiEvent
 */
 * UI事件标记接口
/**

interface UiState
 */
 * UI状态标记接口
/**

}
    protected fun currentState(): S = _uiState.value
     */
     * 获取当前状态
    /**

    }
        }
            _uiEvent.emit(event)
        viewModelScope.launch {
    protected fun sendEvent(event: E) {
     */
     * 发送事件
    /**

    }
        _uiState.value = _uiState.value.reducer()
    protected fun updateState(reducer: S.() -> S) {
     */
     * 更新状态
    /**

    abstract fun initialState(): S
     */
     * 初始状态
    /**

    val uiEvent = _uiEvent.asSharedFlow()
    private val _uiEvent = MutableSharedFlow<E>()

    val uiState: StateFlow<S> = _uiState.asStateFlow()
    private val _uiState = MutableStateFlow(initialState())

abstract class BaseViewModel<S : UiState, E : UiEvent> : ViewModel() {
 */
 * 基础ViewModel，提供通用的状态管理和事件处理
/**

import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel


