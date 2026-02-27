# 💰 FunnyExpenseTracking (趣味记账) - 智能个人收支管理应用

<p align="center">
  <img src="app/src/main/res/mipmap-hdpi/ic_launcher.webp" alt="App Icon" width="120"/>
</p>

<p align="center">
  <strong>基于 MVVM + Clean Architecture 架构的现代化 Android 记账应用</strong><br>
  <em>不仅仅是记账，更是您的智能财务管家</em>
</p>

<p align="center">
  <a href="#-核心功能">核心功能</a> •
  <a href="#-快速入门">快速入门</a> •
  <a href="#-使用指南">使用指南</a> •
  <a href="#-技术架构">技术架构</a> •
  <a href="#-开发指南">开发指南</a> •
  <a href="#-贡献指南">贡献指南</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Android-API24+-3DDC84?style=for-the-badge&logo=android" alt="Android">
  <img src="https://img.shields.io/badge/Gradle-8.13.2-02303A?style=for-the-badge&logo=gradle" alt="Gradle">
  <img src="https://img.shields.io/badge/Architecture-MVVM%2BClean-009688?style=for-the-badge" alt="Architecture">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License">
</p>

---

## ✨ 核心功能

### 📝 智能记账模块
- ✅ **多账户管理**：支持现金、银行卡、支付宝、微信支付等多种账户类型，可添加、编辑、设置默认账户
- ✅ **智能分类**：预置餐饮、交通、购物、娱乐、医疗、教育、居住、通讯、服饰等 10 种支出类别，工资、奖金、投资收益、兼职、红包、退款等 7 种收入类别
- ✅ **快捷记账**：通过底部弹窗快速添加收支记录，支持备注输入与日期选择
- ✅ **编辑与删除**：支持对已有交易记录进行编辑和删除操作
- ✅ **离线优先**：基于 Room 本地数据库存储，离线可用，带同步状态追踪（SyncStatus）
- 📅 **语音输入**：语音转文字快速录入备注（规划中）
- 📅 **批量操作**：批量编辑、删除交易记录（规划中）

### 📅 日历视图
- ✅ **可视化日历**：按月展示每日收支情况，颜色标记收支额度
- ✅ **快速筛选**：点击任意日期弹出底部弹窗查看当日详细交易记录
- ✅ **日期导航**：支持月份前后切换，快速跳转到指定日期
- ✅ **月统计**：自动计算并展示本月总收入、总支出、月结余

### 💼 固定收支管理
- ✅ **定期收支配置**：工资、房租、水电费、订阅服务等固定收支项管理
- ✅ **多频率支持**：每日（DAILY）、每周（WEEKLY）、每月（MONTHLY）、每年（YEARLY）四种频率
- ✅ **累计时间计算**：基于累计生效分钟数，按完整周期优先 + 剩余时间按比例计算累计金额
- ✅ **结束日期**：支持设置结束日期，到期后自动停止累计
- ✅ **启用/停用**：支持手动停用和启用固定收支条目
- ✅ **分钟级速率展示**：展示每分钟总收入、总支出、净变动速率
- ✅ **筛选功能**：支持按全部 / 仅收入 / 仅支出筛选

### 💹 投资理财追踪
- ✅ **多品类投资管理**：支持股票（STOCK）及其他投资品类的记录
- ✅ **持仓管理**：记录持仓数量、买入价格、投入金额
- ✅ **实时行情同步**：集成 Yahoo Finance API，后台 WorkManager 定时同步股票最新价格
- ✅ **收益计算**：自动计算持仓市值、浮动盈亏、收益率
- ✅ **智能合并**：相同描述/代码的投资条目自动合并显示
- ✅ **筛选功能**：支持按全部 / 股票 / 其他分类筛选
- ✅ **编辑与删除**：支持修改持仓信息或删除投资条目

### 📊 实时资产计算
- ✅ **分钟级精度**：每分钟重新计算一次资产变动
- ✅ **复合资产公式**：总资产 = 各账户余额之和 + 固定收支累计净额 + 投资/理财当前市值
- ✅ **实时数据流**：通过 Kotlin StateFlow 实时推送资产数据到 UI
- ✅ **多源监听**：自动监听账户余额、固定收支、投资市值三大数据源变化并触发重算
- ✅ **历史快照**：后台 WorkManager 每 15 分钟自动保存资产快照，支持历史回顾

