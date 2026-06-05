身份设定
你是专注于中文学术论文优化的资深编辑，主要职责是将机械化的 AI 生成文本调整为符合期刊规范的学术表述，在保持原文研究内容、技术逻辑与论证链条完整的前提下，让行文更自然，也更便于阅读。

操作准则
1. 不改变原文的研究假设、实验数据与核心结论等关键信息；
2. 避免使用不符合目标期刊领域的口语化或非专业表达；
3. 不引入与原文无关的额外观点或数据；
4. 不弱化也不过度扩展原有的逻辑论证关系；
5. 不使用夸张或生僻的学术词汇；
6. 减少机械化连接词，让段落衔接更顺一些，但仍保持论文语气；
7. 控制括号使用频率，优先采用逗号、句号和从句结构组织信息。

改写执行步骤
1. 机械内容改写：针对 AI 生成痕迹进行逐项修订，使文本更接近期刊常见表述；
2. 整体逻辑优化：结合章节结构调整段落顺序与句间衔接方式，让论述更顺畅；
3. 专业术语校验：核对改写后的术语使用是否与原文保持一致，确保技术表述准确。

代码核心片段截取规则
1. 对于整个类的截取，非关键方法可使用 `/* logic omitted for brevity */` 进行省略；
2. 对于核心逻辑代码的截取，需完整呈现，并在关键分支和数据流位置附带注释说明。

5.1 Android 客户端功能模块实现

5.1.1 登录模块实现

本模块主要实现用户身份验证与会话建立。系统在移动端采用离线优先与渐进同步的实现思路，先完成输入合法性校验，再发起远端认证请求，随后依据返回结果完成本地会话保存及后续同步处理。这样的设计既能保持较快的交互响应，也便于在登录后尽快恢复用户的本地状态。

从架构层面看，登录功能位于 UI 层与数据层之间，由 ViewModel 负责输入校验、状态维护与事件分发，Repository 负责网络调用及本地持久化，环境切换则由统一配置类管理，相关实现可参见 `app/src/main/java/com/example/funnyexpensetracking/config/ApiEnvironmentConfig.kt`。

在 ViewModel 层中，`LoginViewModel` 负责输入校验、状态切换以及登录成功后的同步触发，核心代码如上所示。Repository 层中，`AuthRepository` 定义认证契约，`AuthRepositoryImpl` 负责向后端发起登录请求并保存会话信息，完成令牌写入、用户信息持久化和退出登录后的会话清理。登录流程由此形成“校验、认证、持久化”的完整链路。

```kotlin
/**
 * 登录界面状态密封接口，定义四种互斥的界面状态。
 * Idle: 初始空闲状态；Loading: 认证请求进行中；
 * Success: 登录成功，即将触发数据同步；Error: 认证失败并携带提示信息。
 */
sealed interface LoginUiState {
    data object Idle : LoginUiState            // 表单初始状态，等待用户输入
    data object Loading : LoginUiState         // 认证请求已发出，界面展示加载指示器
    data object Success : LoginUiState         // 认证成功，即将跳转主界面并触发同步
    data class Error(val message: String) : LoginUiState  // 认证失败，携带本地化错误信息
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncManager: com.example.funnyexpensetracking.data.sync.SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * 执行登录认证流程。
     * 输入校验 → Loading 状态 → 远端认证 → Success/Error 状态。
     * 成功时触发全量同步以恢复用户数据，失败时传递异常消息至 UI 层。
     * @param email    用户邮箱，内部自动 trim
     * @param password 明文密码
     */
    fun login(email: String, password: String) {
        // 空输入提前拦截，避免无效网络请求
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("请输入邮箱和密码")
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading            // 进入加载态
            authRepository.login(email.trim(), password)
                .onSuccess {
                    _uiState.value = LoginUiState.Success     // 认证通过
                    viewModelScope.launch { syncManager.syncAll() }  // 立即同步离线数据
                }
                .onFailure { e -> _uiState.value = LoginUiState.Error(e.message ?: "登录失败") }
        }
    }
}
```

```kotlin
/**
 * 认证仓库契约接口，定义登录、注册与登出操作。
 * 实现类负责向后端发起 HTTP 请求并管理本地会话持久化。
 */
interface AuthRepository {
    /** 用户登录：发送凭证至后端，成功返回 Unit，失败携带异常信息 */
    suspend fun login(email: String, password: String): Result<Unit>
    /** 用户注册：创建新账户并返回认证结果 */
    suspend fun register(email: String, password: String, nickname: String): Result<Unit>
    /** 登出操作：通知服务端并清除本地会话状态 */
    suspend fun logout()
}
```

```kotlin
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val userPreferencesManager: UserPreferencesManager
) : AuthRepository {

    /**
     * 执行远端登录请求并持久化会话信息。
     * 核心数据流：构造请求 → 发出 HTTP 调用 → 校验响应（HTTP 状态码 + 业务码 + 令牌非空）
     * → 依次写入 authToken / refreshToken / userId / userEmail → 返回 Result。
     * @param email    用户邮箱（内部 trim 处理）
     * @param password 明文密码
     * @return Result<Unit> 成功时携带 Unit，失败时携带服务端或网络异常
     */
    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val response = authApiService.login(
                LoginRequestDto(
                    email = email.trim(),
                    password = password,
                    deviceId = userPreferencesManager.getOrCreateDeviceId()  // 设备标识用于多端管理
                )
            )
            val body = response.body()
            // 三重校验：HTTP 成功 + 业务码 200 + 令牌非空
            if (response.isSuccessful && body?.code == 200 && !body.data?.token.isNullOrBlank()) {
                val data = requireNotNull(body.data)
                // ↓ 持久化认证令牌链，供后续请求鉴权使用
                userPreferencesManager.saveAuthToken(data.token)
                if (!data.refreshToken.isNullOrBlank()) userPreferencesManager.saveRefreshToken(data.refreshToken)
                data.user?.id?.let { userPreferencesManager.saveBackendUserId(it.toString()) }
                data.user?.email?.let { userPreferencesManager.saveBackendUserEmail(it) }
                Result.success(Unit)
            } else {
                // 业务层错误：提取服务端消息或 HTTP 状态描述
                Result.failure(Exception(body?.message ?: response.message() ?: "登录失败"))
            }
        } catch (e: Exception) {
            // 网络异常或 JSON 序列化失败
            Result.failure(Exception(e.message ?: "登录失败", e))
        }
    }

    /**
     * 执行登出操作。
     * 先尝试以 refreshToken 通知服务端注销会话；无论服务端是否响应，
     * 均在 finally 块中清除本地所有会话数据，确保客户端状态一致性。
     */
    override suspend fun logout() {
        try {
            val refreshToken = userPreferencesManager.getRefreshToken()
            if (refreshToken.isNotBlank()) {
                authApiService.logout(LogoutRequestDto(refreshToken = refreshToken))  // 通知服务端
            }
        } finally {
            // 无论网络请求是否成功，强制清除本地会话，防止残留令牌
            userPreferencesManager.clearBackendSession()
        }
    }
}
```

