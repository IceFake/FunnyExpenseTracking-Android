# 本地 + 云端并行数据获取改造清单

说明：本文件记录当前分支中需要将“仅靠本地数据库获取数据”改为“本地与云端同时进行数据获取（并行获取/合并）”的所有 repository 接口、对应实现类位置、建议的签名改动与迁移策略。文档以中文编写，便于工程内团队使用。

---

## 总体 Checklist

- [ ] 确认并记录所有需要改造的 `domain/repository` 接口与方法
- [ ] 为每个接口提供非破坏性与破坏性两种改造方案（优先推荐非破坏性）
- [ ] 在 `data/repository` 中实现并行拉取远端数据并写回本地的逻辑（保留离线优先策略）
- [ ] 更新或新增用于暴露“本地数据 + 远端刷新状态”的 API（例如 `Flow<Resource<T>>` 或 `observeXxxWithRemote()`）
- [ ] 扫描并列出使用这些接口的 `ViewModel` / `usecase` / `Fragment`，评估调用点并逐步替换为新接口
- [ ] 编写/更新单元测试与集成测试，确保数据合并行为、冲突解决和网络中断场景正确
- [ ] 运行 `./gradlew clean build` 验证编译通过（建议在 CI 上验证）

---

## 需要改造的 repository 接口（汇总）

下面按优先级列出需要改造的接口。优先级基于使用频率和对 UI 实时性影响：交易 > 账户 > 资产/固定收支 > 投资/持仓。

1. `TransactionRepository` — `app/src/main/java/.../domain/repository/TransactionRepository.kt`
   - 主要方法（当前均为本地 Flow 或本地查询）:
     - `fun getAllTransactions(): Flow<List<Transaction>>`
     - `fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>`
     - `fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>`
     - `fun getTransactionsByCategory(category: String): Flow<List<Transaction>>`
     - `suspend fun getTransactionById(id: Long): Transaction?`
   - 实现类：`app/src/main/java/.../data/repository/TransactionRepositoryImpl.kt`
   - 建议：
     - 非破坏性：新增 `fun observeAllTransactionsWithRemote(): Flow<Resource<List<Transaction>>>` 或 `fun refreshAndObserveTransactions(): Flow<Resource<List<Transaction>>>`。实现：订阅本地 Room `Flow` 同时异步触发远端拉取并将结果合并写回本地，返回 Resource 包含本地缓存与远端刷新状态。
     - 破坏性：把原有 `getAllTransactions()` 改为 `Flow<Resource<List<Transaction>>>`（会需要修改大量调用方）。

2. `AccountRepository` — `app/src/main/java/.../domain/repository/AccountRepository.kt`
   - 主要方法：
     - `fun getAllAccounts(): Flow<List<Account>>`
     - `suspend fun getAccountById(id: Long): Account?`
     - `suspend fun getDefaultAccount(): Account?`
     - `fun getTotalBalanceFlow(): Flow<Double>`
   - 实现类：`app/src/main/java/.../data/repository/AccountRepositoryImpl.kt`
   - 建议：同 Transaction，新增 `observeAllAccountsWithRemote()` 或 `getAllAccountsWithRefresh()`，订阅时触发云端刷新并写回本地。

3. `AssetRepository` — `app/src/main/java/.../domain/repository/AssetRepository.kt`
   - 主要方法（固定收支/快照/汇总）：
     - `fun getAllActiveFixedIncomes(): Flow<List<FixedIncome>>`
     - `fun getAllFixedIncomes(): Flow<List<FixedIncome>>`
     - `fun getFixedIncomesByType(type: FixedIncomeType): Flow<List<FixedIncome>>`
     - `suspend fun getAllFixedIncomesList(): List<FixedIncome>`
     - `suspend fun getLatestSnapshot(): AssetSnapshot?`
     - `fun getLatestSnapshotFlow(): Flow<AssetSnapshot?>`
     - `fun getSnapshotsByTimeRange(startTime: Long, endTime: Long): Flow<List<AssetSnapshot>>`
     - `fun getRecentSnapshots(limit: Int): Flow<List<AssetSnapshot>>`
     - `suspend fun calculateCurrentAssetSummary(): AssetSummary`
     - `fun getAssetSummaryFlow(): Flow<AssetSummary>`
   - 实现类：`app/src/main/java/.../data/repository/AssetRepositoryImpl.kt`
   - 建议：固定收支与快照通常以本地为主，但应在订阅时发起远端拉取（例如用户在多设备更新数据时），并暴露 Resource 状态或新增 refresh 方法。

4. `InvestmentRepository` — `app/src/main/java/.../domain/repository/InvestmentRepository.kt`
   - 主要方法：
     - `fun getAllInvestments(): Flow<List<Investment>>`
     - `suspend fun getInvestmentById(id: Long): Investment?`
     - `suspend fun getAllStockCodes(): List<String>`
     - `fun getTotalCurrentValueFlow(): Flow<Double>`
   - 实现类：`app/src/main/java/.../data/repository/InvestmentRepositoryImpl.kt`
   - 建议：订阅投资列表时应并行刷新远端价格/持仓明细并合并结果，提供 `Flow<Resource<...>>` 或单独 `refreshPrices()` 方法。

