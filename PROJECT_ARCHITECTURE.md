# FunnyExpenseTracking Android 项目架构文档

## 📋 项目概述

这是一个基于 **MVVM + Clean Architecture** 架构的 Android 个人收支管理（记账）应用。

### 主要功能
1. **记账模块** - 每日收支记录管理
2. **统计模块** - 月/年度统计图表（后端生成）
3. **AI分析模块** - 消费习惯智能分析（后端AI接口）
4. **资产管理模块** - 固定收支 + 实时资产计算
5. **股票模块** - 持仓管理 + 实时行情联动

### 技术栈
- **语言**: Kotlin
- **架构**: MVVM + Clean Architecture
- **依赖注入**: Hilt
- **数据库**: Room
- **网络请求**: Retrofit + OkHttp
- **异步处理**: Kotlin Coroutines + Flow
- **UI**: Material Design 3

---

## 📁 项目结构

```
app/src/main/
├── java/com/example/funnyexpensetracking/
│   ├── FunnyExpenseApp.kt          # Application类，Hilt入口
│   ├── MainActivity.kt             # 主Activity
│   ├── data/                       # 数据层
│   │   ├── local/                  # 本地数据源
│   │   │   ├── dao/               # Room DAO
│   │   │   ├── entity/            # Room Entity
│   │   │   └── AppDatabase.kt     # Room数据库
│   │   ├── remote/                 # 远程数据源
│   │   │   ├── api/               # Retrofit API接口
│   │   │   └── dto/               # 数据传输对象
│   │   └── repository/             # Repository实现
│   ├── domain/                     # 领域层
│   │   ├── model/                  # 领域模型
│   │   ├── repository/             # Repository接口
│   │   └── usecase/                # 用例（待实现）
│   ├── ui/                         # 表现层
│   │   ├── common/                 # 通用UI组件
│   │   ├── transaction/            # 记账模块
│   │   ├── statistics/             # 统计模块
│   │   ├── aianalysis/             # AI分析模块
│   │   └── asset/                  # 资产管理模块
│   ├── di/                         # 依赖注入模块
│   ├── util/                       # 工具类
│   └── worker/                     # 后台任务
├── res/
│   ├── layout/                     # 布局文件
│   ├── drawable/                   # 图形资源
│   ├── values/                     # 值资源
│   └── ...
└── AndroidManifest.xml
```

---

## 🏗️ 架构层次详解

### 1. 表现层 (UI Layer)

#### 基础组件
| 文件 | 作用 |
|------|------|
| `ui/common/BaseViewModel.kt` | 基础ViewModel，提供状态管理（StateFlow）和事件处理（SharedFlow） |

#### 记账模块 (`ui/transaction/`)
| 文件 | 作用 |
|------|------|
| `TransactionContract.kt` | 定义UI状态（TransactionUiState）和事件（TransactionUiEvent），包含收入/支出分类常量 |
| `TransactionViewModel.kt` | 管理记账业务逻辑，处理添加/删除/编辑交易，按日期分组数据 |
| `TransactionFragment.kt` | 记账主页面Fragment，显示账单列表和统计卡片 |
| `DailyTransactionAdapter.kt` | RecyclerView适配器，按日期分组显示交易记录 |
| `AddTransactionBottomSheet.kt` | 添加/编辑交易的底部弹窗 |
| `AddAccountDialog.kt` | 添加账户的对话框 |

#### 统计模块 (`ui/statistics/`)
| 文件 | 作用 |
|------|------|
| `StatisticsContract.kt` | 定义统计页面状态和事件 |
| `StatisticsViewModel.kt` | 处理月度/年度统计数据加载，与后端图表API交互 |

#### AI分析模块 (`ui/aianalysis/`)
| 文件 | 作用 |
|------|------|
| `AIAnalysisContract.kt` | 定义AI分析页面状态和事件 |
| `AIAnalysisViewModel.kt` | 请求AI分析，获取消费建议和历史分析记录 |

#### 资产管理模块 (`ui/asset/`)
| 文件 | 作用 |
|------|------|
| `AssetContract.kt` | 定义资产页面状态和事件 |
| `AssetViewModel.kt` | 管理固定收支、股票持仓，实时计算总资产 |

---

### 2. 领域层 (Domain Layer)

#### 领域模型 (`domain/model/`)
| 文件 | 作用 |
|------|------|
| `Transaction.kt` | 交易记录模型、按日期分组模型、账户模型、交易类型枚举 |
| `FixedIncome.kt` | 固定收支模型，包含频率枚举和每分钟收益计算逻辑 |
| `StockHolding.kt` | 股票持仓模型，包含市值、盈亏计算属性；股票行情模型 |
| `AssetSummary.kt` | 资产汇总模型，包含现金资产、股票资产、每分钟净收入等 |
| `Statistics.kt` | 统计数据模型，分类统计、趋势统计 |
| `AIAnalysis.kt` | AI分析结果模型，消费习惯洞察、建议、预测 |

