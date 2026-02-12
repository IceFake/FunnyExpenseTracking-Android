# 💰 FunnyExpenseTracking (趣味记账) - 智能个人收支管理应用

<p align="center">
  <img src="app/src/main/res/mipmap-hdpi/ic_launcher.webp" alt="App Icon" width="120"/>
</p>

<p align="center">
  <strong>基于 MVVM + Clean Architecture 架构的现代化 Android 记账应用</strong><br>
  <em>不仅仅是记账，更是您的智能财务管家</em>
</p>

<p align="center">
  <a href="#✨-核心功能">核心功能</a> •
  <a href="#🚀-快速入门">快速入门</a> •
  <a href="#📱-使用指南">使用指南</a> •
  <a href="#🏗️-技术架构">技术架构</a> •
  <a href="#🛠️-开发指南">开发指南</a> •
  <a href="#🤝-贡献指南">贡献指南</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Android-API24+-3DDC84?style=for-the-badge&logo=android" alt="Android">
  <img src="https://img.shields.io/badge/Architecture-MVVM%2BClean-009688?style=for-the-badge" alt="Architecture">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License">
</p>

---

## ✨ 核心功能

### 📝 智能记账模块
- ✅ **多账户管理**：支持现金、银行卡、支付宝、微信支付等多种账户类型
- ✅ **智能分类**：预置餐饮、交通、购物、娱乐、医疗、教育等10+支出类别，7+收入类别
- ✅ **快捷记账**：通过底部弹窗快速添加收支记录，支持备注输入
- 🔄 **语音输入**：语音转文字快速录入备注（开发中）
- 📅 **批量操作**：批量编辑、删除交易记录（规划中）
- ✅ **实时同步**：离线优先策略，网络恢复后自动同步数据

### 📅 日历视图
- ✅ **可视化日历**：按月展示每日收支情况，颜色标记收支额度
- ✅ **快速筛选**：点击任意日期查看当日详细交易记录
- ✅ **日期导航**：支持月份切换，快速跳转到指定日期
- ✅ **日统计**：每日收入、支出总额自动计算展示

### 💼 固定收支管理
- ✅ **定期收支配置**：工资、房租、水电费、订阅服务等固定收支项管理
- ✅ **多频率支持**：每日、每周、每月、每年等多种频率设置
- ✅ **自动累计**：根据设置频率自动计算累计金额
- 🔄 **实时提醒**：即将到期的固定收支提醒（开发中）

### 💹 投资理财追踪
- ✅ **投资组合管理**：股票、基金、理财产品等投资记录管理
- ✅ **实时行情**：集成Yahoo Finance API，实时获取股票价格
- ✅ **收益计算**：自动计算投资收益、收益率、持仓市值
- 🔄 **风险提示**：投资波动提醒，帮助用户把握投资时机（开发中）

### 📊 实时资产计算
- ✅ **分钟级精度**：每分钟计算一次资产变动，精确到分
- ✅ **复合资产**：总资产 = 账户余额 + 固定收支累计 + 投资市值
- ✅ **趋势分析**：资产变化趋势图表，可视化展示资产增长
- ✅ **历史快照**：后台每15分钟自动保存资产快照，支持历史回顾

### 🤖 AI 智能分析
- 🔄 **消费习惯分析**：基于OpenAI API分析消费模式，识别不合理支出（开发中）
- 🔄 **智能建议**：提供个性化理财建议，优化消费结构（开发中）
- 📅 **预测功能**：基于历史数据预测未来收支趋势（规划中）
- 📅 **自然语言交互**：支持自然语言查询财务状况（规划中）

### 📈 统计报表
- ✅ **多维度统计**：按类别、时间、账户等多维度统计分析
- ✅ **可视化图表**：饼图展示收支分类分布
- 🔄 **趋势分析**：月度、年度收支趋势图表（开发中）
- 📅 **导出功能**：支持报表导出为Excel/PDF格式（规划中）

