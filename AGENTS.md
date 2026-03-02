# PROJECT KNOWLEDGE BASE - FunnyExpenseTracking Android

**Generated:** 2026-03-02
**Commit:** 834c76a8
**Branch:** main

## OVERVIEW
现代化Android记账应用，采用MVVM + Clean Architecture架构，支持多账户管理、固定收支、投资理财、AI分析和实时资产计算。

## STRUCTURE
```
com.example.funnyexpensetracking/
├── data/                 # 数据层
│   ├── local/           # Room本地存储 (dao/entity)
│   ├── remote/          # Retrofit远程API (api/dto)
│   └── repository/      # Repository实现 (@Singleton)
├── domain/              # 领域层
│   ├── model/          # 领域模型 (data class)
│   ├── repository/     # Repository接口
│   └── usecase/        # 业务用例 (按功能分组)
├── ui/                  # 表现层
│   ├── transaction/     # 交易模块 (MVI Contract+VM+Fragment)
│   ├── calendar/        # 日历模块
│   ├── investment/      # 投资模块
│   ├── fixedincome/    # 固定收支模块
│   ├── statistics/     # 统计模块
│   ├── aianalysis/     # AI分析模块
│   ├── financialquery/ # 财务问答模块
│   ├── history/        # 历史账单模块
│   └── usercenter/     # 用户中心模块
├── di/                  # Hilt依赖注入模块
├── util/               # 工具类 (Resource, NetworkMonitor等)
└── worker/             # WorkManager后台任务
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| **添加新功能** | `ui/[module]/` | 需要Contract+ViewModel+Fragment三件套 |
| **数据模型** | `domain/model/` + `data/local/entity/` | 领域模型 + 数据库实体映射 |
| **Repository接口** | `domain/repository/` | 定义数据操作契约 |
| **Repository实现** | `data/repository/` | 实现离线优先策略 |
| **业务逻辑** | `domain/usecase/[module]/` | 用例按功能分组 |
| **工具类** | `util/` | Resource、NetworkMonitor等跨层工具 |

## CODE MAP
| 关键类 | 类型 | 位置 | 角色 |
|--------|------|------|------|
| `BaseViewModel` | 基类 | `ui/common/` | MVI状态管理基础 |
| `Resource` | 工具类 | `util/` | 网络结果封装 (Success/Error/Loading) |
| `SyncManager` | 管理器 | `data/sync/` | 数据同步状态管理 |
| `RealtimeAssetCalculator` | 用例 | `domain/usecase/` | 实时资产计算器 |
| `FunnyExpenseApp` | Application | 根包 | Hilt入口 + WorkManager配置 |
| `MainActivity` | Activity | 根包 | 单Activity架构，底部导航 |

## CONVENTIONS
### 数据层约定
- **Entity**: `@Entity(tableName)` + `data class` + `syncStatus`字段
- **DAO**: `@Dao`接口 + `suspend`方法 + `Flow`返回类型
- **Repository**: `@Singleton`类，实现离线优先策略
- **同步**: 所有实体包含`serverId`、`syncStatus`、`lastSyncAt`

### 领域层约定  
- **模型**: 纯`data class`，无Android依赖
- **用例**: `@Singleton`类，包含业务逻辑函数
- **Repository接口**: 定义数据操作，返回`Flow`或`suspend`函数

### UI层约定
- **MVI模式**: Contract定义`UiState`/`UiEvent`，ViewModel扩展`BaseViewModel`
- **状态管理**: 使用`StateFlow`(状态) + `SharedFlow`(事件)
- **错误处理**: UI状态实现`ErrorState`接口，包含`errorMessage`字段

### 工具类约定
- **Resource**: 所有Repository使用`Resource<T>`封装结果
- **扩展函数**: 使用`.toDomainModel()`、`.toEntity()`进行层间转换

## ANTI-PATTERNS (THIS PROJECT)
- **硬编码依赖版本**: 部分依赖未在`libs.versions.toml`中定义
- **重复CI逻辑**: `.github/workflows/release.yml`中有重复的Release创建步骤
- **职责过载**: `TransactionViewModel`同时处理交易、账户、固定收支
- **反射依赖**: `BaseViewModel.copyWithError`使用反射调用data class的copy方法
- **硬编码密码**: 签名配置包含默认密码`funnyexpense123`
- **未使用工具类**: `CurrencyUtil.kt`定义但未使用

## UNIQUE STYLES
- **中文注释**: 主要文档语言为中文，KDoc使用中文
- **离线优先**: 所有数据操作先本地后网络，包含同步状态追踪
- **实时资产计算**: 分钟级资产变动计算，多数据源监听
- **AI集成**: DeepSeek API消费分析 + 自然语言财务问答
- **固定收支算法**: 基于累计时间，支持多频率计算

## COMMANDS
```bash
# 构建
./gradlew clean build
./gradlew assembleDebug
./gradlew assembleRelease

# 测试
./gradlew test                          # 单元测试
./gradlew connectedAndroidTest          # 仪器测试
./gradlew test --tests "*ClassName"     # 单类测试

# 代码质量
./gradlew lint                          # Lint检查
./gradlew lintDebug                     # Debug变体Lint

# 安装
./gradlew installDebug                  # 安装Debug版本
```

## NOTES
### 架构要点
- **Hilt注入**: ViewModel使用`@HiltViewModel`，Fragment使用`@AndroidEntryPoint`
- **状态管理**: ViewModel使用`updateState`方法，Fragment通过`observe`监听
- **后台任务**: WorkManager每15分钟执行资产快照和股价同步
- **网络监控**: `NetworkMonitor`监听网络状态，Repository据此决策

### 重构建议
1. **拆分TransactionViewModel** → Transaction/Account/FixedIncome三个ViewModel
2. **改进BaseViewModel.copyWithError** → 使用类型安全替代反射
3. **统一依赖版本** → 所有依赖移至`libs.versions.toml`
4. **优化DataManagementRepositoryImpl** → 拆分导入逻辑

### 跨层工具
- **Resource.kt**: 最核心跨层工具类，17个文件使用
- **NetworkMonitor.kt**: 5个文件使用，跨data/ui两层

---

*基于深度分析生成，用于AI代理工作指导*