#### Repository接口 (`domain/repository/`)
| 文件 | 作用 |
|------|------|
| `TransactionRepository.kt` | 交易记录仓库接口：CRUD、按条件查询、服务器同步 |
| `AssetRepository.kt` | 资产仓库接口：固定收支管理、资产快照、实时资产计算 |
| `StockRepository.kt` | 股票仓库接口：持仓管理、行情获取、价格刷新 |
| `StatisticsRepository.kt` | 统计仓库接口：月度/年度统计、分类统计、趋势统计 |
| `AIAnalysisRepository.kt` | AI分析仓库接口：习惯分析、获取建议、历史记录 |

#### 用例 (`domain/usecase/`)
> 目录结构已创建，用于存放业务用例类（可选实现）

---

### 3. 数据层 (Data Layer)

#### 本地数据源

##### Room数据库 (`data/local/`)
| 文件 | 作用 |
|------|------|
| `AppDatabase.kt` | Room数据库定义，包含所有实体和DAO |

##### Entity实体 (`data/local/entity/`)
| 文件 | 作用 |
|------|------|
| `TransactionEntity.kt` | 交易记录表，字段：id、金额、类型、分类、账户ID、备注、日期 |
| `AccountEntity.kt` | 账户表，字段：id、名称、图标、余额、是否默认、排序 |
| `FixedIncomeEntity.kt` | 固定收支表，字段：id、名称、金额、类型、频率、生效日期 |
| `StockHoldingEntity.kt` | 股票持仓表，字段：id、代码、名称、股数、买入价、当前价 |
| `AssetSnapshotEntity.kt` | 资产快照表，字段：id、总资产、现金资产、股票资产、时间戳 |

##### DAO数据访问对象 (`data/local/dao/`)
| 文件 | 作用 |
|------|------|
| `TransactionDao.kt` | 交易记录CRUD，按日期/类型/分类/账户查询 |
| `AccountDao.kt` | 账户CRUD，余额更新，获取总余额 |
| `FixedIncomeDao.kt` | 固定收支CRUD，按类型查询，更新生效状态 |
| `StockHoldingDao.kt` | 股票持仓CRUD，更新价格，计算总市值/成本 |
| `AssetSnapshotDao.kt` | 资产快照插入，按时间范围查询，删除旧快照 |

#### 远程数据源

##### API接口 (`data/remote/api/`)
| 文件 | 作用 |
|------|------|
| `ExpenseApiService.kt` | 交易数据同步API：同步、创建、更新、删除交易 |
| `StatisticsApiService.kt` | 统计API：月度/年度统计、分类统计、趋势统计 |
| `AIAnalysisApiService.kt` | AI分析API：请求分析、获取建议、历史记录 |
| `StockApiService.kt` | 股票API：获取行情、批量行情、搜索股票 |

##### DTO数据传输对象 (`data/remote/dto/`)
| 文件 | 作用 |
|------|------|
| `TransactionDto.kt` | 交易DTO、固定收支DTO、同步请求、通用API响应包装 |
| `StatisticsDto.kt` | 统计请求/响应DTO、分类统计DTO |
| `AIAnalysisDto.kt` | AI分析请求/结果DTO、习惯洞察DTO、建议DTO、预测DTO |
| `StockDto.kt` | 股票行情DTO、批量行情请求/响应、搜索结果 |

#### Repository实现 (`data/repository/`)
| 文件 | 作用 |
|------|------|
| `TransactionRepositoryImpl.kt` | 交易仓库实现，整合本地DAO和远程API |
| `AssetRepositoryImpl.kt` | 资产仓库实现，计算实时资产，管理快照和固定收支 |
| `StockRepositoryImpl.kt` | 股票仓库实现，管理持仓，调用行情API |
| `StatisticsRepositoryImpl.kt` | 统计仓库实现，从后端获取统计数据和图表 |
| `AIAnalysisRepositoryImpl.kt` | AI分析仓库实现，发送分析请求，获取AI建议 |

---

### 4. 依赖注入 (DI Layer)

| 文件 | 作用 |
|------|------|
| `di/AppModule.kt` | 应用级依赖提供（Context等） |
| `di/DatabaseModule.kt` | Room数据库和DAO的依赖注入配置 |
| `di/NetworkModule.kt` | Retrofit、OkHttp、API Service的依赖注入配置 |
| `di/RepositoryModule.kt` | Repository接口与实现类的绑定 |

---

### 5. 工具类 (Util Layer)

| 文件 | 作用 |
|------|------|
| `util/CurrencyUtil.kt` | 货币格式化：金额显示、百分比、大数字简化 |
| `util/DateTimeUtil.kt` | 日期时间工具：获取今日/本月/本年时间戳，格式化 |
| `util/Resource.kt` | 通用资源封装：Success/Error/Loading状态 |