### 🔄 数据管理
- ✅ **本地备份**：一键备份所有数据到本地JSON文件
- 🔄 **云端同步**：支持多设备数据同步（需配置后端服务器，开发中）
- ✅ **数据恢复**：从备份文件恢复数据，防止数据丢失
- ✅ **导入导出**：支持JSON格式数据导入导出

---

## 🚀 快速入门

### 环境要求
- **开发工具**：Android Studio Ladybug (2024.2) 或更高版本
- **JDK版本**：JDK 11 或更高版本
- **Android SDK**：API 36 (Android 14)
- **设备要求**：Android 7.0 (API 24) 及以上版本

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
- **添加账户**：进入「用户中心」→「账户管理」→点击「+」添加新账户
- **编辑账户**：进入账户管理页面，点击账户卡片进行编辑
- **设置默认账户**：在账户编辑页面勾选「默认账户」，记账时自动选中
- **账户排序**：长按账户卡片拖动调整显示顺序

### 2. 日常记账
- **快速记账**：在首页点击右下角「+」按钮，弹出记账窗口
- **选择分类**：从预设的10+支出类别或7+收入类别中选择
- **选择账户**：从已添加的账户中选择交易账户
- **添加备注**：输入交易备注信息（可选）
- **设置日期**：支持选择过去或未来的交易日期

### 3. 固定收支管理
- **添加快捷方式**：进入「固定收支」模块，点击「添加」按钮
- **设置频率**：选择每日、每周、每月或每年等频率
- **配置金额**：输入固定收入或支出金额
- **查看累计**：系统自动计算并显示累计金额
- **编辑删除**：长按固定收支项进行编辑或删除

### 4. 投资管理
- **添加投资**：进入「投资理财」模块，添加股票、基金等投资产品
- **设置持仓**：输入持仓数量、买入价格
- **实时行情**：系统自动从Yahoo Finance获取最新价格
- **查看收益**：实时计算持仓市值、浮动盈亏、收益率
- **更新持仓**：支持修改持仓数量或买入价格

### 5. 数据查看与分析
- **日历视图**：进入「日历」模块，按月查看每日收支情况
- **日详情查看**：点击日历中的日期，查看当日详细交易记录
- **统计报表**：进入「统计」模块，查看收支分类饼图
- **资产总览**：在首页查看实时资产总额、今日收支情况
- **历史记录**：查看所有交易记录，支持按时间筛选

### 6. 数据备份与同步
- **手动备份**：进入「用户中心」→「数据管理」→「导出数据」
- **备份文件**：选择保存位置，系统生成JSON格式备份文件
- **数据恢复**：进入「数据管理」→「导入数据」，选择备份文件恢复
- **数据清理**：支持清空所有数据（谨慎操作）
- **云端同步**：需配置后端服务器地址（开发中）

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
| **UI框架** | Jetpack Compose (规划中) + ViewBinding | Material Design 3 |
| **架构模式** | MVVM + Clean Architecture + MVI | 状态驱动UI |
| **本地存储** | Room Database | 2.6.1 |
| **网络请求** | Retrofit + OkHttp + Gson | 支持拦截器、日志 |
| **异步处理** | Kotlin Coroutines + Flow | 协程 + 数据流 |
| **依赖注入** | Hilt | 2.54 |
| **后台任务** | WorkManager | 2.10.0 |
| **图表库** | MPAndroidChart | v3.1.0 |
| **构建工具** | Gradle Kotlin DSL | 8.13.2 |