上述实现表明，登录模块通过输入校验、异步认证与本地会话保存的分层处理，既保证了界面交互的及时性，也让状态传播的边界更加清楚。

5.1.2 日常记账模块实现

日常记账模块主要实现收支记录的创建、修改与删除，并向界面提供聚合后的流水数据用于统计与展示。该模块采用离线优先策略，所有写操作优先落库并标记为待同步，待网络可用时再由同步管理器统一处理。UI 层通过 `TransactionViewModel` 暴露以日期或账户为粒度的 Flow，从而保持界面数据的实时更新。

从架构上看，交易数据遵循领域模型与本地实体映射的约定，DAO 负责流式查询，RepositoryImpl 承担本地优先读写与同步标记，UseCase 层则封装分类汇总、预算预警等业务规则。ViewModel 将交易、账户与分类等数据源组合为 UIState，并通过事件流完成一次性消息分发。

在 ViewModel 层中，`TransactionViewModel` 负责交易加载、同步状态监听和用户操作入口，核心代码如上所示。Repository 层中，`TransactionRepositoryImpl` 负责本地优先的增删改查与待同步标记，核心流程是先完成本地写入，再根据网络状态决定是否触发后台同步。

```kotlin
// 说明：该 ViewModel 负责加载当天交易、监听同步状态以及处理新增、删除等入口，核心流程围绕本地数据加载与同步触发展开。
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionUseCases: TransactionUseCases,
    private val syncManager: SyncManager,
    private val networkMonitor: NetworkMonitor,
    private val realtimeAssetCalculator: RealtimeAssetCalculator,
    private val userPreferencesManager: UserPreferencesManager
) : BaseViewModel<TransactionUiState, TransactionUiEvent>() {
    override fun initialState() = TransactionUiState()

    init {
        loadData()              // 加载当日交易与账户数据
        initDefaultAccounts()   // 若账户表为空则初始化默认账户
        observeSyncState()      // 监听同步状态与待同步数量
        observeRealtimeAsset()  // 订阅实时资产流，驱动界面分钟级刷新
    }

    /**
     * 添加一笔交易记录。
     * 数据流：调用 UseCase 写入（内部标记待同步）→ 关闭对话框 → 发送成功事件
     * → 根据网络状态决定是否立即触发全量同步。
     * @param amount    交易金额
     * @param type      收支类型（收入/支出）
     * @param category  分类标签
     * @param accountId 所属账户 ID
     * @param note      备注（可选）
     * @param date      交易时间戳，默认为当前时刻
     */
    fun addTransaction(
        amount: Double,
        type: TransactionType,
        category: String,
        accountId: Long,
        note: String = "",
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            transactionUseCases.addTransaction(
                amount = amount,
                type = type,
                category = category,
                accountId = accountId,
                note = note,
                date = date
            )
            hideAddDialog()
            sendEvent(TransactionUiEvent.TransactionAdded)
            // 根据网络状态提供差异化提示
            sendEvent(TransactionUiEvent.ShowMessage(if (networkMonitor.isNetworkAvailable()) "记账成功" else "记账成功（离线模式，稍后自动同步）"))
            if (networkMonitor.isNetworkAvailable()) syncManager.syncAll()  // 在线时立即同步
        }
    }

    /**
     * 删除指定交易记录，成功后触发同步。
     * @param transaction 待删除的交易领域对象
     */
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionUseCases.deleteTransaction(transaction)
            sendEvent(TransactionUiEvent.TransactionDeleted)
            sendEvent(TransactionUiEvent.ShowMessage("删除成功"))
            if (networkMonitor.isNetworkAvailable()) syncManager.syncAll()
        }
    }

    /**
     * 订阅 RealtimeAssetCalculator 输出的实时资产流，
     * 将 currentAsset / incomePerMinute / expensePerMinute / netChangePerMinute 映射到 UIState。
     */
    private fun observeRealtimeAsset() {
        realtimeAssetCalculator.realtimeAsset.onEach { assetData ->
            updateState {
                copy(
                    realtimeAsset = assetData.currentAsset,
                    incomePerMinute = assetData.incomePerMinute,
                    expensePerMinute = assetData.expensePerMinute,
                    netChangePerMinute = assetData.netChangePerMinute
                )
            }
        }.launchIn(viewModelScope)
    }

    /**
     * 监听 SyncManager 的同步状态与待同步计数。
     * 同步成功时向用户展示已同步条数，失败时展示错误信息。
     */
    private fun observeSyncState() {
        syncManager.syncState.onEach { state ->
            updateState { copy(isSyncing = state is SyncState.Syncing, isOffline = !networkMonitor.isNetworkAvailable()) }
            when (state) {
                is SyncState.Success -> if (state.syncedCount > 0) sendEvent(TransactionUiEvent.ShowMessage("同步完成，共同步 ${state.syncedCount} 条记录"))
                is SyncState.Error -> sendEvent(TransactionUiEvent.ShowMessage("同步失败: ${state.message}"))
                else -> {}
            }
        }.launchIn(viewModelScope)
        syncManager.pendingSyncCount.onEach { count -> updateState { copy(pendingSyncCount = count) } }.launchIn(viewModelScope)
    }

    /**
     * 若本地账户表为空，则创建预设默认账户（如现金、银行卡等），
     * 保证首次使用时的记账体验。
     */
    private fun initDefaultAccounts() {
        viewModelScope.launch { transactionUseCases.initializeDefaultAccountsIfEmpty() }
    }

    /**
     * 加载当日交易数据，取今日起止时间戳，通过 UseCase 层获取聚合结果后填充 UIState。
     * 数据源通过 Flow 持续推送，界面随本地数据变更自动刷新。
     */
    private fun loadData() {
        updateState { copy(loadingState = LoadingState.LOADING) }
        val todayStart = DateTimeUtil.getTodayStartTimestamp()
        val todayEnd = DateTimeUtil.getTodayEndTimestamp()
        transactionUseCases.loadTransactionsByDateRange(todayStart, todayEnd).onEach { result ->
            updateState {
                copy(
                    transactions = result.transactions,
                    dailyTransactions = result.dailyTransactions,
                    accounts = result.accounts,
                    todayIncome = result.todayIncome,
                    todayExpense = result.todayExpense,
                    totalBalance = result.totalBalance,
                    loadingState = LoadingState.SUCCESS
                )
            }
        }.launchIn(viewModelScope)
    }
}
```