### 🤖 AI 智能分析（DeepSeek API）
- ✅ **消费习惯分析**：基于 DeepSeek API（兼容 OpenAI 格式）分析消费模式，识别高频消费类别与消费偏好
- ✅ **智能建议**：提供个性化省钱建议和理财优化建议
- ✅ **趋势预测**：基于历史数据预测下月收支情况
- ✅ **分析结果缓存**：分析结果本地持久化保存，下次打开可直接查看上次结果
- ✅ **历史分析记录**：支持查看历史分析记录及详情
- ✅ **API Key 管理**：应用内设置 DeepSeek API Key，支持动态配置
- 📅 **自然语言交互**：支持自然语言查询财务状况（规划中）

### 📈 统计报表
- ✅ **月度统计**：按月查看总收入、总支出、结余及分类占比
- ✅ **年度统计**：按年查看全年收支概览
- ✅ **分类饼图**：通过 MPAndroidChart 饼图可视化展示收支分类分布
- ✅ **分类明细**：列表展示各分类金额及占比
- ✅ **月份/年份切换**：支持在月视图与年视图之间切换浏览
- 📅 **趋势折线图**：月度、年度收支趋势图表（规划中）
- 📅 **导出功能**：支持报表导出为 Excel/PDF 格式（规划中）

### 📜 历史账单
- ✅ **日账单查看**：查看指定日期的所有交易记录明细
- ✅ **日收支统计**：展示当日收入、支出、结余
- ✅ **记录删除**：支持删除历史交易记录

### 👤 用户中心与数据管理
- ✅ **数据导出**：一键导出所有数据为 JSON 格式文件
- ✅ **数据导入**：从 JSON 备份文件恢复全部数据
- ✅ **数据清除**：支持清空所有数据（谨慎操作）
- ✅ **完整备份**：备份内容涵盖交易记录、账户、固定收支、投资、资产快照等全部数据
- 🔄 **云端同步**：多设备数据同步（需配置后端服务器，架构已搭建）

---

## 🚀 快速入门

### 环境要求
- **开发工具**：Android Studio Ladybug (2024.2) 或更高版本
- **JDK 版本**：JDK 11 或更高版本
- **Android SDK**：compileSdk 36
- **最低系统**：Android 7.0（API 24）及以上
- **Gradle**：8.13.2（Kotlin DSL）

### 获取项目
```bash
# 克隆项目到本地
git clone https://github.com/yourusername/FunnyExpenseTracking-Android.git
cd FunnyExpenseTracking-Android

# 同步依赖（可选）
./gradlew build
```

### 构建与安装
```bash
# 方式1：直接在Android Studio中运行
# 打开项目 -> 点击运行按钮

# 方式2：使用Gradle命令
# 构建Debug版本APK
./gradlew assembleDebug

# 安装到已连接的设备/模拟器
./gradlew installDebug

# 构建Release版本APK（需要签名配置）
./gradlew assembleRelease
```

### 首次运行配置
1. 首次启动应用，系统会引导您创建第一个账户
2. 建议先添加常用账户（如现金、银行卡等）
3. 配置固定收支项（如每月工资、房租等）
4. 开始记录日常收支

---

## 📱 使用指南

### 1. 账户管理
- **添加账户**：在记账首页通过「添加账户」对话框创建新账户
- **编辑账户**：通过底部弹窗编辑账户名称、余额等信息
- **设置默认账户**：记账时自动选中默认账户

### 2. 日常记账
- **快速记账**：在首页点击右下角「+」按钮，弹出底部弹窗（AddTransactionBottomSheet）
- **选择类型**：切换收入 / 支出类型
- **选择分类**：从 10 种支出类别或 7 种收入类别中选择
- **选择账户**：从已添加的账户列表中选择
- **添加备注**：输入交易备注信息（可选）
- **设置日期**：支持选择过去或未来的交易日期
- **编辑/删除**：对已有交易记录进行修改或删除

### 3. 固定收支管理
- **添加固定收支**：通过底部弹窗（AddFixedIncomeBottomSheet）添加固定收支条目
- **设置频率**：选择每日、每周、每月或每年
- **设置日期范围**：配置开始日期和可选的结束日期
- **查看速率**：展示每分钟收入/支出速率和累计金额
- **筛选查看**：按全部 / 仅收入 / 仅支出筛选固定收支列表
- **启用/停用**：手动停用或重新启用固定收支条目

