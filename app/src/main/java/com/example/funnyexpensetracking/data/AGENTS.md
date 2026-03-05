# DATA LAYER KNOWLEDGE BASE

**Generated:** 2026-03-02
**Location:** `com.example.funnyexpensetracking.data`

## OVERVIEW
数据层实现，采用离线优先策略，集成Room本地存储与Retrofit远程API。

## STRUCTURE
```
data/
├── local/           # 本地数据库
│   ├── dao/        # 数据访问对象 (@Dao接口)
│   ├── entity/     # 数据库实体 (@Entity)
│   └── sync/       # 同步管理
├── remote/         # 远程API
│   ├── api/        # Retrofit接口
│   └── dto/        # 数据传输对象
└── repository/     # Repository实现 (@Singleton)
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| **新增数据库表** | `local/entity/` + `local/dao/` | 需要Entity + DAO + 类型转换器 |
| **新增API接口** | `remote/api/` + `remote/dto/` | 需要Service接口 + DTO类 |
| **新增Repository** | `repository/` | 实现`domain/repository/`接口 |
| **数据同步** | `sync/SyncManager.kt` | 同步状态管理和网络检查 |

## CONVENTIONS

### Entity约定
```kotlin
@Entity(tableName = "table_name")
data class EntityName(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serverId: Long? = null,           // 云端ID（同步用，Long类型）
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncAt: Long? = null,          // 上次同步时间
    // 业务字段...
)
```

### DAO约定
```kotlin
@Dao
interface EntityNameDao {
    @Query("SELECT * FROM table_name")
    fun getAll(): Flow<List<EntityName>>
    
    @Insert
    suspend fun insert(entity: EntityName): Long
    
    @Update
    suspend fun update(entity: EntityName)
    
    @Query("DELETE FROM table_name WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    // 同步相关查询
    @Query("SELECT * FROM table_name WHERE syncStatus = :status")
    suspend fun getBySyncStatus(status: SyncStatus): List<EntityName>
}
```

### Repository实现约定
```kotlin
@Singleton
class EntityNameRepositoryImpl @Inject constructor(
    private val dao: EntityNameDao,
    private val apiService: EntityNameApiService,
    private val networkMonitor: NetworkMonitor,
    private val syncManager: SyncManager
) : EntityNameRepository {
    
    override fun getAll(): Flow<List<DomainModel>> {
        return dao.getAll().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun add(item: DomainModel): Long {
        val entity = item.toEntity().copy(
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAt = System.currentTimeMillis()
        )
        val localId = dao.insert(entity)
        
        // 尝试立即同步
        if (networkMonitor.isOnline.value) {
            trySync()
        }
        
        return localId
    }
}
```

### DTO约定
```kotlin
data class EntityNameDto(
    @SerializedName("id") val id: Long,           // 或String，根据API设计
    @SerializedName("amount") val amount: Double,
    @SerializedName("created_at") val createdAt: Long // 或String，根据API设计
)
```

### 同步约定
- **SyncStatus枚举**: `SYNCED`, `PENDING_UPLOAD`, `PENDING_DELETE`, `CONFLICT` (定义在`data/local/entity/TransactionEntity.kt`)
- **离线优先**: 所有操作先本地，标记待同步，网络恢复后自动同步
- **冲突解决**: 基于`updatedAt`时间戳，最新版本优先

## ANTI-PATTERNS
- ❌ **硬编码版本依赖** → 使用`libs.versions.toml`统一管理
- ❌ **Entity包含业务逻辑** → 业务逻辑应在Repository或UseCase中
- ❌ **直接暴露Entity** → 使用`.toDomainModel()`转换为领域模型
- ❌ **忽略同步状态** → 所有Entity必须包含`syncStatus`字段
- ❌ **混合网络/数据库异常** → 分别处理SQLiteException和IOException

## LAYER CONVERSIONS
```kotlin
// Entity → DomainModel
fun EntityName.toDomainModel(): DomainModel = DomainModel(
    id = id,
    amount = amount,
    // 转换其他字段...
)

// DomainModel → Entity  
fun DomainModel.toEntity(): EntityName = EntityName(
    id = id,
    amount = amount,
    // 转换其他字段...
)

// DTO → DomainModel
fun EntityNameDto.toDomainModel(): DomainModel = DomainModel(
    id = id, // 如果DTO id为Long直接使用，如果为String需要转换：id.toLongOrNull() ?: 0
    amount = amount,
    // 转换其他字段...
)
```

## NOTES
- **资源管理**: 使用`Resource<T>`封装网络请求结果
- **网络检查**: 通过`NetworkMonitor`监听网络状态变化
- **并发安全**: Repository方法使用`suspend`确保线程安全
- **数据一致性**: 通过Room事务保证相关操作的原子性
- **类型转换**: 在`data/local/Converters.kt`中定义Room类型转换器

---

*数据层核心职责：数据持久化、网络通信、离线同步、Repository实现*