```kotlin
// 说明：该仓库实现负责将交易记录写入本地数据库并在网络可用时触发同步，逻辑重点是本地优先与同步状态维护。
@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val expenseApiService: ExpenseApiService,
    private val networkMonitor: NetworkMonitor,
    private val syncManager: SyncManager
) : TransactionRepository {
    // 非核心查询方法省略，核心写入与同步流程完整展示
    /* logic omitted for brevity */

    /**
     * 新增交易记录：领域对象转换为实体并标记 PENDING_UPLOAD，先写入本地保证离线可用，
     * 随后在网络可用时触发后台同步。
     * @param transaction 交易领域对象
     * @return 本地生成的记录 ID
     */
    override suspend fun addTransaction(transaction: Transaction): Long {
        val entity = toEntity(transaction).copy(
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAt = System.currentTimeMillis()
        )
        val localId = transactionDao.insert(entity)
        trySync()
        return localId
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        val existingEntity = transactionDao.getById(transaction.id)
        val entity = toEntity(transaction).copy(
            serverId = existingEntity?.serverId,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAt = System.currentTimeMillis()
        )
        transactionDao.update(entity)
        trySync()
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        val entity = transactionDao.getById(transaction.id)
        if (entity != null) {
            if (entity.serverId != null) {
                transactionDao.markAsDeleted(transaction.id)
                trySync()
            } else {
                transactionDao.delete(entity)
            }
        }
    }

    /* logic omitted for brevity */

    /**
     * 网络可用时触发交易同步，作为各写入方法的后置钩子，
     * 减少待同步数据积压并降低跨端冲突概率。
     */
    private suspend fun trySync() {
        if (networkMonitor.isNetworkAvailable()) {
            syncManager.syncTransactions()
        }
    }

    /* logic omitted for brevity */
}
```

上述实现体现了本地优先的数据写入策略，也说明了通过观察流式数据来更新界面的常见做法。

5.1.3 固定收支模块实现

固定收支模块旨在管理周期性收入与支出，并支撑“实时资产引擎”的分钟级动态演进机制。在此场景中，任何长周期的固定收支（如“每日收支”、“每月工资”）都需要折算为其在每分钟内的细粒度累计额，以便实时平滑地反映在总资产变化上。

**长线演进与浮点数累加误差挑战**
基于频率计算每分钟收支时，常规方案会将“周期总金额”除以“周期总分钟数”（如 1000元 / 1440分钟）。然而，由此衍生出的无限循环小数若是通过“系统轮询定时累加”的方式长线运行，将不可避免地引发 IEEE 754 浮点数截断与精度丢失。随着时间推移，该类微弱误差的不断正向叠加会导致资产账本产生明显错位。

**差值补偿与周期剥离法（优化策略）**
为了从根本上规避该问题，系统在架构设计层面摒弃了粗暴的“金额累加法”，转而采用**基于时间标度的整除补偿器**策略：
1. **时间标尺唯一化**：系统在定时轮询中仅累加无精度损失的“整型分钟增量”，不对单步金额进行处理。
2. **完整周期剥离**：通过对总累计时长进行整除操作，剥离出“完整的经历周期数（Complete Cycles）”，这部分累计金额直接用 `完整周期数 × 设定金额` 计算，实现“零小数、零妥协”的精准叠加。
3. **零头按比例折算与自动清零**：仅当剔除全部完整周期后，剩下的“零头分钟数（Remaining Minutes）”才会被用作比例折算。由于剩余零头永远小于单个周期，这意味着无论计算运行多少天、多少年，其浮度误差被严格封印在不超过“1个周期”内的微小范围内，并随着新一轮周期的形成而被定期、自动地清零。

该核心思路分别体现在领域模型内的高内聚金额换算函数，以及外界系统的调用映射上。以下为适用于论述展示的核心代码截取：

```kotlin
// 截取自 domain/model/FixedIncome.kt -> FixedIncomeFrequency
/**
 * 根据累计生效的分钟数（长整型）安全计算累计金额
 * 核心在于【周期剥离】，解决长线浮点数累积产生的精度溢出或丢失
 */
fun calculateAccumulatedAmount(amount: Double, accumulatedMinutes: Long): Double {
    val minutesPerCycle = getMinutesPerCycle() 
    
    // 1. 剥离出已跨越的完整“足额”周期，直接进行乘法放大避免循环小数
    val completeCycles = accumulatedMinutes / minutesPerCycle 
    // 2. 取模运算获取当前尚未走完的“零头分钟数”
    val remainingMinutes = accumulatedMinutes % minutesPerCycle 

    // 核心组装：绝对精准的(完整周期额度) + 同比折算的(当期零头额度)
    return (completeCycles * amount) + (remainingMinutes.toDouble() / minutesPerCycle * amount)
}
```

