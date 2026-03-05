# DOMAIN LAYER KNOWLEDGE BASE

**Generated:** 2026-03-02
**Location:** `com.example.funnyexpensetracking.domain`

## OVERVIEW
领域层，包含纯业务模型、Repository接口和业务用例，无Android框架依赖。

## STRUCTURE
```
domain/
├── model/          # 领域模型 (纯Kotlin data class)
├── repository/     # Repository接口定义
└── usecase/       # 业务用例 (按功能分组)
    ├── transaction/
    ├── ai/
    ├── asset/
    ├── statistics/
    └── stock/
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| **新增领域模型** | `model/` | 纯data class，无框架注解 |
| **新增数据操作** | `repository/` | 定义Repository接口 |
| **新增业务逻辑** | `usecase/[module]/` | 按功能分组，包含私有函数 |
| **复杂计算** | `usecase/RealtimeAssetCalculator.kt` | 实时资产计算器 |

## CONVENTIONS

### 领域模型约定
```kotlin
/**
 * 交易记录领域模型
 * @property id 本地数据库ID
 * @property amount 金额
 * @property type 交易类型 (收入/支出)
 * @property category 分类
 * @property date 交易日期时间戳
 */
data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val accountId: Long,
    val note: String? = null,
    val date: Long = System.currentTimeMillis()
) {
    val isIncome: Boolean get() = type == TransactionType.INCOME
    val isExpense: Boolean get() = type == TransactionType.EXPENSE
}
```

### Repository接口约定
```kotlin
/**
 * 交易记录Repository接口
 * 定义数据操作契约，由data层实现
 */
interface TransactionRepository {
    /** 获取所有交易记录 */
    fun getAllTransactions(): Flow<List<Transaction>>
    
    /** 根据日期范围查询 */
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>
    
    /** 根据类型查询 */
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>
    
    /** 添加交易记录 */
    suspend fun addTransaction(transaction: Transaction): Long
    
    /** 更新交易记录 */
    suspend fun updateTransaction(transaction: Transaction)
    
    /** 删除交易记录 */
    suspend fun deleteTransaction(id: Long)
}
```

### 用例约定
```kotlin
@Singleton
class TransactionUseCases @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    
    /** 计算指定日期的总收支 */
    suspend fun calculateDailySummary(date: Long): DailySummary {
        val startOfDay = DateTimeUtil.startOfDay(date)
        val endOfDay = DateTimeUtil.endOfDay(date)
        
        val transactions = transactionRepository
            .getTransactionsByDateRange(startOfDay, endDate = endOfDay)
            .first()
        
        val totalIncome = transactions
            .filter { it.isIncome }
            .sumOf { it.amount }
        
        val totalExpense = transactions
            .filter { it.isExpense }
            .sumOf { it.amount }
        
        return DailySummary(
            date = date,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netChange = totalIncome - totalExpense
        )
    }
    
    /** 验证交易数据 */
    private fun validateTransaction(transaction: Transaction): ValidationResult {
        return when {
            transaction.amount <= 0 -> ValidationResult.Error("金额必须大于0")
            transaction.category.isBlank() -> ValidationResult.Error("请选择分类")
            transaction.accountId <= 0 -> ValidationResult.Error("请选择账户")
            else -> ValidationResult.Valid
        }
    }
}
```

### 枚举和密封类约定
```kotlin
/** 交易类型枚举 */
enum class TransactionType {
    INCOME, EXPENSE
}

/** 同步状态枚举 */
enum class SyncStatus {
    SYNCED,           // 已同步
    PENDING_UPLOAD,   // 待上传
    PENDING_DELETE,   // 待删除
    CONFLICT          // 冲突
}

/** 验证结果密封类 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
```

## ANTI-PATTERNS
- ❌ **引入Android依赖** → 保持纯Kotlin，无android.*导入
- ❌ **在模型中使用框架注解** → 如`@Entity`、`@SerializedName`属于data层
- ❌ **Repository接口包含实现逻辑** → 只定义契约，实现在data层
- ❌ **UseCase直接操作数据库** → 通过Repository接口访问数据
- ❌ **领域模型包含UI状态** → UI状态在ui层定义

## DESIGN PRINCIPLES

### 单一职责
- **模型**: 仅包含业务数据和验证逻辑
- **Repository接口**: 仅定义数据操作契约
- **UseCase**: 封装具体业务逻辑，可组合多个Repository

### 依赖倒置
- **高层模块** (UseCase) 不依赖 **低层模块** (Repository实现)
- 两者都依赖 **抽象** (Repository接口)

### 开闭原则
- 通过扩展(新UseCase)而非修改(现有UseCase)来添加功能
- Repository接口稳定，实现可替换

### 接口隔离
- 每个Repository接口专注于一个聚合根
- 避免庞大的通用Repository接口

## LAYER BOUNDARIES

### 与data层边界
```kotlin
// ✅ 正确：domain定义接口，data实现
// domain/repository/TransactionRepository.kt
interface TransactionRepository {
    suspend fun getById(id: Long): Transaction?
}

// data/repository/TransactionRepositoryImpl.kt  
@Singleton
class TransactionRepositoryImpl : TransactionRepository {
    override suspend fun getById(id: Long): Transaction? {
        return dao.getById(id)?.toDomainModel()
    }
}
```

### 与ui层边界
```kotlin
// ✅ 正确：ui层调用UseCase，获取领域模型
class TransactionViewModel @Inject constructor(
    private val transactionUseCases: TransactionUseCases
) : BaseViewModel<TransactionUiState, TransactionUiEvent>() {
    
    fun loadDailySummary(date: Long) {
        viewModelScope.launch {
            val summary = transactionUseCases.calculateDailySummary(date)
            updateState { copy(dailySummary = summary) }
        }
    }
}
```

## NOTES
- **纯Kotlin**: 可在JVM单元测试中独立测试
- **不可变性**: 所有模型为`data class`，属性为`val`
- **扩展函数**: 使用`.toDomainModel()`、`.toEntity()`进行层间转换
- **业务规则集中**: 所有业务逻辑集中在UseCase中
- **测试友好**: 易于Mock Repository接口进行单元测试

---

*领域层核心职责：业务模型定义、业务规则封装、数据访问契约*