### 项目结构
```
app/src/main/java/com/example/funnyexpensetracking/
├── di/                             # 依赖注入模块
│   ├── AppModule.kt               # 应用级依赖
│   ├── DatabaseModule.kt          # 数据库配置
│   ├── NetworkModule.kt           # 网络配置
│   └── RepositoryModule.kt        # Repository绑定
├── data/                          # 数据层
│   ├── local/                     # 本地数据源
│   │   ├── dao/                   # Room数据访问对象
│   │   ├── entity/                # 数据库实体
│   │   └── AppDatabase.kt         # Room数据库
│   ├── remote/                    # 远程数据源
│   │   ├── api/                   # Retrofit API接口
│   │   └── dto/                   # 网络传输对象
│   └── repository/                # Repository实现
├── domain/                        # 领域层
│   ├── model/                     # 领域模型
│   ├── repository/                # Repository接口
│   └── usecase/                   # 业务用例
├── ui/                            # 表现层
│   ├── common/                    # 通用组件
│   │   └── BaseViewModel.kt       # 基础ViewModel
│   ├── transaction/               # 记账模块
│   ├── calendar/                  # 日历模块
│   ├── fixedincome/               # 固定收支模块
│   ├── investment/                # 投资理财模块
│   ├── statistics/                # 统计模块
│   ├── aianalysis/                # AI分析模块
│   ├── history/                   # 历史记录模块
│   ├── usercenter/                # 用户中心模块
│   └── asset/                     # 资产管理模块
├── worker/                        # 后台任务
│   ├── AssetSnapshotWorker.kt     # 资产快照任务
│   └── StockPriceSyncWorker.kt    # 股票价格同步
└── util/                          # 工具类
    ├── Resource.kt                # 网络结果封装
    ├── DateTimeUtil.kt            # 日期时间工具
    └── CurrencyUtil.kt            # 货币格式化
```

---

## ⚙️ 配置说明

### 基础配置
1. **网络配置**：修改 `NetworkModule.kt` 中的 `BASE_URL`
   ```kotlin
   private const val BASE_URL = "https://your-backend-server.com/api/"
   ```

2. **API密钥配置**：在项目根目录创建 `local.properties`
   ```properties
   # OpenAI API密钥（AI分析功能）
   OPENAI_API_KEY=your_openai_api_key_here
   
   # 股票API配置
   YAHOO_FINANCE_API_KEY=optional_api_key
   ```

3. **数据库配置**：Room数据库配置在 `DatabaseModule.kt`
   - 数据库名称：`funny_expense.db`
   - 版本管理：自动迁移支持

### 高级配置
1. **后台任务配置**：
   - 资产快照频率：每15分钟（可配置）
   - 股票同步频率：每15分钟（交易时段）
   - 数据备份频率：每天凌晨2点

2. **主题定制**：
   - 支持深色/浅色主题切换
   - 可自定义主色调
   - 字体大小调节

3. **通知设置**：
   - 固定收支提醒
   - 投资价格提醒
   - 月度报告提醒

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
- [🤖 AI分析功能规划](docs/AI_ANALYSIS_TODO.md) - AI模块开发路线
- [💼 固定收支逻辑说明](docs/FIXED_EXPENSE_LOGIC.md) - 固定收支计算逻辑
- [👨‍💻 开发者指南](AGENTS.md) - 面向开发者的详细指南

---

## 📋 开发路线图

### ✅ 已完成
- [x] 基础记账功能（收支记录、分类管理）
- [x] 多账户支持与管理
- [x] 日历视图与日详情查看
- [x] 固定收支管理与累计计算
- [x] 投资理财模块与实时行情
- [x] 实时资产计算与分钟级更新
- [x] 数据本地存储（Room数据库）
- [x] 后台定时任务（资产快照、价格同步）
- [x] 统计图表（收支分类饼图）
- [x] 数据备份恢复（JSON格式）

### 🚧 进行中
- [ ] AI智能分析功能开发
- [ ] 数据云同步功能集成
- [ ] 深色模式主题适配
- [ ] 趋势分析图表开发
- [ ] 投资风险评估功能

### 📅 规划中
- [ ] 多语言支持（中英文）
- [ ] 数据导入/导出（CSV/Excel格式）
- [ ] 账单提醒与通知功能
- [ ] 预算管理与超支预警
- [ ] 家庭账户共享与协同记账
- [ ] 微信/支付宝账单自动导入
- [ ] 语音输入与智能识别
- [ ] 收据拍照OCR识别

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