```kotlin
// 截取自 domain/usecase/RealtimeAssetCalculator.kt
/**
 * 为指定固定收支条目计算自上次记录以来的累计
 * 设计要点：引擎循环中剥离金额敏感操作，仅作时间整型演进，交由底层映射最终金额
 */
private suspend fun updateFixedIncomeAccumulation(
    fixedIncome: FixedIncome,
    currentTime: Long
) {
    // 逻辑抽象：计算距上次运算经过了多少有效分钟(effectiveMinutes)...
    
    // Step1: 各次轮询仅累加无误差的【时间整型常量】
    val newAccumulatedMinutes = fixedIncome.accumulatedMinutes + effectiveMinutes

    // Step2: 将总时间常量投入上文的【周期剥离补偿公式】，直接还原为精确的总资产额
    val newAccumulatedAmount = fixedIncome.frequency.calculateAccumulatedAmount(
        fixedIncome.amount,
        newAccumulatedMinutes
    )

    // Step3: 持久化覆写，替换上一次的历史状态
    assetRepository.updateFixedIncomeAccumulation(
        id = fixedIncome.id,
        accumulatedMinutes = newAccumulatedMinutes,
        accumulatedAmount = newAccumulatedAmount, // 避免了增量叠加
        lastRecordTime = currentTime
    )
}
```

上述实现展示了在移动端高频、长周期的财务金融计算背景下，如何通过对时间维度与货币维度的合理切分重构，以算子融合的方式彻底化解微观计息带来的累加误差隐患漏洞。该模块设计高度符合“单一事实来源（Single Source of Truth）”原则，具有极高的工业参考价值与容错性。



5.1.4 投资管理模块实现

投资管理模块主要实现投资条目的记录、组合汇总、外部行情拉取与周期性市值刷新。架构上，投资数据通过 `InvestmentRepository` 暴露给 ViewModel，Repository 负责缓存本地数据，并在需要时向后端或第三方行情接口发起请求。为降低界面等待时间，该模块采用异步刷新策略，先展示本地缓存的市值，再在后台完成行情更新与结果回写。

从实现层面看，需要关注行情请求的限流与去重、不同网络条件下的降级处理，以及价格更新与资产快照之间的一致性。下面的两段代码分别展示 `InvestmentViewModel` 的条目合并与刷新触发逻辑，以及 `InvestmentRepositoryImpl` 的行情拉取与本地更新过程。

```kotlin
// 说明：该 ViewModel 负责加载投资数据、合并同类条目，并周期性触发股票价格刷新，整体逻辑围绕本地展示和异步行情更新展开。
@HiltViewModel
class InvestmentViewModel @Inject constructor(
    private val investmentDao: InvestmentDao,
    private val investmentRepository: InvestmentRepository
) : BaseViewModel<InvestmentUiState, InvestmentUiEvent>() {

    private var stockPriceRefreshJob: Job? = null

    override fun initialState() = InvestmentUiState()

    init {
        loadInvestments()           // 加载本地投资数据
        startStockPriceRefresh()    // 启动周期性行情刷新
    }

    /**
     * 从 DAO 加载全部投资条目并进行聚合。
     * 流程：DAO Flow → 实体映射为领域对象 → 同品类合并 → 计算总投入/总市值/总盈亏 → 写入 UIState。
     * 合并逻辑由 mergeInvestments() 完成，将同一品类下相同描述的条目聚合为单一投资项，
     * 累加其数量、投入成本与当前市值，避免界面出现重复条目。
     */
    private fun loadInvestments() {
        updateState { copy(loadingState = LoadingState.LOADING) }

        investmentDao.getAllInvestments()
            .onEach { entities ->
                val investments = entities.map { toDomainModel(it) }

                val mergedInvestments = mergeInvestments(investments)

                val totalInvestment = mergedInvestments.sumOf { it.investment }
                val totalCurrentValue = mergedInvestments.sumOf { it.calcCurrentValue() }
                val totalProfitLoss = totalCurrentValue - totalInvestment

                updateState {
                    copy(
                        allInvestments = mergedInvestments,
                        filteredInvestments = applyFilter(mergedInvestments, filterType),
                        totalInvestment = totalInvestment,
                        totalCurrentValue = totalCurrentValue,
                        totalProfitLoss = totalProfitLoss,
                        loadingState = LoadingState.SUCCESS
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 按品类与描述将同名投资条目合并。
     * 分组键为 "category_description"，单条目直接返回，多条目累加数量、投入成本与当前市值后返回合并副本。
     * 该方法确保同一投资品种（如同一股票多次买入）在界面中展示为一行，降低用户认知负担。
     * @param investments 原始投资列表
     * @return 合并后的投资列表
     */
    private fun mergeInvestments(investments: List<Investment>): List<Investment> {
        return investments
            .groupBy { "${it.category}_${it.description}" }  // 按品类+描述作为合并键
            .map { (_, items) ->
                if (items.size == 1) {
                    items.first()
                } else {
                    val first = items.first()
                    first.copy(
                        quantity = items.sumOf { it.quantity },
                        investment = items.sumOf { it.investment },
                        currentValue = items.sumOf { it.currentValue }
                    )
                }
            }
    }

    /* UI filtering, CRUD, dialog control, and entity mapping methods omitted for brevity */

    /**
     * 启动周期性股票价格刷新 Job。
     * 初始化时立即执行一次以获取最新行情，随后每 60 秒循环调用 Repository 的批量刷新接口。
     * 后台静默刷新结果（成功/失败）不向用户反馈，仅更新数据库中的 currentPrice 字段，
     * 由 DAO Flow 自动驱动界面更新。ViewModel 销毁时通过 onCleared() 取消协程防止泄漏。
     */
    private fun startStockPriceRefresh() {
        stockPriceRefreshJob?.cancel()
        stockPriceRefreshJob = viewModelScope.launch {
            refreshStockPricesInternal()

            while (isActive) {
                delay(60_000)
                refreshStockPricesInternal()
            }
        }
    }

    /**
     * 用户手动触发股票价格刷新入口。
     * 设置 isRefreshing 状态以展示加载指示器，调用 Repository 批量刷新接口，
     * 完成后向用户反馈成功或失败消息并恢复界面状态。
     */
    fun refreshStockPrices() {
        viewModelScope.launch {
            updateState { copy(isRefreshing = true) }
            when (val result = investmentRepository.refreshAllStockPrices()) {
                is Resource.Success -> sendEvent(InvestmentUiEvent.ShowMessage("刷新成功"))
                is Resource.Error -> sendEvent(InvestmentUiEvent.ShowMessage("刷新失败: ${result.message}"))
                is Resource.Loading -> {}
            }
            updateState { copy(isRefreshing = false) }
        }
    }

    /* lifecycle cleanup and internal helpers omitted for brevity */
}
```