---

### 6. 后台任务 (Worker Layer)

| 文件 | 作用 |
|------|------|
| `worker/AssetSnapshotWorker.kt` | 定时保存资产快照（每15分钟） |
| `worker/StockPriceSyncWorker.kt` | 定时同步股票价格（每15分钟） |

---

## 📱 布局文件

### 主要布局 (`res/layout/`)
| 文件 | 作用 |
|------|------|
| `activity_main.xml` | 主Activity布局，包含Fragment容器 |
| `fragment_transaction.xml` | 记账主页面：余额卡片、今日收支、账单列表、浮动按钮 |
| `item_daily_transactions.xml` | 按日期分组的交易列表项：日期头部、当日交易卡片 |
| `item_transaction.xml` | 单条交易记录项：图标、分类、账户、金额 |
| `dialog_add_transaction.xml` | 添加交易弹窗：类型切换、金额、分类Chip、账户、日期、备注 |
| `dialog_add_account.xml` | 添加账户弹窗：账户名称、初始余额 |

### 图形资源 (`res/drawable/`)
| 文件 | 作用 |
|------|------|
| `bg_circle_gray.xml` | 圆形灰色背景（分类图标） |
| `divider_line.xml` | 分隔线 |
| `ic_launcher_*.xml` | 应用图标 |

---

## 🔄 数据流向

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  Fragment ←→ ViewModel ←→ UiState/UiEvent                   │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                            │
│              Repository Interface / UseCase                  │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                       Data Layer                             │
│  ┌─────────────────┐              ┌─────────────────┐       │
│  │   Local (Room)  │              │  Remote (Retrofit)│      │
│  │  DAO ← Entity   │              │  API ← DTO       │      │
│  └────────┬────────┘              └────────┬────────┘       │
│           └────────────┬───────────────────┘                │
│                        ▼                                    │
│              Repository Implementation                       │
└─────────────────────────────────────────────────────────────┘
```

---

## 🗄️ 数据库表结构

### transactions（交易记录表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| amount | Double | 金额 |
| type | TransactionType | 类型：INCOME/EXPENSE |
| category | String | 分类 |
| accountId | Long | 关联账户ID |
| note | String | 备注 |
| date | Long | 交易日期时间戳 |
| createdAt | Long | 创建时间戳 |

### accounts（账户表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| name | String | 账户名称 |
| icon | String | 图标标识 |
| balance | Double | 当前余额 |
| isDefault | Boolean | 是否默认账户 |
| sortOrder | Int | 排序顺序 |
| createdAt | Long | 创建时间戳 |

### fixed_incomes（固定收支表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| name | String | 名称（如：工资、房租） |
| amount | Double | 金额 |
| type | FixedIncomeType | 类型：INCOME/EXPENSE |
| frequency | FixedIncomeFrequency | 频率：DAILY/WEEKLY/MONTHLY/YEARLY |
| startDate | Long | 开始日期 |
| endDate | Long? | 结束日期（可空） |
| isActive | Boolean | 是否生效 |
| createdAt | Long | 创建时间戳 |

### stock_holdings（股票持仓表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| symbol | String | 股票代码 |
| name | String | 股票名称 |
| shares | Double | 持有股数 |
| purchasePrice | Double | 买入价格 |
| purchaseDate | Long | 买入日期 |
| currentPrice | Double | 当前价格（缓存） |
| lastUpdated | Long | 最后更新时间 |
| createdAt | Long | 创建时间戳 |

### asset_snapshots（资产快照表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| totalAsset | Double | 总资产 |
| cashAsset | Double | 现金资产 |
| stockAsset | Double | 股票资产 |
| timestamp | Long | 快照时间戳 |

---

## 🚀 模块依赖关系

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│    UI       │────▶│   Domain    │────▶│    Data     │
│  (ViewModel)│     │ (Repository │     │ (DAO/API)   │
│             │     │  Interface) │     │             │
└─────────────┘     └─────────────┘     └─────────────┘
       │                   │                   │
       └───────────────────┴───────────────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │     DI      │
                    │   (Hilt)    │
                    └─────────────┘
```

**依赖方向**: UI → Domain → Data（单向依赖，遵循Clean Architecture原则）

---

## 📝 TODO

- [ ] 实现完整的 UseCase 层
- [ ] 添加统计模块 Fragment 和布局
- [ ] 添加 AI 分析模块 Fragment 和布局
- [ ] 添加资产管理模块 Fragment 和布局
- [ ] 实现底部导航栏切换模块
- [ ] 配置实际的后端服务器地址
- [ ] 添加用户登录/注册功能
- [ ] 实现数据云同步
- [ ] 完善 Worker 的 Repository 注入

---

## 📦 构建

```bash
# Debug构建
./gradlew assembleDebug

# APK位置
app/build/outputs/apk/debug/app-debug.apk
```

---

*文档生成时间: 2026年1月29日*