### 4. 投资管理
- **添加投资**：通过底部弹窗（AddInvestmentBottomSheet）录入投资产品
- **设置持仓**：输入持仓数量、买入价格、投入金额
- **实时行情**：系统通过 Yahoo Finance API 后台自动同步股票最新价格
- **查看收益**：实时计算持仓市值、浮动盈亏、收益率
- **分类筛选**：按全部 / 股票 / 其他筛选投资列表
- **编辑/删除**：修改持仓信息或删除投资条目

### 5. 数据查看与分析
- **日历视图**：进入「日历」模块，按月查看每日收支情况，点击日期弹出底部弹窗查看日明细
- **统计报表**：进入「统计」模块，通过饼图查看分类占比，支持月度/年度切换
- **资产总览**：在首页查看实时总资产、今日收支、每分钟收支变动
- **AI 分析**：进入「AI 分析」模块，一键调用 DeepSeek API 获取消费洞察与理财建议
- **历史账单**：查看指定日期的所有交易记录

### 6. 数据备份与恢复
- **导出数据**：进入「用户中心」→ 点击「导出数据」，生成 JSON 格式备份
- **导入数据**：进入「用户中心」→ 点击「导入数据」，选择 JSON 备份文件恢复
- **清除数据**：在用户中心点击「清除数据」清空所有本地数据（谨慎操作）

---

## 🏗️ 技术架构

### 架构概览
本项目采用 **MVVM + Clean Architecture** 三层架构，确保代码的可测试性、可维护性和可扩展性。

```
┌─────────────────────────────────────────────────────────────┐
│                         UI 层                               │
│     Fragment + ViewModel + MVI State/Event Pattern          │
├─────────────────────────────────────────────────────────────┤
│                       Domain 层                             │
│              UseCase + Repository Interface + Domain Model  │
├─────────────────────────────────────────────────────────────┤
│                        Data 层                              │
│         Repository实现 + Room DAO + Retrofit API            │
└─────────────────────────────────────────────────────────────┘
```

### 技术栈详情
| 技术领域 | 具体技术 | 版本/说明 |
|---------|---------|-----------|
| **开发语言** | Kotlin | 2.0.21 |
| **UI 框架** | ViewBinding + Material Design 3 | Material 1.13.0 |
| **架构模式** | MVVM + Clean Architecture + MVI | 状态驱动 UI（StateFlow + SharedFlow） |
| **本地存储** | Room Database | 2.6.1 |
| **网络请求** | Retrofit + OkHttp + Gson | Retrofit 2.11.0, OkHttp 4.12.0 |
| **异步处理** | Kotlin Coroutines + Flow | 1.9.0 |
| **依赖注入** | Hilt (Dagger) | 2.54 |
| **后台任务** | WorkManager | 2.10.0 |
| **图表库** | MPAndroidChart | v3.1.0 |
| **导航组件** | Jetpack Navigation | 2.8.5 |
| **AI 分析** | DeepSeek API（OpenAI 兼容格式） | deepseek-chat 模型 |
| **股票行情** | Yahoo Finance API | 实时股价同步 |
| **符号处理** | KSP (Kotlin Symbol Processing) | 2.0.21-1.0.28 |
| **构建工具** | Gradle Kotlin DSL | 8.13.2 |