```kotlin
// 说明：该仓库负责查询股票代码、调用行情接口并把最新价格写回数据库，逻辑重点是先查本地持仓，再批量刷新价格。
@Singleton
class InvestmentRepositoryImpl @Inject constructor(
    private val investmentDao: InvestmentDao,
    private val stockApiService: StockApiService
) : InvestmentRepository {

    companion object {
        private const val TAG = "InvestmentRepository"
    }

    override suspend fun refreshAllStockPrices(): Resource<Unit> {
        return try {
            val stockCodes = investmentDao.getAllStockCodes()
            Log.d(TAG, "获取到的股票代码: $stockCodes")

            if (stockCodes.isEmpty()) {
                Log.d(TAG, "没有股票需要刷新价格")
                return Resource.Success(Unit)
            }

            val sinaSymbols = stockCodes.map { convertToSinaSymbol(it) }
            val symbolsParam = sinaSymbols.joinToString(",")
            Log.d(TAG, "请求后端股票行情 API, symbols: $symbolsParam")

            val response = stockApiService.getBatchQuotes(BatchQuoteRequest(sinaSymbols))
            Log.d(TAG, "API响应码: ${response.code()}, 是否成功: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val quotes = response.body()!!.data?.quotes.orEmpty()
                Log.d(TAG, "解析到 ${quotes.size} 个股票价格")

                if (quotes.isEmpty()) {
                    Log.w(TAG, "没有获取到有效的股票价格数据")
                    return Resource.Error("未找到股票数据，请检查股票代码格式")
                }

                quotes.forEach { result ->
                    Log.d(TAG, "更新股票 ${result.symbol} 价格: ${result.currentPrice ?: 0.0}")
                    val originalCode = findOriginalCode(stockCodes, result.symbol)
                    if (originalCode != null) {
                        investmentDao.updateStockPrice(originalCode, result.currentPrice ?: 0.0)
                    }
                }

                Resource.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "API请求失败: ${response.code()}, 错误信息: $errorBody")
                Resource.Error("获取股票价格失败: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "刷新股票价格异常", e)
            Resource.Error("网络错误: ${e.message}")
        }
    }

    /**
     * 将本地股票代码转换为行情接口所需的 Symbol 格式。
     * 规则：
     *   6 位数字且首位为 6 → sh 前缀（沪市）；
     *   6 位数字且首位为 0 或 3 → sz 前缀（深市）；
     *   5 位数字 → hk 前缀（港股）；
     *   已含 SH/SZ/HK/GB_ 前缀 → 转小写保留；
     *   其他 → gb_ 前缀（美股等）。
     * @param code 本地存储的股票代码
     * @return 行情接口格式的 Symbol 字符串
     */
    private fun convertToSinaSymbol(code: String): String {
        val upperCode = code.uppercase().trim()

        return when {
            upperCode.startsWith("SH") || upperCode.startsWith("SZ") -> upperCode.lowercase()
            upperCode.startsWith("HK") -> upperCode.lowercase()
            upperCode.startsWith("GB_") -> upperCode.lowercase()
            upperCode.matches(Regex("^6\\d{5}$")) -> "sh$upperCode"
            upperCode.matches(Regex("^[03]\\d{5}$")) -> "sz$upperCode"
            upperCode.matches(Regex("^\\d{5}$")) -> "hk$upperCode"
            else -> "gb_${upperCode.lowercase()}"
        }
    }

    /* findOriginalCode and other helper methods omitted for brevity */
}
```

该模块体现了本地优先展示与异步刷新的实现思路，使投资数据能够在不阻塞界面的前提下保持较高的时效性。

5.1.5 数据管理模块实现

数据管理模块主要实现本地持久化结构定义、DAO 接口设计、Repository 实现以及跨端数据同步管理。实现策略以离线优先为核心，所有实体均包含 `serverId`、`syncStatus` 与 `lastSyncAt` 等字段，以便在网络恢复后执行增量同步。Schema 演化采用 Room 的迁移机制，复杂迁移脚本需在构建阶段或迁移工具中提前验证。

在实现层面，DAO 保持细粒度查询方法并返回 Flow，与 ViewModel 和 UseCase 层自然衔接。RepositoryImpl 遵循本地优先、网络回补的策略，`SyncManager` 则负责消费待同步队列并与后端保持幂等交互。下面的两段代码分别展示 `TransactionDao` 的查询与同步状态接口，以及 `SyncManager` 的交易同步入口。

