# FunnyExpenseTracking Android 架构文档

## 项目概述

这是一个基于 MVVM 架构的个人收支管理（记账）Android 应用，采用 Clean Architecture 分层设计。

## 技术栈

- **语言**: Kotlin
- **架构模式**: MVVM + Clean Architecture
- **依赖注入**: Hilt
- **数据库**: Room
- **网络请求**: Retrofit + OkHttp
- **异步处理**: Kotlin Coroutines + Flow
- **后台任务**: WorkManager
- **UI组件**: Jetpack Navigation, ViewBinding

## 功能模块

### 1. 记账模块 (Transaction)
- 每日收支记录
- 分类管理
- 支持多种分类（餐饮、交通、购物等）

### 2. 统计模块 (Statistics)
- 月度/年度统计报表
- 分类统计（饼图、柱状图）
- 图表由后端生成，客户端展示

### 3. AI分析模块 (AIAnalysis)
- 消费习惯分析
- 智能建议
- 消费预测
- 所有AI功能在后端实现

### 4. 资产管理模块 (Asset)
- 固定收支管理（工资、房租等）
- 资产实时计算（按分钟摊薄）
- 资产快照历史

### 5. 股票模块 (Stock)
- 股票持仓管理
- 实时行情同步
- 股票盈亏计算
- 股票资产纳入总资产

## 包结构

```
com.example.funnyexpensetracking/
├── FunnyExpenseApp.kt              # Application类（Hilt入口）
├── MainActivity.kt                  # 主Activity
│
├── di/                             # 依赖注入模块
│   ├── AppModule.kt                # 应用级依赖
│   ├── DatabaseModule.kt           # 数据库依赖
│   ├── NetworkModule.kt            # 网络依赖
│   └── RepositoryModule.kt         # Repository绑定
│
├── data/                           # 数据层
│   ├── local/
│   │   ├── AppDatabase.kt          # Room数据库
│   │   ├── dao/                    # 数据访问对象
│   │   │   ├── TransactionDao.kt
│   │   │   ├── FixedIncomeDao.kt
│   │   │   ├── StockHoldingDao.kt
│   │   │   └── AssetSnapshotDao.kt
│   │   └── entity/                 # 数据库实体
│   │       ├── TransactionEntity.kt
│   │       ├── FixedIncomeEntity.kt
│   │       ├── StockHoldingEntity.kt
│   │       └── AssetSnapshotEntity.kt
│   ├── remote/
│   │   ├── api/                    # Retrofit API接口
│   │   │   ├── ExpenseApiService.kt
│   │   │   ├── StatisticsApiService.kt
│   │   │   ├── AIAnalysisApiService.kt
│   │   │   └── StockApiService.kt
│   │   └── dto/                    # 网络数据传输对象
│   │       ├── TransactionDto.kt
│   │       ├── StatisticsDto.kt
│   │       ├── AIAnalysisDto.kt
│   │       └── StockDto.kt
│   └── repository/                 # Repository实现
│       ├── TransactionRepositoryImpl.kt
│       ├── StatisticsRepositoryImpl.kt
│       ├── AIAnalysisRepositoryImpl.kt
│       ├── AssetRepositoryImpl.kt
│       └── StockRepositoryImpl.kt
│
├── domain/                         # 领域层
│   ├── model/                      # 业务模型
│   │   ├── Transaction.kt
│   │   ├── FixedIncome.kt
│   │   ├── StockHolding.kt
│   │   ├── AssetSummary.kt
│   │   ├── Statistics.kt
│   │   └── AIAnalysis.kt
│   ├── repository/                 # Repository接口
│   │   ├── TransactionRepository.kt
│   │   ├── StatisticsRepository.kt
│   │   ├── AIAnalysisRepository.kt
│   │   ├── AssetRepository.kt
│   │   └── StockRepository.kt
│   └── usecase/                    # 用例
│       ├── transaction/
│       │   └── TransactionUseCases.kt
│       ├── statistics/
│       │   └── StatisticsUseCases.kt
│       ├── asset/
│       │   └── AssetUseCases.kt
│       ├── stock/
│       │   └── StockUseCases.kt
│       └── ai/
│           └── AIAnalysisUseCases.kt
│
├── ui/                             # UI层
│   ├── common/
│   │   └── BaseViewModel.kt        # 基础ViewModel
│   ├── transaction/
│   │   ├── TransactionContract.kt  # 状态和事件定义
│   │   └── TransactionViewModel.kt
│   ├── statistics/
│   │   ├── StatisticsContract.kt
│   │   └── StatisticsViewModel.kt
│   ├── asset/
│   │   ├── AssetContract.kt
│   │   └── AssetViewModel.kt
│   └── aianalysis/
│       ├── AIAnalysisContract.kt
│       └── AIAnalysisViewModel.kt
│
├── worker/                         # 后台任务
│   ├── AssetSnapshotWorker.kt      # 资产快照定时任务
│   └── StockPriceSyncWorker.kt     # 股票价格同步任务
│
└── util/                           # 工具类
    ├── Resource.kt                 # 网络结果封装
    ├── DateTimeUtil.kt             # 日期时间工具
    └── CurrencyUtil.kt             # 货币格式化工具
```