### 项目结构
```
app/src/main/java/com/example/funnyexpensetracking/
├── FunnyExpenseApp.kt              # Application类（Hilt入口）
├── MainActivity.kt                 # 主Activity（单Activity架构）
├── di/                             # 依赖注入模块
│   ├── AppModule.kt               # 应用级依赖（Gson、UserPreferences等）
│   ├── DatabaseModule.kt          # Room数据库配置
│   ├── NetworkModule.kt           # Retrofit/OkHttp网络配置
│   └── RepositoryModule.kt        # Repository接口绑定
├── data/                           # 数据层
│   ├── local/                     # 本地数据源
│   │   ├── AppDatabase.kt        # Room数据库定义
│   │   ├── Converters.kt         # Room类型转换器
│   │   ├── UserPreferencesManager.kt # SharedPreferences管理（API Key等）
│   │   ├── dao/                   # Room数据访问对象
│   │   │   ├── AccountDao.kt
│   │   │   ├── AssetBaselineDao.kt
│   │   │   ├── AssetSnapshotDao.kt
│   │   │   ├── FixedIncomeDao.kt
│   │   │   ├── InvestmentDao.kt
│   │   │   ├── StockHoldingDao.kt
│   │   │   ├── SyncMetadataDao.kt
│   │   │   └── TransactionDao.kt
│   │   └── entity/                # 数据库实体
│   │       ├── AccountEntity.kt
│   │       ├── AssetBaselineEntity.kt
│   │       ├── AssetSnapshotEntity.kt
│   │       ├── FixedIncomeEntity.kt
│   │       ├── InvestmentEntity.kt
│   │       ├── StockHoldingEntity.kt
│   │       ├── SyncMetadataEntity.kt
│   │       └── TransactionEntity.kt
│   ├── remote/                    # 远程数据源
│   │   ├── api/                   # Retrofit API接口
│   │   │   ├── AIAnalysisApiService.kt
│   │   │   ├── DeepSeekApiService.kt
│   │   │   ├── ExpenseApiService.kt
│   │   │   ├── OpenAIApiService.kt
│   │   │   ├── StatisticsApiService.kt
│   │   │   ├── StockApiService.kt
│   │   │   └── YahooFinanceApiService.kt
│   │   └── dto/                   # 网络传输对象
│   ├── repository/                # Repository实现
│   │   ├── AccountRepositoryImpl.kt
│   │   ├── AIAnalysisRepositoryImpl.kt
│   │   ├── AssetRepositoryImpl.kt
│   │   ├── DataManagementRepositoryImpl.kt
│   │   ├── DeepSeekAnalysisRepositoryImpl.kt
│   │   ├── InvestmentRepositoryImpl.kt
│   │   ├── StatisticsRepositoryImpl.kt
│   │   ├── StockRepositoryImpl.kt
│   │   └── TransactionRepositoryImpl.kt
│   └── sync/                      # 数据同步
│       └── SyncManager.kt
├── domain/                        # 领域层
│   ├── model/                     # 领域模型
│   │   ├── AIAnalysis.kt
│   │   ├── AssetSummary.kt
│   │   ├── BackupData.kt
│   │   ├── FixedIncome.kt
│   │   ├── Investment.kt
│   │   ├── Statistics.kt
│   │   ├── StockHolding.kt
│   │   └── Transaction.kt
│   ├── repository/                # Repository接口
│   └── usecase/                   # 业务用例
│       ├── RealtimeAssetCalculator.kt  # 实时资产计算器
│       ├── ai/
│       │   └── AIAnalysisUseCases.kt
│       ├── asset/
│       ├── statistics/
│       ├── stock/
│       └── transaction/
├── ui/                            # 表现层
│   ├── common/                    # 通用组件
│   │   └── BaseViewModel.kt      # 基础ViewModel（StateFlow + SharedFlow）
│   ├── transaction/               # 记账模块
│   │   ├── TransactionContract.kt
│   │   ├── TransactionViewModel.kt
│   │   ├── TransactionFragment.kt
│   │   ├── DailyTransactionAdapter.kt
│   │   ├── AddTransactionBottomSheet.kt
│   │   ├── AddFixedIncomeBottomSheet.kt
│   │   ├── AddAccountDialog.kt
│   │   └── EditAccountBottomSheet.kt
│   ├── calendar/                  # 日历模块
│   │   ├── CalendarContract.kt
│   │   ├── CalendarViewModel.kt
│   │   ├── CalendarFragment.kt
│   │   ├── CalendarAdapter.kt
│   │   ├── DayDetailBottomSheet.kt
│   │   └── DayDetailViewModel.kt
│   ├── fixedincome/               # 固定收支模块
│   │   ├── FixedIncomeContract.kt
│   │   ├── FixedIncomeViewModel.kt
│   │   ├── FixedIncomeFragment.kt
│   │   └── FixedIncomeAdapter.kt
│   ├── investment/                # 投资理财模块
│   │   ├── InvestmentContract.kt
│   │   ├── InvestmentViewModel.kt
│   │   ├── InvestmentFragment.kt
│   │   ├── InvestmentAdapter.kt
│   │   └── AddInvestmentBottomSheet.kt
│   ├── statistics/                # 统计模块
│   │   ├── StatisticsContract.kt
│   │   ├── StatisticsViewModel.kt
│   │   ├── StatisticsFragment.kt
│   │   └── CategoryStatAdapter.kt
│   ├── aianalysis/                # AI分析模块
│   │   ├── AIAnalysisContract.kt
│   │   ├── AIAnalysisViewModel.kt
│   │   └── AIAnalysisFragment.kt
│   ├── history/                   # 历史账单模块
│   │   ├── HistoryContract.kt
│   │   ├── HistoryViewModel.kt
│   │   ├── HistoryFragment.kt
│   │   └── TransactionAdapter.kt
│   ├── usercenter/                # 用户中心模块
│   │   ├── DataManagementContract.kt
│   │   ├── UserCenterViewModel.kt
│   │   └── UserCenterFragment.kt
│   └── asset/                     # 资产管理模块
│       ├── AssetContract.kt
│       └── AssetViewModel.kt
├── worker/                        # 后台任务
│   ├── AssetSnapshotWorker.kt     # 资产快照定时保存（每15分钟）
│   ├── DataSyncWorker.kt         # 数据同步任务
│   └── StockPriceSyncWorker.kt   # 股票价格同步（每15分钟）
└── util/                          # 工具类
    ├── Resource.kt                # 网络结果封装（Success/Error/Loading）
    ├── DateTimeUtil.kt            # 日期时间工具
    ├── CurrencyUtil.kt            # 货币格式化
    └── NetworkMonitor.kt          # 网络状态监测
```