```kotlin
// 说明：该 DAO 提供交易查询、增删改和同步状态维护接口，整体逻辑是把本地数据与同步队列分开管理。
@Dao
interface TransactionDao {
    /**
     * 按日期范围查询交易记录，以 Flow 形式返回并按日期降序排列，
     * 供界面层订阅以获取实时更新的流水数据。
     */
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    /**
     * 查询所有待同步交易（默认状态：PENDING_UPLOAD 和 PENDING_DELETE），
     * 由 SyncManager 批量消费。
     */
    @Query("SELECT * FROM transactions WHERE syncStatus IN (:statuses)")
    suspend fun getPendingSyncTransactions(statuses: List<SyncStatus> = listOf(SyncStatus.PENDING_UPLOAD, SyncStatus.PENDING_DELETE)): List<TransactionEntity>

    /**
     * 更新指定交易的同步状态与最后同步时间，
     * 通常在成功同步或冲突解决后调用。
     */
    @Query("UPDATE transactions SET syncStatus = :status, lastSyncAt = :syncTime WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus, syncTime: Long = System.currentTimeMillis())
}
```

```kotlin
// 说明：该同步管理器负责统一处理交易与账户的上传、删除和冲突解决，核心逻辑是先检查网络与会话，再批量同步本地待处理数据。
@Singleton
class SyncManager @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val expenseApiService: ExpenseApiService,
    private val accountApiService: AccountApiService,
    private val networkMonitor: NetworkMonitor,
    private val userPreferencesManager: UserPreferencesManager
) {
    /** 同步状态流，ViewModel 层订阅以展示同步进度 */
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /** 待同步条目计数流，供界面展示离线积压数量 */
    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    companion object {
        const val TABLE_TRANSACTIONS = "transactions"
        const val TABLE_ACCOUNTS = "accounts"
    }

    init {
        observeNetworkAndSync()     // 监听网络恢复，自动触发同步
        updatePendingSyncCount()    // 初始化待同步计数
    }

    /**
     * 监听网络状态变化。当网络恢复且存在待同步条目且用户已登录时，自动触发全量同步，
     * 实现离线操作后网络恢复时的透明数据上传。
     */
    private fun observeNetworkAndSync() {
        scope.launch {
            networkMonitor.observeNetworkStatus()
                .filter { it == NetworkStatus.AVAILABLE }  // 仅在网络可用时触发
                .collect {
                    if (_pendingSyncCount.value > 0 && userPreferencesManager.hasBackendSession()) {
                        syncAll()
                    }
                }
        }
    }

    /**
     * 重新计算待同步条目总数（交易 + 账户），更新 _pendingSyncCount。
     * 在各同步操作完成后调用以反映最新积压状态。
     */
    private fun updatePendingSyncCount() {
        scope.launch {
            val transactionCount = transactionDao.getPendingSyncCount()
            val accountCount = accountDao.getPendingSyncCount()
            _pendingSyncCount.value = transactionCount + accountCount
        }
    }

    /**
     * 全量同步入口。
     * 前置校验：检查用户登录状态与网络可用性；
     * 执行流程：依次同步交易与账户，累加成功计数，最后更新待同步计数与同步状态。
     * @return SyncResult<Int> 包含成功同步的总条目数或错误信息
     */
    fun syncAll(): SyncResult<Int> {
        if (!userPreferencesManager.hasBackendSession()) {
            return SyncResult.Error("未登录，数据已保存在本地，登录后将自动同步")
        }

        if (!networkMonitor.isNetworkAvailable()) {
            return SyncResult.Error("网络不可用，数据已保存在本地")
        }

        _syncState.value = SyncState.Syncing
        var totalSynced = 0

        return try {
            // 先同步交易，再同步账户
            val transactionResult = syncTransactions()
            if (transactionResult is SyncResult.Success) {
                totalSynced += transactionResult.data
            }

            val accountResult = syncAccounts()
            if (accountResult is SyncResult.Success) {
                totalSynced += accountResult.data
            }

            _syncState.value = SyncState.Success(totalSynced)
            updatePendingSyncCount()
            SyncResult.Success(totalSynced)
        } catch (e: Exception) {
            val errorMessage = "同步失败: ${e.message}"
            _syncState.value = SyncState.Error(errorMessage, e)
            SyncResult.Error(errorMessage, e)
        }
    }

    /**
     * 同步交易记录：将本地 PENDING_UPLOAD 条目批量上传，处理 PENDING_DELETE 条目，
     * 并应对 HTTP 409 冲突。
     * 流程：查询待同步列表 → 分离上传/删除 → 上传 → 处理响应（200/409/其他） → 删除本地条目。
     * @return SyncResult<Int> 成功时携带已同步条目数
     */
    fun syncTransactions(): SyncResult<Int> {
        if (!networkMonitor.isNetworkAvailable()) {
            return SyncResult.Error("网络不可用")
        }

        return try {
            val pendingTransactions = transactionDao.getPendingSyncTransactions()
            if (pendingTransactions.isEmpty()) {
                return SyncResult.Success(0)
            }

            // 按同步状态分流
            val toUpload = pendingTransactions.filter { it.syncStatus == SyncStatus.PENDING_UPLOAD }
            val toDelete = pendingTransactions.filter { it.syncStatus == SyncStatus.PENDING_DELETE }

            var syncedCount = 0

            // ── 上传待同步条目 ──
            if (toUpload.isNotEmpty()) {
                val dtos = toUpload.map { it.toSyncDto() }
                val lastSyncTime = syncMetadataDao.getByTableName(TABLE_TRANSACTIONS)?.lastSyncTimestamp ?: 0
                val request = SyncRequest(dtos, lastSyncTime)

                val response = expenseApiService.syncTransactions(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.code == 200) {
                        val serverTransactions = body.data.orEmpty()

                        // 将服务端返回的 ID 回写本地，建立映射
                        for (entity in toUpload) {
                            val match = findServerTransactionForLocal(entity, serverTransactions)
                            val serverId = match?.id?.takeIf { it > 0 }
                            if (serverId != null) {
                                transactionDao.updateServerId(entity.id, serverId.toString(), SyncStatus.SYNCED)
                            } else {
                                transactionDao.updateSyncStatus(entity.id, SyncStatus.SYNCED)
                            }
                        }
                        syncedCount += toUpload.size
                    } else {
                        return SyncResult.Error("同步交易失败: ${body?.message}")
                    }
                } else if (response.code() == 409) {
                    // HTTP 409 表示冲突，触发冲突解决流程
                    return handleTransactionConflict(toUpload)
                } else {
                    return SyncResult.Error("服务器返回错误: ${response.code()}")
                }
            }

            // ── 处理待删除条目 ──
            if (toDelete.isNotEmpty()) {
                for (entity in toDelete) {
                    entity.serverId?.toLongOrNull()?.let { serverId ->
                        val response = expenseApiService.deleteTransaction(serverId)
                        if (response.isSuccessful) {
                            transactionDao.delete(entity)
                            syncedCount++
                        }
                    } ?: run {
                        // 无 serverId 则直接删除（本地未上云数据）
                        transactionDao.delete(entity)
                        syncedCount++
                    }
                }
            }

            updateSyncMetadata(TABLE_TRANSACTIONS)
            SyncResult.Success(syncedCount)
        } catch (e: Exception) {
            updateSyncError(TABLE_TRANSACTIONS, e.message)
            SyncResult.Error("同步交易记录失败: ${e.message}", e)
        }
    }

    /**
     * 处理交易同步冲突（HTTP 409）。
     * 拉取服务端最新数据，以 updatedAt 时间戳判断冲突方向：
     * 若服务端版本更新或本地处于 CONFLICT 状态，则用服务端数据覆盖本地；
     * 完成后将本地条目标记为 SYNCED。
     * @param toUpload 触发冲突的本地待上传条目
     * @return SyncResult<Int> 携带成功解决的条目数
     */
    private fun handleTransactionConflict(toUpload: List<TransactionEntity>): SyncResult<Int> {
        return try {
            val now = System.currentTimeMillis()
            val response = expenseApiService.getTransactions(0, now)
            if (response.isSuccessful && response.body()?.code == 200) {
                val serverTransactions = response.body()?.data ?: emptyList()
                for (serverTx in serverTransactions) {
                    serverTx.id?.let { serverId ->
                        val localEntity = transactionDao.getByServerId(serverId.toString())
                        if (localEntity != null) {
                            // 冲突解决策略：以服务端时间戳为准，更新本地版本
                            if (serverTx.updatedAt != null &&
                                (localEntity.updatedAt < serverTx.updatedAt || localEntity.syncStatus == SyncStatus.CONFLICT)
                            ) {
                                transactionDao.update(serverTx.toEntity().copy(id = localEntity.id))
                            }
                        }
                    }
                }
                for (entity in toUpload) {
                    transactionDao.updateSyncStatus(entity.id, SyncStatus.SYNCED)
                }
                updateSyncMetadata(TABLE_TRANSACTIONS)
                SyncResult.Success(toUpload.size)
            } else {
                SyncResult.Error("冲突处理失败")
            }
        } catch (e: Exception) {
            SyncResult.Error("冲突处理失败: ${e.message}")
        }
    }

    /**
     * 同步账户数据。逻辑与 syncTransactions 对称：
     * 上传 PENDING_UPLOAD 条目 → 处理 409 冲突 → 删除 PENDING_DELETE 条目。
     * @return SyncResult<Int> 成功时携带已同步条目数
     */
    fun syncAccounts(): SyncResult<Int> {
        if (!networkMonitor.isNetworkAvailable()) {
            return SyncResult.Error("网络不可用")
        }

        return try {
            val pendingAccounts = accountDao.getPendingSyncAccounts()
            if (pendingAccounts.isEmpty()) {
                return SyncResult.Success(0)
            }

            val toUpload = pendingAccounts.filter { it.syncStatus == SyncStatus.PENDING_UPLOAD }
            val toDelete = pendingAccounts.filter { it.syncStatus == SyncStatus.PENDING_DELETE }

            var syncedCount = 0

            if (toUpload.isNotEmpty()) {
                val dtos = toUpload.map { it.toSyncDto() }
                val lastSyncTime = syncMetadataDao.getByTableName(TABLE_ACCOUNTS)?.lastSyncTimestamp ?: 0
                val request = AccountSyncRequest(dtos, lastSyncTime)

                val response = accountApiService.syncAccounts(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.code == 200) {
                        val serverAccounts = body.data ?: emptyList()

                        for (entity in toUpload) {
                            val match = findServerAccountForLocal(entity, serverAccounts)
                            val serverId = match?.id?.takeIf { it > 0 }
                            if (serverId != null) {
                                accountDao.updateServerId(entity.id, serverId.toString(), SyncStatus.SYNCED)
                            } else {
                                accountDao.updateSyncStatus(entity.id, SyncStatus.SYNCED)
                            }
                        }
                        syncedCount += toUpload.size
                    } else {
                        return SyncResult.Error("同步账户失败: ${body?.message}")
                    }
                } else if (response.code() == 409) {
                    return handleAccountConflict(toUpload)
                } else {
                    return SyncResult.Error("服务器返回错误: ${response.code()}")
                }
            }

            if (toDelete.isNotEmpty()) {
                for (entity in toDelete) {
                    entity.serverId?.toLongOrNull()?.let { serverId ->
                        val response = accountApiService.deleteAccount(serverId)
                        if (response.isSuccessful) {
                            accountDao.delete(entity)
                            syncedCount++
                        }
                    } ?: run {
                        accountDao.delete(entity)
                        syncedCount++
                    }
                }
            }

            updateSyncMetadata(TABLE_ACCOUNTS)
            SyncResult.Success(syncedCount)
        } catch (e: Exception) {
            updateSyncError(TABLE_ACCOUNTS, e.message)
            SyncResult.Error("同步账户数据失败: ${e.message}", e)
        }
    }

    /**
     * 处理账户同步冲突。
     * 与服务端版本比较 updatedAt，若服务端版本更新则覆盖本地数据。
     * @param toUpload 触发冲突的本地待上传账户列表
     * @return SyncResult<Int> 携带成功解决的条目数
     */
    private fun handleAccountConflict(toUpload: List<AccountEntity>): SyncResult<Int> {
        return try {
            val response = accountApiService.getAccounts()
            if (response.isSuccessful && response.body()?.code == 200) {
                val serverAccounts = response.body()?.data ?: emptyList()
                for (serverAcct in serverAccounts) {
                    serverAcct.id?.let { serverId ->
                        val localEntity = accountDao.getByServerId(serverId.toString())
                        if (localEntity != null) {
                            if (serverAcct.updatedAt != null && localEntity.updatedAt < serverAcct.updatedAt) {
                                accountDao.update(serverAcct.toEntity().copy(id = localEntity.id))
                            }
                        }
                    }
                }
                for (entity in toUpload) {
                    accountDao.updateSyncStatus(entity.id, SyncStatus.SYNCED)
                }
                updateSyncMetadata(TABLE_ACCOUNTS)
                SyncResult.Success(toUpload.size)
            } else {
                SyncResult.Error("账户冲突处理失败")
            }
        } catch (e: Exception) {
            SyncResult.Error("账户冲突处理失败: ${e.message}")
        }
    }

    /**
     * 从服务端拉取指定时间范围内的交易记录并合并到本地。
     * 对于已存在于本地的记录（以 serverId 匹配），若非本地待上传状态则更新；
     * 未匹配到的记录直接插入本地数据库。
     * @param startDate 查询起始时间戳
     * @param endDate   查询结束时间戳
     * @return SyncResult<List<TransactionEntity>> 携带拉取的实体列表
     */
    fun pullTransactionsFromServer(startDate: Long, endDate: Long): SyncResult<List<TransactionEntity>> {
        if (!networkMonitor.isNetworkAvailable()) {
            return SyncResult.Error("网络不可用")
        }

        return try {
            val response = expenseApiService.getTransactions(startDate, endDate)
            if (response.isSuccessful && response.body()?.code == 200) {
                val serverTransactions = response.body()?.data ?: emptyList()
                val entities = serverTransactions.map { it.toEntity() }

                for (entity in entities) {
                    val sid = entity.serverId
                    if (sid != null) {
                        val localEntity = transactionDao.getByServerId(sid)
                        if (localEntity != null) {
                            // 保护本地待上传数据不被覆盖
                            if (localEntity.syncStatus != SyncStatus.PENDING_UPLOAD) {
                                transactionDao.update(entity.copy(id = localEntity.id))
                            }
                        } else {
                            transactionDao.insert(entity)
                        }
                    }
                }

                SyncResult.Success(entities)
            } else {
                SyncResult.Error("服务器返回错误: ${response.body()?.message}")
            }
        } catch (e: Exception) {
            SyncResult.Error("拉取数据失败: ${e.message}", e)
        }
    }

    /** 更新指定表的同步元数据时间戳，记录最后一次成功同步时间 */
    private fun updateSyncMetadata(tableName: String) { /* omitted in doc excerpt */ }
    /** 记录指定表同步过程中的错误信息 */
    private fun updateSyncError(tableName: String, error: String?) { /* omitted in doc excerpt */ }

    /** 是否存在待同步数据 */
    fun needsSync(): Boolean {
        return _pendingSyncCount.value > 0
    }

    /** 手动刷新待同步计数 */
    fun refreshPendingCount() {
        updatePendingSyncCount()
    }

    /**
     * 在服务端返回列表中匹配本地交易实体。
     * 匹配策略：选取 createdAt 时间差最小的服务端记录。
     */
    private fun findServerTransactionForLocal(local: TransactionEntity, serverList: List<TransactionDto>): TransactionDto? { return serverList.minByOrNull { kotlin.math.abs((it.createdAt ?: it.date) - local.createdAt) }
    }

    /**
     * 在服务端返回列表中匹配本地账户实体，
     * 以 createdAt 最小绝对差作为匹配依据。
     */
    private fun findServerAccountForLocal(local: AccountEntity, serverList: List<AccountDto>): AccountDto? { return serverList.minByOrNull { kotlin.math.abs((it.createdAt ?: 0L) - local.createdAt) }
    }

    /** TransactionEntity → TransactionDto 映射，用于上传同步 */
    private fun toSyncDto(entity: TransactionEntity): TransactionDto { return TransactionDto(id = entity.serverId?.toLongOrNull() ?: 0L, amount = entity.amount, type = entity.type.name, category = entity.category, note = entity.note, date = entity.date, accountId = entity.accountId.takeIf { it > 0 }, createdAt = entity.createdAt, updatedAt = entity.updatedAt, deletedAt = null) }

    /** TransactionDto → TransactionEntity 映射，用于服务端数据拉取后的本地转换 */
    private fun toEntity(dto: TransactionDto): TransactionEntity { return TransactionEntity(serverId = dto.id?.takeIf { it > 0 }?.toString(), amount = dto.amount, type = try { TransactionType.valueOf(dto.type) } catch (_: Exception) { TransactionType.EXPENSE }, category = dto.category, accountId = dto.accountId ?: 0, note = dto.note, date = dto.date, createdAt = dto.createdAt ?: dto.date, updatedAt = dto.updatedAt ?: System.currentTimeMillis(), syncStatus = SyncStatus.SYNCED, lastSyncAt = System.currentTimeMillis()) }

    /** AccountEntity → AccountDto 映射 */
    private fun toSyncDto(entity: AccountEntity): AccountDto { return AccountDto(id = entity.serverId?.toLongOrNull() ?: 0L, name = entity.name, icon = entity.icon, balance = entity.balance, isDefault = entity.isDefault, sortOrder = entity.sortOrder, createdAt = entity.createdAt, updatedAt = entity.updatedAt) }

    /** AccountDto → AccountEntity 映射 */
    private fun toEntity(dto: AccountDto): AccountEntity { return AccountEntity(serverId = dto.id?.takeIf { it > 0 }?.toString(), name = dto.name, icon = dto.icon, balance = dto.balance, isDefault = dto.isDefault, sortOrder = dto.sortOrder, createdAt = dto.createdAt ?: System.currentTimeMillis(), updatedAt = dto.updatedAt ?: System.currentTimeMillis(), syncStatus = SyncStatus.SYNCED, lastSyncAt = System.currentTimeMillis()) }
}
```

