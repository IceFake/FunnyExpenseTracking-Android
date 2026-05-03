# 💰 FunnyExpenseTracking (趣味记账)

<p align="center">
  <strong>基于 MVVM + Clean Architecture 的现代化 Android 智能记账应用</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Android-API24+-3DDC84?style=for-the-badge&logo=android" alt="Android">
  <img src="https://img.shields.io/badge/Gradle-8.10.1-02303A?style=for-the-badge&logo=gradle" alt="Gradle">
  <img src="https://img.shields.io/badge/Architecture-MVVM%2BClean-009688?style=for-the-badge" alt="Architecture">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License">
</p>

---

## 📑 目录

- [项目概述](#-项目概述)
- [整体架构](#-整体架构)
- [技术栈全景](#-技术栈全景)
- [功能模块详解](#-功能模块详解)
  - [记账模块](#1--智能记账模块)
  - [日历视图](#2--日历视图模块)
  - [固定收支管理](#3--固定收支管理模块)
  - [投资理财追踪](#4--投资理财追踪模块)
  - [实时资产计算](#5--实时资产计算引擎)
  - [AI 智能分析](#6--ai-智能分析模块)
  - [AI 自然语言问答](#7--ai-自然语言财务问答模块)
  - [统计报表](#8--统计报表模块)
  - [历史账单](#9--历史账单模块)
  - [数据备份与恢复](#10--数据备份与恢复模块)
- [技术栈深度解析](#-技术栈深度解析)
  - [Hilt 依赖注入](#1-hilt-依赖注入)
  - [Room 数据库](#2-room-本地数据库)
  - [Retrofit + OkHttp 网络层](#3-retrofit--okhttp-网络层)
  - [Kotlin Coroutines + Flow 异步处理](#4-kotlin-coroutines--flow-异步处理)
  - [WorkManager 后台任务](#5-workmanager-后台任务调度)
  - [MVI 状态管理](#6-mvi-状态管理模式)
  - [安全存储](#7-安全存储-encryptedsharedpreferences)
  - [Markdown 渲染](#8-markdown-渲染)
  - [MPAndroidChart 图表](#9-mpandroidchart-图表可视化)
  - [网络监控](#10-网络状态监控)
- [项目结构](#-项目结构)
- [快速入门](#-快速入门)

---

## 📖 项目概述

FunnyExpenseTracking 是一款功能全面的 Android 个人收支管理应用。项目采用 **单 Activity + 多 Fragment** 架构，严格遵循 **MVVM + Clean Architecture** 三层分离设计，通过 Hilt 实现依赖注入，Room 提供离线优先的本地数据持久化，Retrofit 完成远程 API 通信，WorkManager 执行周期性后台任务，并深度集成 DeepSeek AI 实现消费习惯分析与自然语言财务问答。

**核心能力一览：**

| 能力 | 实现方式 |
|------|---------|
| 多账户收支记录 | Room + DAO + Flow 响应式数据流 |
| 日历可视化 | 自定义 CalendarAdapter + 底部弹窗 |
| 固定收支累计 | 分钟级精度时间算法 + 定时器每 60 秒更新 |
| 投资行情同步 | 新浪财经 API + WorkManager 15 分钟轮询 |
| 实时资产计算 | StateFlow 多源合并 + 分钟级推送 |
| AI 消费分析 | DeepSeek API（OpenAI 兼容格式）+ JSON 模式 |
| AI 自然语言问答 | DeepSeek Chat + 多轮对话上下文 + Markdown 渲染 |
| 统计图表 | MPAndroidChart 饼图 + 本地数据聚合 |
| 数据备份恢复 | JSON 序列化/反序列化 + 多表联合导出 |
| 离线优先同步 | SyncStatus 状态机 + NetworkMonitor 触发 |

---

## 🏗️ 整体架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              UI 层 (Presentation)                          │
│   Fragment / Compose + ViewModel + MVI (StateFlow + SharedFlow)            │
│   ┌───────────┐ ┌──────────┐ ┌───────────┐ ┌──────────┐ ┌───────────┐    │
│   │Transaction │ │ Calendar │ │FixedIncome│ │Investment│ │AIAnalysis │    │
│   │ Fragment   │ │ Fragment │ │  Fragment │ │ Fragment │ │  Fragment │    │
│   └─────┬─────┘ └────┬─────┘ └─────┬─────┘ └────┬─────┘ └─────┬─────┘    │
│         │            │             │            │             │           │
│   ┌─────┴─────┐ ┌────┴─────┐ ┌────┴──────┐ ┌───┴──────┐ ┌───┴───────┐   │
│   │Transaction │ │ Calendar │ │FixedIncome│ │Investment│ │AIAnalysis │   │
│   │ ViewModel  │ │ ViewModel│ │ ViewModel │ │ ViewModel│ │ ViewModel │   │
│   └─────┬─────┘ └────┬─────┘ └─────┬─────┘ └────┬─────┘ └─────┬─────┘   │
└─────────┼────────────┼─────────────┼────────────┼─────────────┼──────────┘
          │            │             │            │             │
          ▼            ▼             ▼            ▼             ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Domain 层 (Business)                            │
│          UseCase + Repository 接口 + Domain Model                         │
│   ┌────────────────────┐  ┌─────────────────────────────────────┐         │
│   │  RealtimeAsset     │  │  TransactionUseCases                │         │
│   │  Calculator        │  │  AssetUseCases / StockUseCases      │         │
│   │                    │  │  AIAnalysisUseCases / StatisticsUC  │         │
│   └────────┬───────────┘  └──────────────────┬──────────────────┘         │
│            │                                 │                            │
│   ┌────────┴─────────────────────────────────┴────────────────────┐       │
│   │  Repository 接口（TransactionRepo / AssetRepo / AccountRepo   │       │
│   │  InvestmentRepo / AIAnalysisRepo / FinancialQueryRepo ...）   │       │
│   └──────────────────────────┬────────────────────────────────────┘       │
└──────────────────────────────┼───────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                             Data 层 (Infrastructure)                       │
│         Repository 实现 + Room DAO + Retrofit API + WorkManager           │
│   ┌────────────────────────┐       ┌─────────────────────────────┐        │
│   │       Local            │       │         Remote               │        │
│   │  ┌──────────────────┐  │       │  ┌───────────────────────┐  │        │
│   │  │  Room Database   │  │       │  │  DeepSeek API         │  │        │
│   │  │  (8张核心表)      │  │       │  │  新浪财经 API          │  │        │
│   │  │  DAO + Entity    │  │       │  │  后端同步 API          │  │        │
│   │  └──────────────────┘  │       │  └───────────────────────┘  │        │
│   │  ┌──────────────────┐  │       │  ┌───────────────────────┐  │        │
│   │  │ UserPreferences  │  │       │  │  Retrofit + OkHttp    │  │        │
│   │  │ (加密存储API Key) │  │       │  │  Gson / Scalars      │  │        │
│   │  └──────────────────┘  │       │  └───────────────────────┘  │        │
│   └────────────────────────┘       └─────────────────────────────┘        │
└─────────────────────────────────────────────────────────────────────────────┘
```

**数据流向：**

```
用户操作 → Fragment → ViewModel (发送 Intent)
    → UseCase → Repository 接口
        → Repository 实现：
            ├── Room DAO (本地读写) → Flow<Entity> → 映射为 Domain Model
            └── Retrofit API (远程通信) → Response<DTO> → 映射为 Domain Model
        → 合并/处理 → 更新 StateFlow → Fragment 观察 UI State → 渲染界面
```

---

## 🧰 技术栈全景

| 技术领域 | 库/框架 | 版本 | 在项目中的角色 |
|---------|---------|------|---------------|
| **语言** | Kotlin | 2.0.21 | 全项目开发语言，使用 Coroutines + Flow 作为异步基础 |
| **UI 框架** | ViewBinding + Material Design 3 | Material 1.13.0 | 主要 UI 层，Fragment + XML 布局 + MD3 组件 |
| **Compose** | Jetpack Compose | BOM 2024.12.01 | 统计模块部分页面使用 Compose 构建 |
| **架构模式** | MVVM + Clean Architecture + MVI | — | BaseViewModel 封装 StateFlow/SharedFlow 状态管理 |
| **依赖注入** | Hilt (Dagger) | 2.54 | 4 个 Module 管理全局单例（DB/Network/Repo/App）|
| **本地存储** | Room Database | 2.6.1 | 8 张核心表 + 8 个 DAO + TypeConverter + Migration |
| **网络请求** | Retrofit + OkHttp | 2.11.0 / 4.12.0 | 5 个 Retrofit 实例（默认/Yahoo/新浪/DeepSeek/通用）|
| **序列化** | Gson + Scalars | 2.11.0 | JSON 解析（DTO/AI 响应）+ 纯文本解析（新浪行情）|
| **异步处理** | Kotlin Coroutines + Flow | 1.9.0 | 全局异步操作 + 响应式数据流 |
| **后台任务** | WorkManager | 2.10.0 | 3 个 Worker（资产快照/股价同步/数据同步）|
| **导航组件** | Jetpack Navigation | 2.8.5 | Fragment 间导航 + BottomNavigation 集成 |
| **图表库** | MPAndroidChart | v3.1.0 | 统计模块饼图（分类占比可视化）|
| **Markdown** | Markwon | 4.6.2 | AI 回复 Markdown 渲染（支持表格/删除线/列表）|
| **安全存储** | EncryptedSharedPreferences | 1.1.0-alpha06 | AES256-GCM 加密存储 DeepSeek API Key |
| **AI 引擎** | DeepSeek API（OpenAI 兼容） | deepseek-chat | 消费分析 + 自然语言问答 |
| **股票行情** | 新浪财经 API | — | A 股/港股/美股实时行情获取与解析 |
| **符号处理** | KSP | 2.0.21-1.0.28 | Room/Hilt 编译期注解处理，替代 kapt |
| **构建工具** | Gradle Kotlin DSL | 8.10.1 | Version Catalog 统一依赖管理 |

---

## 🔧 功能模块详解

### 1. 📝 智能记账模块

> **核心文件：** `TransactionFragment` → `TransactionViewModel` → `TransactionUseCases` → `TransactionRepositoryImpl` → `TransactionDao` + `TransactionEntity`

#### 功能说明

支持多账户收支记录管理，预置 10 种支出类别（餐饮、交通、购物、娱乐、医疗、教育、居住、通讯、服饰、其他）和 7 种收入类别（工资、奖金、投资收益、兼职、红包、退款、其他），通过底部弹窗快速记账。

#### 实现细节

- **数据模型**：`TransactionEntity` 使用 Room `@Entity` 注解映射到 `transactions` 表，包含金额、类型（`TransactionType` 枚举：`INCOME`/`EXPENSE`）、分类、账户 ID、备注、日期等字段
- **同步状态追踪**：每条记录携带 `SyncStatus` 枚举（`SYNCED`/`PENDING_UPLOAD`/`PENDING_DELETE`/`CONFLICT`），实现离线优先策略
- **DAO 层**：`TransactionDao` 提供 `getAllTransactions()`、`getTransactionsByDateRange()`、`getTransactionsByType()` 等 Flow 查询，查询结果自动过滤 `PENDING_DELETE` 状态的记录
- **Repository 层**：`TransactionRepositoryImpl` 注入 `TransactionDao`、`ExpenseApiService`、`NetworkMonitor`、`SyncManager`，所有写操作先本地再尝试同步：
  ```
  addTransaction() → 插入 Entity (status=PENDING_UPLOAD) → trySync()
  deleteTransaction() → 有 serverId 则标记 PENDING_DELETE，无则直接删除
  ```
- **ViewModel 层**：`TransactionViewModel` 继承 `BaseViewModel<TransactionUiState, TransactionUiEvent>`，通过 `StateFlow` 驱动 UI 更新，使用 MVI 模式管理状态
- **UI 层**：`TransactionFragment` 使用 ViewBinding 绑定布局，`AddTransactionBottomSheet` 实现快速记账弹窗，`DailyTransactionAdapter` 按日期分组展示记录
- **多账户支持**：`AccountEntity` 存储在 `accounts` 表，支持添加、编辑、设置默认账户。通过 `AddAccountDialog` 创建账户，`EditAccountBottomSheet` 编辑

---

### 2. 📅 日历视图模块

> **核心文件：** `CalendarFragment` → `CalendarViewModel` / `DayDetailViewModel` → `TransactionDao` + `CalendarAdapter`

#### 功能说明

按月展示每日收支情况，颜色标记金额，点击日期弹出底部弹窗查看当日明细。

#### 实现细节

- **日历渲染**：`CalendarAdapter` 基于 RecyclerView 实现月历网格，每个日期格子根据当日收支额度显示不同颜色标记
- **数据加载**：`CalendarViewModel` 通过 `TransactionDao.getTransactionsByDateRange()` 获取整月的 Flow 数据，计算每日收入/支出汇总
- **日期导航**：支持月份前后切换，ViewModel 维护当前年月状态，切换时重新查询对应月份的数据
- **日详情弹窗**：`DayDetailBottomSheet` + `DayDetailViewModel` 独立加载选中日期的交易列表，展示当日收入、支出、结余
- **月统计**：自动计算并展示本月总收入、总支出、月结余

---

### 3. 💼 固定收支管理模块

> **核心文件：** `FixedIncomeFragment` → `FixedIncomeViewModel` → `AssetRepository` → `FixedIncomeDao` + `FixedIncomeEntity`

#### 功能说明

管理工资、房租、水电费、订阅服务等周期性收支，支持每日/每周/每月/每年四种频率，通过累计生效分钟数实时计算累计金额。

#### 实现细节

- **数据模型**：`FixedIncomeEntity` 包含：
  - `frequency`：`FixedIncomeFrequency` 枚举（`DAILY`/`WEEKLY`/`MONTHLY`/`YEARLY`），每种频率对应精确的分钟数（如 `MONTHLY` = 30×24×60 = 43200 分钟）
  - `accumulatedMinutes`：累计生效分钟数
  - `accumulatedAmount`：累计金额
  - `lastRecordTime`：上次记录时间戳
  - `isActive`：是否启用
  - `endDate`：可选的结束日期
- **累计金额算法**（`FixedIncomeFrequency.calculateAccumulatedAmount()`）：
  ```
  completeCycles = accumulatedMinutes / minutesPerCycle    // 完整周期数
  remainingMinutes = accumulatedMinutes % minutesPerCycle  // 剩余分钟
  累计金额 = completeCycles × 周期金额 + (remainingMinutes / minutesPerCycle × 周期金额)
  ```
- **生效判断**：`FixedIncomeEntity.isEffectiveAt(timestamp)` 检查 `isActive && timestamp >= startDate && (endDate == null || timestamp <= endDate)`
- **每分钟速率**：`getAmountPerMinute() = amount / frequency.getMinutesPerCycle()`
- **定时更新**：由 `RealtimeAssetCalculator` 每 60 秒调用 `updateFixedIncomeAccumulations()`，遍历所有固定收支条目，计算有效时间差并更新 `accumulatedMinutes`
- **UI 层**：`FixedIncomeAdapter` 展示列表，`AddFixedIncomeBottomSheet` 提供添加/编辑弹窗，支持按全部/仅收入/仅支出筛选

---

### 4. 💹 投资理财追踪模块

> **核心文件：** `InvestmentFragment` → `InvestmentViewModel` → `InvestmentRepositoryImpl` → `InvestmentDao` + `InvestmentEntity` + `SinaFinanceApiService` + `StockPriceSyncWorker`

#### 功能说明

支持股票和其他投资品类管理，集成新浪财经 API 获取 A 股/港股/美股实时行情。

#### 实现细节

- **数据模型**：`InvestmentEntity` 使用 `InvestmentCategory` 枚举区分 `STOCK`（股票）和 `OTHER`（其他）：
  - 股票：`description` 存储股票代码（如 `sh600519`），`quantity` 记录持仓数量，`currentPrice` 由 API 更新
  - 其他：`currentValue` 手动设置当前价值
  - 收益计算：`calcCurrentValue()` 根据品类计算当前市值，`getProfitLoss()` = 当前市值 - 投入金额

- **新浪财经 API 集成**：
  - `SinaFinanceApiService` 接口定义 `@GET("list")` 端点，参数 `@Query("list")` 传入逗号分隔的股票代码
  - 响应为纯文本格式（如 `var hq_str_sh600519="贵州茅台,1800.00,..."`），由 `SinaQuoteParser` 手动解析
  - 解析器按股票代码前缀区分市场：`sh`/`sz`=A 股、`hk`=港股、`gb_`=美股，各有不同的字段位置映射
  - Retrofit 实例使用 `ScalarsConverterFactory`（非 Gson）以纯文本方式接收响应
  - OkHttp 添加 `Referer: https://finance.sina.com.cn/` 请求头，防止被反爬拦截

- **后台价格同步**：`StockPriceSyncWorker`（`@HiltWorker`）每 15 分钟执行一次：
  1. 从 `investments` 表和 `stock_holdings` 表收集所有股票代码并去重
  2. 批量调用 `SinaFinanceApiService.getQuotes()`
  3. 解析响应后分别更新两张表的 `currentPrice`
  4. 失败自动重试（最多 3 次），区分网络不可用/超时/其他异常

- **智能合并显示**：UI 层对相同描述/代码的投资条目自动合并展示

---

### 5. 📊 实时资产计算引擎

> **核心文件：** `RealtimeAssetCalculator` → `AccountRepository` + `AssetRepository` + `InvestmentRepository` + `AssetBaselineDao`

#### 功能说明

每分钟重新计算一次总资产，公式：**总资产 = 各账户余额之和 + 固定收支累计净额 + 投资/理财当前市值**。

#### 实现细节

- **架构设计**：`RealtimeAssetCalculator` 是 `@Singleton`，由 Hilt 注入，在 `init` 块中启动三路 Flow 监听 + 一个定时器
- **多源监听**：
  ```kotlin
  observeAccountChanges()     → accountRepository.getTotalBalanceFlow().distinctUntilChanged()
  observeFixedIncomeChanges() → combine(assetRepository.getAllActiveFixedIncomes(), ...)
  observeInvestmentChanges()  → investmentRepository.getTotalCurrentValueFlow().distinctUntilChanged()
  ```
  任一数据源变化均触发 `recalculateAsset()`
- **定时更新**：`startFixedIncomeAccumulationUpdate()` 启动协程每 60 秒执行 `updateFixedIncomeAccumulations()`，更新所有固定收支的累计分钟数并重算
- **数据输出**：`RealtimeAssetData` 数据类通过 `StateFlow` 暴露：
  ```kotlin
  data class RealtimeAssetData(
      val currentAsset: Double,           // 当前资产值
      val incomePerMinute: Double,        // 每分钟收入
      val expensePerMinute: Double,       // 每分钟支出
      val netChangePerMinute: Double,     // 每分钟净变动
      val totalAccountBalance: Double,    // 账户余额总和
      val totalFixedIncome: Double,       // 固定收入累计
      val totalFixedExpense: Double,      // 固定支出累计
      val totalInvestmentValue: Double    // 投资市值
  )
  ```
- **资产快照**：`AssetSnapshotWorker` 每 15 分钟拍摄一次快照，存入 `asset_snapshots` 表（`totalAsset`/`cashAsset`/`stockAsset`），自动清理 30 天前的旧数据

---

### 6. 🤖 AI 智能分析模块

> **核心文件：** `AIAnalysisFragment` → `AIAnalysisViewModel` → `AIAnalysisUseCases` → `DeepSeekAnalysisRepositoryImpl` → `DeepSeekApiService`

#### 功能说明

基于 DeepSeek AI（兼容 OpenAI Chat Completion 格式）分析用户最近 3 个月的消费记录，生成消费习惯洞察、省钱建议和下月预测。

#### 实现细节

- **API 接口**：`DeepSeekApiService` 定义单一端点 `@POST("v1/chat/completions")`，接收 `OpenAIChatRequest`（包含 `model`/`messages`/`temperature`/`maxTokens`/`responseFormat`），返回 `OpenAIChatResponse`
- **DTO 设计**：完整的 OpenAI 兼容数据结构：
  - `OpenAIChatRequest`：支持 `ResponseFormat(type="json_object")` 强制 AI 返回 JSON
  - `OpenAIChatResponse`：包含 `choices`、`usage`（Token 用量）、`error` 处理
  - `OpenAIChatMessage`：`role`（system/user/assistant）+ `content`
- **分析流程**：
  1. 从 `UserPreferencesManager` 获取加密存储的 API Key（空则返回 `ERROR_API_KEY_MISSING`）
  2. 通过 `TransactionDao.getAllTransactions()` 获取近 3 个月交易记录
  3. `buildAnalysisPrompt()` 构建结构化数据摘要：总收支、月度统计、分类明细（按金额降序）
  4. System Prompt 指定返回纯 JSON 格式（含 `summary`/`habits`/`suggestions`/`prediction` 字段）
  5. 调用 API → 解析 JSON 响应 → 映射为 `AIAnalysisResult` 领域模型
  6. 错误处理覆盖：401（Key 无效）、402（余额不足）、429（限流）、500/503（服务端错误）、超时、网络不可用
- **结果缓存**：分析结果通过 `UserPreferencesManager` 持久化到 SharedPreferences，支持历史分析记录查看
- **Retrofit 配置**：DeepSeek 专用 OkHttpClient 配置 120 秒超时 + 自动重试拦截器（处理 429/500/503），不记录请求日志以避免泄露 API Key

---

### 7. 💬 AI 自然语言财务问答模块

> **核心文件：** `FinancialQueryFragment` → `FinancialQueryViewModel` → `FinancialQueryRepositoryImpl` → `DeepSeekApiService` + `ChatMessageAdapter`

#### 功能说明

支持用自然语言查询财务状况（如"本月花了多少钱"、"哪个类别花得最多"），聊天式对话界面，支持多轮对话上下文关联。

#### 实现细节

- **财务上下文构建**：`buildFinancialContext()` 实时从 4 张 DAO 表汇总数据快照：
  - 账户信息（名称、余额、是否默认）
  - 本月收支概况（收入/支出/结余 + 分类明细）
  - 固定收支列表（名称、金额、频率、累计）
  - 投资持仓（品类、数量、市值、盈亏）
- **多轮对话**：保留最近 5 轮（10 条消息）历史，作为 `messages` 列表传给 API，实现上下文关联
- **System Prompt**：定义"趣味记账 AI"角色，指示基于财务数据回答、使用中文 + emoji、支持 Markdown 格式
- **Markdown 渲染**：AI 回复通过 `MarkdownRenderer.render(textView, markdown)` 渲染到 `ChatMessageAdapter` 的消息气泡中
- **Markwon 配置**：单例初始化，启用 `StrikethroughPlugin`（删除线）和 `TablePlugin`（表格），支持加粗、列表、代码块、标题等
- **不使用 JSON 模式**：与消费分析模块不同，`responseFormat = null`，直接返回自然语言 + Markdown

---

### 8. 📈 统计报表模块

> **核心文件：** `StatisticsFragment` → `StatisticsViewModel` → `StatisticsUseCases` → `StatisticsRepositoryImpl` → `TransactionDao` + `MPAndroidChart`

#### 功能说明

月度/年度统计报表，饼图展示分类占比，列表展示各分类金额及百分比。

#### 实现细节

- **双数据源策略**：`StatisticsRepositoryImpl` 优先调用远端 `StatisticsApiService`，失败时自动回退到本地数据：
  ```kotlin
  getMonthlyStatistics() → try { API } catch { getLocalMonthlyStatistics() }
  ```
- **本地统计聚合**：`getLocalMonthlyStatistics()` 从 `TransactionDao.getTransactionsByDateRange()` 获取数据，按 `TransactionType` 分组，计算各类别金额和占比（`amount / total * 100`）
- **领域模型**：
  - `Statistics`：包含 `period`（月/年）、`totalIncome`/`totalExpense`/`netIncome`、`categoryBreakdown: List<CategoryStat>`
  - `CategoryStat`：`category`/`amount`/`percentage`/`type`
  - `TrendStatistics`：`periodStats`/`avgIncome`/`avgExpense`/`incomeTrend`/`expenseTrend`
- **趋势判断**：`calculateTrend()` 将时间序列分为前后两半，计算变化百分比，>5% 为 `INCREASING`，<-5% 为 `DECREASING`，否则 `STABLE`
- **饼图可视化**：使用 `MPAndroidChart` 的 `PieChart` 组件渲染分类占比，`CategoryStatAdapter` 列表展示明细
- **Compose 集成**：统计模块部分页面使用 Jetpack Compose 构建（`ui/compose/statistics/`）

---

### 9. 📜 历史账单模块

> **核心文件：** `HistoryFragment` → `HistoryViewModel` → `TransactionDao` + `TransactionAdapter`

#### 功能说明

查看指定日期的所有交易记录明细，展示当日收入、支出、结余。

#### 实现细节

- **数据查询**：`HistoryViewModel` 通过 `TransactionDao.getTransactionsByDateRange()` 查询选定日期范围内的记录
- **日收支统计**：在 ViewModel 中实时计算当日收入/支出/结余
- **记录管理**：支持删除历史交易记录，删除逻辑遵循离线优先策略（有 serverId 标记 `PENDING_DELETE`，无则直接删除）
- **UI 展示**：`TransactionAdapter` 使用 RecyclerView 展示交易列表

---

### 10. 💾 数据备份与恢复模块

> **核心文件：** `UserCenterFragment` → `UserCenterViewModel` → `DataManagementRepositoryImpl` → 全部 7 个 DAO

#### 功能说明

一键导出/导入全量数据（JSON 格式），支持清空所有数据。

#### 实现细节

- **导出流程**（`exportAllData()`）：
  1. 从 7 个 DAO 分别获取所有数据：`transactions`（含账户名映射）、`accounts`、`fixed_incomes`、`investments`、`stock_holdings`、`asset_baseline`、`asset_snapshots`（最近 100 条）
  2. 每类 Entity 通过 `toBackup()` 转换为 `BackupData` 领域模型
  3. 使用 Gson 序列化为 JSON 字符串写入文件

- **导入流程**（`importData()`）：
  1. 账户按名称匹配：已存在则叠加余额，不存在则新增
  2. 交易记录全部新增（不去重），无法匹配账户的记录自动创建默认账户
  3. 固定收支按名称+类型匹配：已存在则叠加累计时间并重算金额，不存在则新增
  4. 投资按描述+品类匹配：已存在则叠加数量和金额，不存在则新增
  5. 返回 `ImportResult`，包含各类数据的新增/合并/错误计数

- **数据清除**：清空所有 7 张表的数据

---

## 🔬 技术栈深度解析

### 1. Hilt 依赖注入

**Hilt** 是基于 Dagger 的 Android 依赖注入框架，本项目通过 4 个 Module 管理所有依赖：

| Module | 作用 | 提供的依赖 |
|--------|------|-----------|
| `AppModule` | 应用级依赖 | `FunnyExpenseApp` 实例 |
| `DatabaseModule` | 数据库依赖 | `AppDatabase` 单例 + 8 个 DAO + 4 个数据库 Migration |
| `NetworkModule` | 网络依赖 | 5 个 OkHttpClient + 5 个 Retrofit 实例 + 各 API Service |
| `RepositoryModule` | Repository 绑定 | 8 个 Repository 接口 → 实现类绑定（`@Binds`）|

**关键注解使用：**
- `@HiltAndroidApp`：`FunnyExpenseApp` 作为 Hilt 入口
- `@AndroidEntryPoint`：`MainActivity` 启用 Hilt 注入
- `@HiltWorker`：`AssetSnapshotWorker`/`StockPriceSyncWorker` 支持 WorkManager + Hilt
- `@Singleton` + `@InstallIn(SingletonComponent::class)`：全局单例生命周期
- `@Named("deepSeek")`/`@Named("sinaFinance")` 等：区分多个同类型实例

**WorkManager 集成**：`FunnyExpenseApp` 实现 `Configuration.Provider` 接口，注入 `HiltWorkerFactory` 使 Worker 支持依赖注入。

---

### 2. Room 本地数据库

**Room** 是 Android Jetpack 的 ORM 框架，本项目定义了 **8 张核心数据表**：

| 表名 | Entity | 主要字段 | 用途 |
|------|--------|---------|------|
| `transactions` | `TransactionEntity` | amount, type, category, accountId, syncStatus | 交易记录 |
| `accounts` | `AccountEntity` | name, balance, isDefault, syncStatus | 账户管理 |
| `fixed_incomes` | `FixedIncomeEntity` | amount, frequency, accumulatedMinutes, lastRecordTime | 固定收支 |
| `investments` | `InvestmentEntity` | category, quantity, currentPrice, currentValue | 投资管理 |
| `stock_holdings` | `StockHoldingEntity` | symbol, shares, purchasePrice, totalCost | 股票持仓 |
| `asset_snapshots` | `AssetSnapshotEntity` | totalAsset, cashAsset, stockAsset, timestamp | 资产快照 |
| `asset_baseline` | `AssetBaselineEntity` | baselineAmount, timestamp | 资产基准 |
| `sync_metadata` | `SyncMetadataEntity` | lastSyncTime, syncStatus | 同步元数据 |

**数据库配置：**
- 数据库名称：`funny_expense.db`，当前版本 7
- 使用 `@TypeConverters(Converters::class)` 处理自定义类型转换
- KSP（`2.0.21-1.0.28`）替代 kapt 进行编译期注解处理

**Migration 策略**（4 个手动迁移）：

| 版本 | 变更内容 |
|------|---------|
| 3 → 4 | `fixed_incomes` 添加 `accumulatedAmount` 字段 |
| 4 → 5 | 新增 `investments` 表 |
| 5 → 6 | `stock_holdings` 添加 `totalCost` 字段 + 数据回填 |
| 6 → 7 | `fixed_incomes` 添加 `accumulatedMinutes` + `lastRecordTime` 字段 |

**DAO 层设计**：所有列表查询返回 `Flow<List<Entity>>`，实现数据变更后 UI 自动刷新。

---

### 3. Retrofit + OkHttp 网络层

本项目配置了 **5 个独立的 Retrofit 实例**，通过 Hilt `@Named` 注解区分：

| 实例名称 | Base URL | Converter | 特殊配置 | 用途 |
|---------|----------|-----------|---------|------|
| `default` | 后端服务器 | Gson | 标准日志 | 数据同步 API |
| `yahooFinance` | `query1.finance.yahoo.com` | Gson | UA/Accept 头 | Yahoo 兼容（备用）|
| `sinaFinance` | `hq.sinajs.cn` | **Scalars** | Referer 头 | 新浪财经行情 |
| `deepSeek` | `api.deepseek.com` | Gson | 120 秒超时 + 重试拦截器 + 无日志 | AI API |

**OkHttp 拦截器：**
- **日志拦截器**：`HttpLoggingInterceptor(Level.BODY)` 用于调试（DeepSeek 实例除外）
- **请求头拦截器**：为新浪/Yahoo API 添加 `User-Agent`、`Referer`、`Accept` 等头部，规避反爬策略
- **重试拦截器**（DeepSeek）：捕获 429/500/503 状态码，指数退避重试最多 3 次

**Gson 解析**：通过 `@SerializedName` 注解映射 JSON 字段名，`OpenAIDto.kt` 完整定义了 OpenAI Chat Completion 的请求/响应结构。

**Scalars 解析**：新浪财经 API 返回纯文本（非 JSON），使用 `ScalarsConverterFactory` 以 `String` 方式接收后由 `SinaQuoteParser` 手动正则解析。

---

### 4. Kotlin Coroutines + Flow 异步处理

**Coroutines** 和 **Flow** 是本项目的异步处理核心：

| 场景 | 使用方式 |
|------|---------|
| DAO 查询 | `suspend fun` 单次查询 / `fun xxx(): Flow<List<T>>` 响应式监听 |
| API 调用 | `suspend fun`（Retrofit 原生协程支持）|
| ViewModel | `viewModelScope.launch {}` 启动协程 |
| 状态管理 | `MutableStateFlow` / `MutableSharedFlow` |
| 数据合并 | `combine()` 合并多个 Flow，`distinctUntilChanged()` 去重 |
| 资产计算 | `CoroutineScope(Dispatchers.Default)` 独立作用域 |
| WorkManager | `CoroutineWorker` 在后台线程执行挂起函数 |
| 网络监控 | `callbackFlow {}` 将回调 API 转换为 Flow |

**BaseViewModel 封装**：
```kotlin
abstract class BaseViewModel<S : UiState, E : UiEvent> : ViewModel() {
    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<S>                    // UI 观察此 Flow

    private val _uiEvent = MutableSharedFlow<E>()
    val uiEvent                                  // 一次性事件（Toast/导航等）

    protected fun updateState(reducer: S.() -> S)  // 状态更新
    protected fun sendEvent(event: E)              // 事件发送
    protected suspend fun safeExecute(...)         // 安全异步执行 + 自动错误处理
}
```

---

### 5. WorkManager 后台任务调度

本项目使用 **3 个 Worker** 执行周期性后台任务：

| Worker | 周期 | 网络要求 | 功能 |
|--------|------|---------|------|
| `AssetSnapshotWorker` | 15 分钟 | 不需要 | 保存资产快照到 `asset_snapshots` 表，清理 30 天前旧数据 |
| `StockPriceSyncWorker` | 15 分钟 | `NetworkType.CONNECTED` | 从新浪财经 API 批量同步股价，更新 `investments` 和 `stock_holdings` 表 |
| `DataSyncWorker` | 按需 | 需要 | 与后端服务器同步数据 |

**Worker 注册**（`MainActivity.setupWorkers()`）：
```kotlin
PeriodicWorkRequestBuilder<AssetSnapshotWorker>(15, TimeUnit.MINUTES)
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
    .build()

workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
```

**Hilt 集成**：Worker 使用 `@HiltWorker` + `@AssistedInject` 注解，通过 `HiltWorkerFactory` 自动注入 DAO 和 API Service 依赖。

---

### 6. MVI 状态管理模式

每个功能模块遵循 **Contract + ViewModel + Fragment** 的 MVI 三件套：

- **Contract 文件**：定义 `UiState`（data class）和 `UiEvent`（sealed class）
  - `UiState`：实现 `ErrorState` 接口，包含所有页面需要的数据字段
  - `UiEvent`：定义一次性事件（如 `ShowMessage`、`NavigateTo`）
- **ViewModel**：继承 `BaseViewModel<State, Event>`，通过 `updateState {}` 更新状态，`sendEvent()` 发送事件
- **Fragment**：收集 `uiState: StateFlow` 驱动 UI 渲染，收集 `uiEvent: SharedFlow` 处理一次性操作

**错误处理**：`BaseViewModel.safeExecute()` 封装 try-catch，自动更新 `errorMessage` 状态并发送错误事件。`copyWithError()` 扩展函数通过反射调用 data class 的 `copy()` 方法更新错误字段。

---

### 7. 安全存储 (EncryptedSharedPreferences)

`UserPreferencesManager` 实现分级存储策略：

| 数据类型 | 存储方式 | 说明 |
|---------|---------|------|
| DeepSeek API Key | `EncryptedSharedPreferences`（AES256-GCM） | 基于 Android Keystore，即使 root 也无法读取明文 |
| UI 偏好/分析缓存 | 普通 `SharedPreferences` | 避免不必要的加解密开销 |

**降级策略**：如果 `EncryptedSharedPreferences` 初始化失败（低版本设备），自动降级到普通 `SharedPreferences` 并输出日志警告。

**旧版迁移**：首次调用 `getDeepSeekApiKey()` 时检查普通 SP 中是否存在历史 Key，若存在则自动迁移到加密存储并删除旧数据。

---

### 8. Markdown 渲染

`MarkdownRenderer` 基于 **Markwon 4.6.2** 库实现：

```kotlin
Markwon.builder(context)
    .usePlugin(StrikethroughPlugin.create())  // 删除线支持
    .usePlugin(TablePlugin.create(context))   // 表格支持
    .build()
```

- **双线程安全单例**：`@Volatile` + `synchronized` 确保 Markwon 实例线程安全
- **使用场景**：AI 自然语言问答模块的 `ChatMessageAdapter`，将 AI 回复的 Markdown 渲染到 `TextView`
- **支持格式**：加粗、斜体、删除线、有序/无序列表、代码块、表格、标题、链接

---

### 9. MPAndroidChart 图表可视化

统计模块使用 **MPAndroidChart v3.1.0** 的 `PieChart` 组件展示分类占比饼图：

- 数据源：`CategoryStat` 列表（类别名、金额、百分比、类型）
- 渲染：每个分类对应一个饼图扇区，颜色自动分配，显示百分比标签
- 交互：支持点击扇区查看详情

---

### 10. 网络状态监控

`NetworkMonitor` 基于 `ConnectivityManager` 实现：

- **即时查询**：`isNetworkAvailable()` 检查当前网络（WiFi/蜂窝/以太网）
- **响应式监听**：`observeNetworkStatus(): Flow<NetworkStatus>` 使用 `callbackFlow` 将 `NetworkCallback` 转换为 Flow
- **状态枚举**：`AVAILABLE`/`UNAVAILABLE`/`LOSING`/`LOST`
- **使用场景**：
  - `TransactionRepositoryImpl` 在写操作后调用 `trySync()` 检查网络状态
  - `SyncManager` 监听网络恢复时自动触发数据同步

---

## 📁 项目结构

```
app/src/main/java/com/example/funnyexpensetracking/
├── FunnyExpenseApp.kt                # @HiltAndroidApp 入口 + WorkManager 配置
├── MainActivity.kt                   # 单 Activity + Fragment 切换 + Worker 注册
├── di/                               # Hilt 依赖注入模块
│   ├── AppModule.kt                 # 应用级依赖
│   ├── DatabaseModule.kt           # Room 数据库 + Migration(3→7)
│   ├── NetworkModule.kt            # 5 个 Retrofit 实例 + OkHttp 拦截器
│   └── RepositoryModule.kt         # 8 个 Repository 接口绑定
├── data/                             # 数据层
│   ├── local/
│   │   ├── AppDatabase.kt          # Room 数据库定义（8 张表, v7）
│   │   ├── Converters.kt           # Room 类型转换器
│   │   ├── UserPreferencesManager.kt # 偏好管理（加密 + 普通双存储）
│   │   ├── dao/                     # 8 个 DAO（Flow 响应式查询）
│   │   └── entity/                  # 8 个 Entity（Room @Entity）
│   ├── remote/
│   │   ├── api/                     # Retrofit API 接口
│   │   │   ├── DeepSeekApiService.kt       # DeepSeek Chat Completion
│   │   │   ├── SinaFinanceApiService.kt    # 新浪财经行情（纯文本）
│   │   │   ├── YahooFinanceApiService.kt   # Yahoo 兼容（备用）
│   │   │   ├── ExpenseApiService.kt        # 后端数据同步
│   │   │   └── StatisticsApiService.kt     # 后端统计
│   │   └── dto/                     # 网络传输对象
│   │       ├── OpenAIDto.kt                # Chat Completion 完整 DTO
│   │       ├── YahooFinanceDto.kt          # 新浪行情解析器 + Yahoo 兼容
│   │       └── ...
│   ├── repository/                  # 10 个 Repository 实现
│   │   ├── TransactionRepositoryImpl.kt    # 离线优先 + SyncStatus 状态机
│   │   ├── DeepSeekAnalysisRepositoryImpl.kt # AI 分析 + JSON 模式
│   │   ├── FinancialQueryRepositoryImpl.kt   # 多轮对话 + 财务上下文构建
│   │   ├── InvestmentRepositoryImpl.kt       # 新浪 API 行情获取
│   │   ├── DataManagementRepositoryImpl.kt   # 多表联合导入/导出
│   │   └── ...
│   └── sync/SyncManager.kt         # 云端同步管理器
├── domain/                           # 领域层
│   ├── model/                       # 领域模型（Transaction/FixedIncome/Investment/...）
│   ├── repository/                  # 8 个 Repository 接口
│   └── usecase/
│       ├── RealtimeAssetCalculator.kt      # 实时资产计算（多源监听 + 定时器）
│       ├── transaction/TransactionUseCases.kt
│       ├── ai/AIAnalysisUseCases.kt
│       ├── asset/AssetUseCases.kt
│       ├── statistics/StatisticsUseCases.kt
│       └── stock/StockUseCases.kt
├── ui/                               # 表现层
│   ├── common/BaseViewModel.kt      # MVI 基类（StateFlow + SharedFlow）
│   ├── compose/                     # Compose UI 组件
│   ├── transaction/                 # 记账（Fragment/ViewModel/Adapter/BottomSheet）
│   ├── calendar/                    # 日历（CalendarAdapter/DayDetailBottomSheet）
│   ├── fixedincome/                 # 固定收支
│   ├── investment/                  # 投资理财
│   ├── statistics/                  # 统计报表（MPAndroidChart 饼图）
│   ├── aianalysis/                  # AI 分析
│   ├── financialquery/              # AI 问答（ChatMessageAdapter + Markdown）
│   ├── history/                     # 历史账单
│   ├── usercenter/                  # 用户中心（导入/导出/清除/主题/API Key）
│   └── asset/                       # 资产管理
├── worker/                           # WorkManager 后台任务
│   ├── AssetSnapshotWorker.kt       # 每 15 分钟资产快照
│   ├── StockPriceSyncWorker.kt      # 每 15 分钟股价同步
│   └── DataSyncWorker.kt           # 数据同步
└── util/                             # 工具类
    ├── Resource.kt                  # sealed class 网络结果封装
    ├── MarkdownRenderer.kt          # Markwon 单例（删除线 + 表格插件）
    ├── NetworkMonitor.kt            # ConnectivityManager + callbackFlow
    ├── DateTimeUtil.kt              # 日期时间工具
    └── CurrencyUtil.kt             # 货币格式化
```

---

## 🚀 快速入门

### 环境要求

| 项目 | 要求 |
|------|------|
| Android Studio | Ladybug (2024.2) 或更高 |
| JDK | 17 |
| Android SDK | compileSdk 36, minSdk 24 |
| Gradle | 8.10.1（Kotlin DSL）|

### 克隆与构建

```bash
# 1. 克隆仓库
git clone https://github.com/IceFake/FunnyExpenseTracking-Android.git
cd FunnyExpenseTracking-Android

# 2. 在 Android Studio 中打开项目，等待 Gradle 同步完成

# 3. 构建 Debug APK
./gradlew assembleDebug

# 4. 安装到已连接设备
./gradlew installDebug

# 5. 构建 Release APK（需配置签名，见下方说明）
./gradlew assembleRelease
```

**APK 产物路径：**

| 构建类型 | 路径 |
|---------|------|
| Debug | `app/build/outputs/apk/debug/app-debug.apk` |
| Release | `app/build/outputs/apk/release/app-release.apk` |

---

### 配置说明

#### 1. DeepSeek API Key

AI 分析和财务问答功能需要 DeepSeek API Key，在应用运行后于 **用户中心** 页面填写，Key 通过 `EncryptedSharedPreferences`（AES256-GCM）加密存储在设备本地：

1. 访问 [platform.deepseek.com](https://platform.deepseek.com/) 注册并创建 API Key
2. 打开应用 → **用户中心** → **AI 设置** → 填入 API Key
3. 保存后即可使用 AI 智能分析和自然语言问答功能

#### 2. 后端同步服务（可选）

数据云同步功能需要配置后端服务器地址。在 `di/NetworkModule.kt` 中修改：

```kotlin
private const val BASE_URL = "https://your-backend-server.com/api/"
```

如不配置后端，应用将以 **纯离线模式** 运行，所有功能（除数据同步外）均可正常使用。

#### 3. Release 签名配置

Release 构建需要提供签名密钥文件。可通过环境变量配置，或在本地将密钥文件放置于 `app/release-key.jks`：

| 环境变量 | 说明 |
|---------|------|
| `KEYSTORE_BASE64` | Base64 编码的 .jks 密钥文件（CI 使用） |
| `KEYSTORE_PASSWORD` | Keystore 密码 |
| `KEY_ALIAS` | Key 别名 |
| `KEY_PASSWORD` | Key 密码 |

---

### 常用 Gradle 命令

```bash
./gradlew assembleDebug          # 构建 Debug APK
./gradlew assembleRelease        # 构建 Release APK（需签名配置）
./gradlew installDebug           # 安装 Debug APK 到设备
./gradlew test                   # 运行单元测试
./gradlew connectedAndroidTest   # 运行仪器测试（需连接设备/模拟器）
./gradlew lint                   # 代码质量检查
./gradlew clean                  # 清理构建产物
```

---

## 🤖 CI/CD

项目使用 **GitHub Actions** 实现自动化构建和发布，配置文件位于 `.github/workflows/release.yml`。

### 触发方式

| 触发条件 | 说明 |
|---------|------|
| 推送 `v*` 格式的 Tag（如 `v1.0.0`）| 自动构建 Release APK 并发布到 GitHub Releases |
| 手动触发（Workflow Dispatch）| 输入版本号后手动执行构建和发布 |

### 构建流程

1. 检出代码（`actions/checkout@v4`）
2. 配置 JDK 17（Temurin 发行版，启用 Gradle 缓存）
3. 从 Secrets 解码并写入 Keystore 文件
4. 执行 `./gradlew assembleRelease` 构建签名 APK
5. 重命名 APK 为 `FunnyExpenseTracking-v{版本号}.apk`
6. 上传 APK 作为 Workflow Artifact
7. 创建 GitHub Release 并附加 APK

### 所需 Secrets

在仓库的 **Settings → Secrets and variables → Actions** 中配置：

| Secret 名称 | 说明 |
|------------|------|
| `KEYSTORE_BASE64` | Base64 编码的签名密钥文件 |
| `KEYSTORE_PASSWORD` | Keystore 密码 |
| `KEY_ALIAS` | Key 别名 |
| `KEY_PASSWORD` | Key 密码 |

---

## 📄 许可证

本项目基于 [MIT License](LICENSE) 开源。

```
MIT License

Copyright (c) 2026 FunnyExpenseTracking Team
```
