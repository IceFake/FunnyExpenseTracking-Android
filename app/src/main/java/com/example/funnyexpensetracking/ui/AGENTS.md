# PROJECT KNOWLEDGE BASE - UI Layer (表现层)

**Generated:** 2026-03-02  
**Commit:** 834c76a8  
**Branch:** main

## OVERVIEW
UI层采用MVI (Model-View-Intent) 模式，基于Clean Architecture的表现层实现。每个功能模块包含Contract（状态/事件定义）、ViewModel（状态管理）、Fragment（视图渲染）三件套。

## STRUCTURE
```
ui/
├── common/                    # 通用组件
│   └── BaseViewModel.kt      # 基础ViewModel，MVI核心实现
├── transaction/              # 记账模块 (示例)
│   ├── TransactionContract.kt # 状态和事件定义
│   ├── TransactionViewModel.kt # ViewModel实现
│   ├── TransactionFragment.kt # Fragment实现
│   ├── DailyTransactionAdapter.kt # 适配器
│   └── AddTransactionBottomSheet.kt # 底部弹窗
├── calendar/                 # 日历模块
├── fixedincome/              # 固定收支模块
├── investment/               # 投资理财模块
├── statistics/               # 统计模块
├── aianalysis/               # AI分析模块
├── financialquery/           # 财务问答模块
├── history/                  # 历史账单模块
├── usercenter/              # 用户中心模块
└── asset/                   # 资产管理模块
```

## MVI PATTERN
### 核心接口
```kotlin
// 标记接口
interface UiState
interface UiEvent
interface ErrorState {
    val errorMessage: String?
}

// 加载状态
enum class LoadingState { IDLE, LOADING, SUCCESS, ERROR }
```

### BaseViewModel 架构
```kotlin
abstract class BaseViewModel<S : UiState & ErrorState, E : UiEvent> : ViewModel() {
    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<S> = _uiState.asStateFlow()
    
    private val _uiEvent = MutableSharedFlow<E>()
    val uiEvent = _uiEvent.asSharedFlow()
    
    abstract fun initialState(): S
    protected fun updateState(reducer: S.() -> S)
    protected fun sendEvent(event: E)
    protected fun setErrorMessage(message: String?)
    protected suspend fun <T> safeExecute(...)
}
```

### Contract 模式
每个模块必须包含 `[Module]Contract.kt` 文件，定义：
1. **UiState**: 实现 `UiState` 和 `ErrorState` 接口的 data class
2. **UiEvent**: 继承 `UiEvent` 的密封类
3. **辅助数据类**: 如表单状态等

**示例 (TransactionContract.kt):**
```kotlin
data class TransactionUiState(
    val dailyTransactions: List<DailyTransactions> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val loadingState: LoadingState = LoadingState.IDLE,
    override val errorMessage: String? = null,
    // ... 其他状态字段
) : UiState, ErrorState

sealed class TransactionUiEvent : UiEvent {
    data class ShowMessage(val message: String) : TransactionUiEvent()
    object TransactionAdded : TransactionUiEvent()
    // ... 其他事件
}
```

## CREATING NEW MODULE
### 步骤1: 创建Contract文件
```kotlin
// ui/[module]/[Module]Contract.kt
package com.example.funnyexpensetracking.ui.[module]

import com.example.funnyexpensetracking.ui.common.*

data class [Module]UiState(
    // 状态字段
    override val errorMessage: String? = null
) : UiState, ErrorState

sealed class [Module]UiEvent : UiEvent {
    // 事件定义
}
```

### 步骤2: 创建ViewModel
```kotlin
// ui/[module]/[Module]ViewModel.kt
package com.example.funnyexpensetracking.ui.[module]

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class [Module]ViewModel @Inject constructor(
    // 依赖注入
) : BaseViewModel<[Module]UiState, [Module]UiEvent>() {
    
    override fun initialState() = [Module]UiState()
    
    init {
        loadData()
    }
    
    // 业务方法
    fun someAction() {
        viewModelScope.launch {
            safeExecute {
                // 业务逻辑
            }
        }
    }
}
```

### 步骤3: 创建Fragment
```kotlin
// ui/[module]/[Module]Fragment.kt
package com.example.funnyexpensetracking.ui.[module]

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class [Module]Fragment : Fragment() {
    
    private val viewModel: [Module]ViewModel by viewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeState()
        observeEvents()
    }
    
    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    // 更新UI
                }
            }
        }
    }
    
    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    // 处理事件
                }
            }
        }
    }
}
```