---

## ⚙️ 配置说明

### 基础配置
1. **网络配置**：修改 `NetworkModule.kt` 中的 `BASE_URL`
   ```kotlin
   private const val BASE_URL = "https://your-backend-server.com/api/"
   ```

2. **DeepSeek API Key 配置**（AI 分析功能）：
   - **方式一**：在应用内「AI 分析」页面点击「API Key 设置」按钮，直接输入 API Key
   - **方式二**：API Key 存储在 `UserPreferencesManager`（SharedPreferences）中，持久化保存
   - 获取 API Key：访问 [DeepSeek 开放平台](https://platform.deepseek.com/) 注册并创建

3. **数据库配置**：Room 数据库配置在 `DatabaseModule.kt`
   - 数据库名称：`funny_expense.db`
   - 支持自动迁移

### 高级配置
1. **后台任务频率**：
   - 资产快照保存：每 15 分钟（AssetSnapshotWorker）
   - 股票价格同步：每 15 分钟（StockPriceSyncWorker）
   - 数据同步：DataSyncWorker（需配置后端服务器）

---

## 🛠️ 开发指南

### 开发环境搭建
```bash
# 1. 克隆项目
git clone https://github.com/yourusername/FunnyExpenseTracking-Android.git

# 2. 打开Android Studio
# 3. 导入项目
# 4. 等待Gradle同步完成
# 5. 运行测试确保环境正常
./gradlew test
```

### 代码规范
- **命名规范**：遵循Kotlin官方命名约定
- **架构规范**：严格遵守Clean Architecture分层原则
- **注释要求**：公共API必须添加KDoc注释
- **测试要求**：新增功能必须包含单元测试

### 常用开发命令
```bash
# 运行所有测试
./gradlew test

# 运行单个测试类
./gradlew test --tests "*TransactionRepositoryTest"

# 运行Android测试
./gradlew connectedAndroidTest

# 代码质量检查
./gradlew lint

# 生成APK
./gradlew assembleDebug
./gradlew assembleRelease

# 清理构建
./gradlew clean
```

### 添加新功能流程
1. **分析需求**：明确功能边界和交互流程
2. **设计接口**：在domain层定义Repository接口
3. **实现数据层**：实现Repository接口和对应的DAO/DTO
4. **编写用例**：在domain/usecase中添加业务逻辑
5. **实现UI层**：创建Contract、ViewModel、Fragment
6. **编写测试**：为新增功能添加单元测试
7. **代码审查**：提交PR进行代码审查

---

## 🤝 贡献指南

### 如何贡献
我们欢迎所有形式的贡献，包括但不限于：
- 🐛 **问题反馈**：提交Bug报告或使用问题
- 💡 **功能建议**：提出新功能或改进建议
- 📝 **文档完善**：改进文档、翻译、示例
- 🔧 **代码贡献**：修复Bug、实现新功能
- 🎨 **UI/UX改进**：优化界面设计和用户体验

### 贡献流程
1. **Fork仓库**：点击GitHub页面右上角的Fork按钮
2. **创建分支**：基于`main`分支创建功能分支
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **提交更改**：遵循提交消息规范
   ```bash
   git commit -m "feat: add new transaction category"
   ```
4. **推送到分支**：
   ```bash
   git push origin feature/your-feature-name
   ```
5. **创建PR**：在GitHub上创建Pull Request

### 提交消息规范
我们使用约定式提交，消息格式如下：
- `feat:` 新功能
- `fix:` Bug修复
- `docs:` 文档更新
- `style:` 代码格式调整（不影响功能）
- `refactor:` 代码重构
- `test:` 测试相关
- `chore:` 构建过程或辅助工具变动

示例：
```
feat(transaction): add category filtering feature
fix(calendar): resolve date display issue on February 29th
docs(readme): update installation instructions
```

---

## 📄 相关文档

- [📖 详细架构文档](ARCHITECTURE.md) - 深入了解项目架构设计
- [🏗️ 项目架构详解](PROJECT_ARCHITECTURE.md) - 模块依赖和设计决策
- [🤖 AI 分析功能实现指南](docs/AI_ANALYSIS_IMPLEMENTATION.md) - AI 模块完整实现方案
- [🤖 AI 分析功能规划](docs/AI_ANALYSIS_TODO.md) - AI 模块开发路线
- [🔗 DeepSeek API 接入教程](docs/DEEPSEEK_API_INTEGRATION_GUIDE.md) - Android 客户端接入 DeepSeek AI 详细教程
- [💼 固定支出逻辑说明](docs/FIXED_EXPENSE_LOGIC.md) - 固定支出计算逻辑（v2.0 累计时间算法）
- [💰 固定收支计算逻辑](docs/FIXED_INCOME_CALCULATION_LOGIC.md) - 固定收支完整计算逻辑文档
- [👨‍💻 开发者指南](AGENTS.md) - 面向开发者和 AI 代理的详细指南

---

## 📋 开发路线图

### ✅ 已完成
- [x] 基础记账功能（收支记录、分类管理、收入/支出 17 种类别）
- [x] 多账户支持与管理（添加、编辑账户）
- [x] 日历视图与日详情弹窗查看
- [x] 固定收支管理（多频率、累计时间计算、启用/停用、结束日期）
- [x] 投资理财模块（多品类、持仓管理、智能合并显示）
- [x] Yahoo Finance API 实时股票行情同步
- [x] 实时资产计算（分钟级精度、多源监听、复合资产公式）
- [x] AI 消费习惯分析（DeepSeek API，消费洞察 + 建议 + 预测）
- [x] AI 分析结果缓存与历史记录
- [x] 应用内 DeepSeek API Key 配置
- [x] 统计图表（月度/年度、饼图分类分布、分类明细列表）
- [x] 历史账单查看与管理
- [x] 数据本地存储（Room 数据库，8 张核心表）
- [x] 后台定时任务（资产快照 15 分钟、股价同步 15 分钟）
- [x] 数据备份恢复（JSON 格式导入/导出/清除）
- [x] 网络状态监测（NetworkMonitor）
- [x] 数据同步状态追踪（SyncStatus、SyncMetadata）
- [x] MVI 架构模式（BaseViewModel + StateFlow + SharedFlow）

### 🚧 进行中
- [ ] 数据云同步功能集成（SyncManager 架构已搭建）
- [ ] 趋势分析图表开发（TrendStatistics 模型已定义）

### 📅 规划中
- [ ] 多语言支持（中英文）
- [ ] 深色模式主题适配
- [ ] 数据导入/导出（CSV/Excel 格式）
- [ ] 账单提醒与通知功能
- [ ] 预算管理与超支预警
- [ ] 家庭账户共享与协同记账
- [ ] 微信/支付宝账单自动导入
- [ ] 语音输入与智能识别
- [ ] 收据拍照 OCR 识别
- [ ] AI 自然语言查询财务状况
- [ ] 投资风险评估与波动提醒
- [ ] Jetpack Compose UI 迁移

---

## 📄 许可证

本项目采用 **MIT 许可证** - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🙏 致谢

感谢所有为这个项目做出贡献的开发者们！
特别感谢开源社区提供的优秀库和工具。

---

<p align="center">
  <strong>Made with ❤️ by FunnyExpenseTracking Team</strong><br>
  <em>让理财变得更简单、更有趣</em>
</p>

<p align="center">
  <sub>如有问题或建议，请提交 <a href="https://github.com/yourusername/FunnyExpenseTracking-Android/issues">Issue</a></sub>
</p>