5. `StockRepository` — `app/src/main/java/.../domain/repository/StockRepository.kt`
   - 主要方法（持仓相关）：
     - `fun getAllStockHoldings(): Flow<List<StockHolding>>`
     - `suspend fun getStockHoldingById(id: Long): StockHolding?`
     - `suspend fun getStockHoldingBySymbol(symbol: String): StockHolding?`
   - 实现类：`app/src/main/java/.../data/repository/StockRepositoryImpl.kt`
   - 说明：行情查询系列方法（`getStockQuote`, `getBatchQuotes`）已为远端 Resource 风格，持仓列表仍需在观察时支持远端合并刷新。

6. 其他可选审查接口
   - `StatisticsRepository`、`AIAnalysisRepository`、`FinancialQueryRepository` 等多数已为远端或 Resource 包装，通常不需要改造。
   - 需要额外扫描项目中其他 `Flow<List<...>>` 或直接读取本地的 repository 接口，确保不会遗漏。

---

## 推荐的 API 头签名示例（非破坏性方案）

- 新增 Resource 类型的 Flow 观察接口（保留老接口以兼容旧代码）

示例（Transaction）：

- 保留：
  - `fun getAllTransactions(): Flow<List<Transaction>>`
- 新增：
  - `fun observeAllTransactionsWithRemote(): Flow<Resource<List<Transaction>>>`
  - `suspend fun refreshTransactionsFromRemote(startDate: Long? = null, endDate: Long? = null): Resource<Unit>`

Resource 为项目已有工具类 `com.example.funnyexpensetracking.util.Resource`。

实现要点：
- `observeAllTransactionsWithRemote()` 实现可返回一个合并流：先发出本地数据（Resource.Success），然后触发远端拉取，发出 Resource.Loading（可选），拉取成功后写入本地并发出更新后的本地数据（或直接发出来自远端的数据），拉取失败则发出 Resource.Error（但不清除本地数据）。
- `refreshTransactionsFromRemote()` 为手动触发远端同步接口（UI 显示“下拉刷新”时可调用）。

---

## 迁移策略（建议分阶段执行）

阶段 1 — 调查与接口兼容层（1-2 天）
- 扫描并列出所有调用上述接口的方法和类（ViewModel、UseCase、Fragment）。
- 在 `data/repository` 中为每个目标仓库新增 `observeXxxWithRemote()` 与 `refreshXxx()`，实现内部：订阅本地 Flow + 并行远端拉取并写回本地。
- 不修改 domain 接口原有签名（可通过在实现类中新增扩展接口并通过 DI 注入需要新接口的实现，或者将实现类新增方法）。

阶段 2 — 逐步替换调用方（1-2 周）
- 在 UI 层（ViewModel）优先使用新 `observeXxxWithRemote()` 替换关键页面（交易页、资产概览、账户页）。
- 为关键路径添加错误/加载状态显示。

阶段 3 — 可选的接口统一（长期）
- 如果团队希望统一语义，可把老接口替换为 `Flow<Resource<T>>`，同时修正所有调用方并移除兼容层。

---

## 合并/冲突处理建议（实现细节）

- 对于远端 Pull->写入本地 时，请遵循现有离线优先策略：
  - 永远不要覆盖本地处于 `SyncStatus.PENDING_UPLOAD`（或类似状态）的记录。
  - 对于没有未同步改动的本地记录，可以采用最后更新时间（`updatedAt`）或 `serverId` 进行合并/替换。
  - 对于删除操作，注意删除标记与服务器端状态的一致性。

- 在实现写回时尽量使用事务（Room）以保证一致性。
- 提供可配置的冲突解析策略：`prefer_local`, `prefer_remote`, `merge` 等，默认使用 `prefer_local`（避免丢失未上传改动）。

---

## 测试与验证

1. 本地单元测试：为 `RepositoryImpl` 增加单元测试（使用 Fake/Mock 的 `ExpenseApiService` 与 `TransactionDao`），覆盖以下场景：
   - 本地有未同步改动时，远端拉取不覆盖本地改动
   - 本地为空但远端有数据时，远端数据写入本地并触发 Flow 更新
   - 网络不可用时，observe 返回本地数据且 `refresh` 返回网络错误但不影响本地

2. 集成测试：在模拟网络条件下运行 UI 层关键屏幕，检查刷新指示器与错误显示。

3. 本地手动验证：
   - 运行应用，打开交易列表页面，断网/联网切换，确认 UI 在联网后从云端拉取并显示更新。

建议在 Windows PowerShell 下运行的命令：

```powershell
# 编译与运行单元测试
.\gradlew clean test ; .\gradlew assembleDebug
```

---

## 下一步（我可以代劳的项）

- [ ] 扫描并列出所有调用到这些 repository 接口的 ViewModel/usecase/Fragment 文件（逐文件列出调用行），便于评估改造范围。
- [ ] 按优先级为 `TransactionRepository` 编写兼容实现补丁（新增 `observeAllTransactionsWithRemote` + `refreshTransactionsFromRemote`），并修改 `TransactionRepositoryImpl` 来实现并行拉取逻辑，随后运行 `./gradlew build` 并修复出现的编译错误。
- [ ] 为至少一个关键页面（例如交易列表）把 ViewModel 切换到新 API，完成端到端验证。

请回复你希望我执行的下一步（例如“列出所有调用点”或“直接修改 TransactionRepository 实现并提交补丁”），我会立即开始并在更改后运行编译检查。

---

*文档生成时间：2026-05-02*