## CONVENTIONS
### 状态管理
- **单一状态源**: 所有UI状态集中在 `UiState` data class 中
- **不可变状态**: 通过 `copy()` 方法更新状态，确保不可变性
- **错误处理**: 所有 `UiState` 必须实现 `ErrorState` 接口
- **加载状态**: 使用 `LoadingState` 枚举管理异步操作状态

### 事件处理
- **用户交互**: Fragment捕获用户操作，调用ViewModel方法
- **单向数据流**: Fragment → ViewModel → State更新 → Fragment重新渲染
- **单次事件**: 使用 `SharedFlow` 发送单次事件（如导航、Toast）

### 依赖注入
- **ViewModel**: 使用 `@HiltViewModel` 注解，构造函数添加 `@Inject`
- **Fragment**: 使用 `@AndroidEntryPoint` 注解
- **View绑定**: 使用 `by viewModels()` 委托获取ViewModel实例

### 生命周期
- **状态观察**: 使用 `repeatOnLifecycle(Lifecycle.State.STARTED)` 避免后台状态更新
- **资源清理**: `onDestroyView()` 中清理 `_binding` 引用
- **事件收集**: 在 `onViewCreated()` 中开始观察状态和事件

## ERROR HANDLING
### BaseViewModel 错误处理
```kotlin
protected suspend fun <T> safeExecute(
    block: suspend () -> T,
    showMessage: Boolean = true,
    onSuccess: (suspend (T) -> Unit)? = null,
    onError: (suspend (Throwable) -> Unit)? = null
): T?
```
- 自动捕获异常并设置错误信息
- 可选的错误消息显示和自定义错误处理

### 错误状态更新
```kotlin
protected fun setErrorMessage(message: String?) {
    updateState { copyWithError(message) as S }
}
```
- 使用反射实现的 `copyWithError()` 扩展函数
- **注意**: 这依赖于data class有 `errorMessage` 参数

## ADAPTER PATTERNS
### 列表适配器
- 每个列表模块创建对应的 `[Module]Adapter.kt`
- 使用 `ListAdapter` 或 `RecyclerView.Adapter` 实现差异更新
- 通过回调将用户交互传递给ViewModel

### 底部弹窗
- 复杂表单使用 `BottomSheetDialogFragment`
- 简单对话框使用 `MaterialAlertDialogBuilder`
- 弹窗状态通过ViewModel管理，确保状态一致性

## NAVIGATION
### 模块间导航
```kotlin
// 使用FragmentManager进行导航
parentFragmentManager.beginTransaction()
    .replace(R.id.fragmentContainer, DestinationFragment())
    .addToBackStack(null)
    .commit()
```

### 事件驱动导航
```kotlin
// ViewModel中发送导航事件
sendEvent(TransactionUiEvent.NavigateToDetail(transactionId))

// Fragment中处理导航事件
when (event) {
    is TransactionUiEvent.NavigateToDetail -> {
        // 执行导航
    }
}
```

## ANTI-PATTERNS (THIS PROJECT)
1. **职责过载**: `TransactionViewModel` 同时处理交易、账户、固定收支（应拆分）
2. **反射依赖**: `copyWithError()` 使用反射调用data class的copy方法（潜在运行时问题）
3. **状态爆炸**: 某些 `UiState` 包含过多字段（考虑拆分或嵌套状态）

## BEST PRACTICES
### 状态设计
- 保持 `UiState` 扁平化，避免深度嵌套
- 相关字段分组到嵌套data class中
- 计算属性使用 `val` 而不是 `var`

### 事件设计
- 使用密封类定义明确的事件层次
- 导航事件包含必要参数（如ID、类型）
- 成功/失败事件包含用户友好的消息

### 性能优化
- 使用 `collectLatest` 收集状态，避免旧值处理
- 复杂UI更新使用 `diffUtil` 或 `ListAdapter`
- 图片加载使用 `Glide` 或 `Coil`（如需要）

## VERIFICATION CHECKLIST
创建新模块后验证：
- [ ] Contract文件包含完整的 `UiState` 和 `UiEvent` 定义
- [ ] ViewModel正确扩展 `BaseViewModel<S, E>`
- [ ] Fragment使用 `@AndroidEntryPoint` 和 `by viewModels()`
- [ ] 状态观察使用 `repeatOnLifecycle(Lifecycle.State.STARTED)`
- [ ] 错误处理通过 `safeExecute()` 或手动 `try-catch`
- [ ] 所有用户交互调用ViewModel方法，不直接执行业务逻辑

---

*基于深度分析生成，用于AI代理工作指导*