## 模块依赖关系

```
┌─────────────────────────────────────────────────────────────┐
│                         UI 层                               │
│     Fragment/Activity + ViewModel + State/Event             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │Transaction│ │Statistics│ │  Asset   │ │AIAnalysis│       │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘       │
└───────┼────────────┼────────────┼────────────┼──────────────┘
        │            │            │            │
        ▼            ▼            ▼            ▼
┌─────────────────────────────────────────────────────────────┐
│                       Domain 层                             │
│              UseCase + Repository接口 + Model               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ TransactionUseCases  StatisticsUseCases  AssetUseCases│  │
│  │ StockUseCases        AIAnalysisUseCases               │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                        Data 层                              │
│         Repository实现 + Room DAO + Retrofit API            │
│  ┌─────────────────────┐    ┌─────────────────────┐        │
│  │      Local          │    │       Remote        │        │
│  │  ┌───────────────┐  │    │  ┌───────────────┐  │        │
│  │  │ Room Database │  │    │  │ Retrofit APIs │  │        │
│  │  │     DAOs      │  │    │  │     DTOs      │  │        │
│  │  └───────────────┘  │    │  └───────────────┘  │        │
│  └─────────────────────┘    └─────────────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

## 数据流向

```
用户操作 → ViewModel → UseCase → Repository → 
    ├── Local (Room) → 返回数据
    └── Remote (Retrofit) → 返回数据
        ↓
    合并/处理数据 → 更新 UI State → UI 更新
```

## 实时资产计算逻辑

1. **基础资产** = 历史收入总额 - 历史支出总额
2. **固定收支摊薄** = 固定收入/支出按分钟计算
3. **股票资产** = 持仓股数 × 当前价格
4. **总资产** = 基础资产 + 股票资产 + (每分钟净收入 × 经过分钟数)

## 后端API需求

### 1. 记账同步API
- POST `/api/transactions/sync` - 同步交易记录

### 2. 统计API
- POST `/api/statistics/monthly` - 获取月度统计（含图表URL）
- POST `/api/statistics/yearly` - 获取年度统计（含图表URL）

### 3. AI分析API
- POST `/api/ai/analyze` - 请求AI分析
- GET `/api/ai/suggestions` - 获取AI建议

### 4. 股票API
- GET `/api/stock/quote/{symbol}` - 获取单个股票行情
- POST `/api/stock/quotes` - 批量获取股票行情

## 后续开发建议

1. **UI实现**: 需要为每个模块创建Fragment和布局文件
2. **Navigation**: 配置Navigation Graph实现页面导航
3. **主题**: 支持深色模式
4. **国际化**: 支持多语言
5. **单元测试**: 为UseCase和Repository编写测试
6. **错误处理**: 完善网络错误和本地数据错误处理
7. **数据同步**: 实现本地数据与服务器的双向同步
8. **通知**: 添加账单提醒功能

## 配置说明

在 `NetworkModule.kt` 中修改 `BASE_URL` 为你的后端服务器地址：

```kotlin
private const val BASE_URL = "https://your-backend-server.com/api/"
```

