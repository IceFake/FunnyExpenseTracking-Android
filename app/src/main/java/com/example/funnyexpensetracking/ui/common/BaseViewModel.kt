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
 * 错误状态接口，所有UI状态都应实现此接口以支持错误处理
 */
interface ErrorState {
    val errorMessage: String?
}

/**
 * 基础ViewModel，提供通用的状态管理和事件处理
 */
abstract class BaseViewModel<S, E : UiEvent> : ViewModel() where S : UiState, S : ErrorState {

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

    /**
     * 设置错误信息并更新状态
     */
    protected fun setErrorMessage(message: String?) {
        updateState { copyWithError(message) as S }
    }

    /**
     * 清除错误信息
     */
    protected fun clearError() {
        setErrorMessage(null)
    }

    /**
     * 安全执行异步操作，自动处理错误
     * @param block 要执行的挂起函数
     * @param showMessage 是否发送显示消息的事件
     * @param onSuccess 成功时的回调（可选）
     * @param onError 错误时的自定义处理（可选）
     */
    protected suspend fun <T> safeExecute(
        block: suspend () -> T,
        showMessage: Boolean = true,
        onSuccess: (suspend (T) -> Unit)? = null,
        onError: (suspend (Throwable) -> Unit)? = null
    ): T? {
        return try {
            val result = block()
            clearError()
            onSuccess?.invoke(result)
            result
        } catch (e: Exception) {
            setErrorMessage(e.message)
            if (showMessage) {
                sendErrorMessage(e.message ?: "操作失败")
            }
            onError?.invoke(e)
            null
        }
    }

    /**
     * 发送错误消息事件（需子类实现具体事件类型）
     * 默认实现使用反射创建ShowMessage事件，建议子类重写此方法
     */
    protected open suspend fun sendErrorMessage(message: String) {
        // 尝试通过反射创建ShowMessage事件
        // 如果事件类有ShowMessage伴生对象或工厂方法，可以在这里实现
        // 默认不发送任何事件，子类应重写此方法
    }
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

/**
 * 扩展函数：为所有实现ErrorState的data class提供copyWithError方法
 * 注意：此扩展函数要求data class有errorMessage属性且为var
 */
@Suppress("UNCHECKED_CAST")
fun <T : ErrorState> T.copyWithError(errorMessage: String?): T {
    return try {
        val copyMethod = this::class.members.find { it.name == "copy" }
        if (copyMethod != null) {
            val errorParam = copyMethod.parameters.find { it.name == "errorMessage" }
            if (errorParam != null) {
                copyMethod.callBy(mapOf(errorParam to errorMessage)) as T
            } else {
                this
            }
        } else {
            this
        }
    } catch (e: Exception) {
        this
    }
}


