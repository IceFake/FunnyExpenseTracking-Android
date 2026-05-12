# 本地数据库表结构字典（Room v8）

> 数据来源：`app/src/main/java/com/example/funnyexpensetracking/data/local/AppDatabase.kt`、`app/src/main/java/com/example/funnyexpensetracking/data/local/entity/*.kt`、`app/src/main/java/com/example/funnyexpensetracking/data/local/Converters.kt`、`app/src/main/java/com/example/funnyexpensetracking/di/DatabaseModule.kt`
>
> 说明：
> - 本文档口径为当前 `Room` 版本 `v8`。
> - SQLite 无严格 VARCHAR 长度约束；`TEXT` 为变长，`INTEGER` 为 1~8 字节变长存储，`REAL` 为 8 字节。
> - “是否默认 null”表示字段是否可空（可空即默认可为 `NULL`）。
> - 当前实体未声明 `@ForeignKey` / `@Index` / `@ColumnInfo(defaultValue)`。

## 数据库概览

- 数据库名：`funny_expense_db`
- Room 版本：`8`
- 表清单：
  - `transactions`
  - `accounts`
  - `fixed_incomes`
  - `stock_holdings`
  - `asset_snapshots`
  - `sync_metadata`
  - `asset_baseline`
  - `investments`

---

## 表：`transactions`

交易记录表用于存储用户的收支流水信息。在该表中，id 字段作为本地主键，用于唯一标识每一条交易记录，serverId 用于关联云端记录标识；amount、type、category 分别用于表征金额、收支类型与分类；accountId 用于标识该交易所属账户；note 用于记录备注信息；date 用于表示交易发生时间；createdAt 与 updatedAt 分别用于记录创建时间和更新时间；syncStatus 与 lastSyncAt 则用于描述该记录的同步状态及最近同步时间。

| 字段名 | 类型 | 长度 | 是否默认 null | 约束 | 描述 |
|---|---|---|---|---|---|
| `id` | INTEGER | 1~8 字节 | 否 | `PRIMARY KEY AUTOINCREMENT` | 本地主键 |
| `serverId` | TEXT | 变长 | 是 | 无 | 服务端主键（UUID） |
| `amount` | REAL | 8 字节 | 否 | `NOT NULL` | 金额 |
| `type` | TEXT（枚举） | 变长 | 否 | `NOT NULL` | 交易类型（`INCOME`/`EXPENSE`） |
| `category` | TEXT | 变长 | 否 | `NOT NULL` | 分类 |
| `accountId` | INTEGER | 1~8 字节 | 否 | `NOT NULL` | 账户 ID（逻辑关联，无外键约束） |
| `note` | TEXT | 变长 | 否 | `NOT NULL` | 备注 |
| `date` | INTEGER | 1~8 字节 | 否 | `NOT NULL` | 交易时间戳 |
| `createdAt` | INTEGER | 1~8 字节 | 否 | `NOT NULL` | 创建时间 |
| `updatedAt` | INTEGER | 1~8 字节 | 否 | `NOT NULL` | 更新时间 |
| `syncStatus` | TEXT（枚举） | 变长 | 否 | `NOT NULL` | 同步状态（`SYNCED`/`PENDING_UPLOAD`/`PENDING_DELETE`/`CONFLICT`） |
| `lastSyncAt` | INTEGER | 1~8 字节 | 是 | 无 | 上次同步时间 |

---

## 表：`accounts`

账户表用于存储用户资金账户的基础信息。在该表中，id 字段作为本地主键，用于唯一标识账户，serverId 用于关联云端账户；name 与 icon 分别用于表示账户名称和图标标识；balance 用于记录账户当前余额；isDefault 用于标识是否为默认账户，其中 1 表示默认，0 表示非默认；sortOrder 用于控制账户的展示顺序；createdAt 与 updatedAt 分别用于记录账户创建时间和更新时间；syncStatus 与 lastSyncAt 则用于支撑离线优先场景下的同步状态追踪。

| 字段名 | 类型 | 长度 | 是否默认 null | 约束 | 描述 |
|---|---|---|---|---|---|
| `id` | INTEGER | 1~8 字节 | 否 | `PRIMARY KEY AUTOINCREMENT` | 本地主键 |
| `serverId` | TEXT | 变长 | 是 | 无 | 服务端主键（UUID） |
| `name` | TEXT | 变长 | 否 | `NOT NULL` | 账户名称 |
| `icon` | TEXT | 变长 | 否 | `NOT NULL` | 图标标识 |
| `balance` | REAL | 8 字节 | 否 | `NOT NULL` | 账户余额 |
| `isDefault` | INTEGER（布尔） | 1~8 字节 | 否 | `NOT NULL` | 是否默认账户（0/1） |
| `sortOrder` | INTEGER | 1~8 字节 | 否 | `NOT NULL` | 排序顺序 |
| `createdAt` | INTEGER | 1~8 字节 | 否 | `NOT NULL` | 创建时间 |
| `updatedAt` | INTEGER | 1~8 字节 | 否 | `NOT NULL` | 更新时间 |
| `syncStatus` | TEXT（枚举） | 变长 | 否 | `NOT NULL` | 同步状态 |
| `lastSyncAt` | INTEGER | 1~8 字节 | 是 | 无 | 上次同步时间 |

---

## 表：`fixed_incomes`

固定收支表用于存储按周期自动累计的固定收入或固定支出配置，例如工资、房租等。在该表中，id 字段作为主键，用于唯一标识一条固定收支规则，name、amount、type、frequency 共同定义该规则的基础属性；startDate 与 endDate 用于表示生效时间区间，其中 endDate 允许为空，以支持长期有效的配置；isActive 用于标识规则当前是否启用；accumulatedMinutes、accumulatedAmount、lastRecordTime 用于记录实时累计算法所依赖的状态信息；createdAt 用于记录配置创建时间。

| 字段名 | 类型 | 长度 | 是否默认 null | 约束 | 描述 |
|---|---|---|---|---|---|
| `id` | INTEGER | 1~8 字节 | 否 | `PRIMARY KEY AUTOINCREMENT` | 主键 |
| `name` | TEXT | 变长 | 否 | `NOT NULL` | 固定收支名称 |
| `amount` | REAL | 8 字节 | 否 | `NOT NULL` | 周期金额 |
| `type` | TEXT（枚举） | 变长 | 否 | `NOT NULL` | 类型（固定收入/固定支出） |
| `frequency` | TEXT（枚举） | 变长 | 否 | `NOT NULL` | 频率（`DAILY`/`WEEKLY`/`MONTHLY`/`YEARLY`） |
| `startDate` | INTEGER | 1~8 字节 | 否 | `NOT NULL` | 开始日期（分钟级时间戳） |
| `endDate` | INTEGER | 1~8 字节 | 是 | 无 | 结束日期（可空） |
| `isActive` | INTEGER（布尔） | 1~8 字节 | 否 | `NOT NULL` | 是否生效 |
| `accumulatedMinutes` | INTEGER | 1~8 字节 | 否 | `NOT NULL` | 累计生效分钟数 |
| `accumulatedAmount` | REAL | 8 字节 | 否 | `NOT NULL` | 累计收支金额 |
| `lastRecordTime` | INTEGER | 1~8 字节 | 否 | `NOT NULL` | 上次记录时间点 |
| `createdAt` | INTEGER | 1~8 字节 | 否 | `NOT NULL` | 创建时间 |

---

## 表：`stock_holdings`

股票持仓表用于存储本地持仓明细及行情缓存信息。在该表中，id 字段作为主键，用于唯一标识一条持仓记录，symbol 与 name 分别表示股票代码和股票名称；shares 用于表示持仓数量；purchasePrice 与 totalCost 分别用于表示买入单价和购入总成本；purchaseDate 用于记录买入时间；currentPrice 与 lastUpdated 用于缓存最新价格及其更新时间；createdAt 用于记录持仓记录的创建时间。

| 字段名 | 类型 | 长度 | 是否默认 null | 约束 | 描述 |
|---|---|---|---|---|---|
| `id` | INTEGER | 1~8 字节 | 否 | `PRIMARY KEY AUTOINCREMENT` | 主键 |
| `symbol` | TEXT | 变长 | 否 | `NOT NULL` | 股票代码 |
| `name` | TEXT | 变长 | 否 | `NOT NULL` | 股票名称 |
| `shares` | REAL | 8 字节 | 否 | `NOT NULL` | 持有股数 |
| `purchasePrice` | REAL | 8 字节 | 否 | `NOT NULL` | 购入单价 |
| `totalCost` | REAL | 8 字节 | 否 | `NOT NULL` | 购入总成本 |
| `purchaseDate` | INTEGER | 1~8 字节 | 否 | `NOT NULL` | 购入日期 |
| `currentPrice` | REAL | 8 字节 | 否 | `NOT NULL` | 当前价格（缓存） |
| `lastUpdated` | INTEGER | 1~8 字节 | 否 | `NOT NULL` | 最后更新时间 |
| `createdAt` | INTEGER | 1~8 字节 | 否 | `NOT NULL` | 创建时间 |

---

## 表：`asset_snapshots`

资产快照表用于按时间序列记录资产变化情况，以支持趋势分析与历史回溯。在该表中，id 字段作为主键，用于唯一标识一次快照记录，totalAsset 用于表示总资产，cashAsset 用于表示现金类资产，stockAsset 用于表示股票类资产；timestamp 用于记录该次快照的采样时间点。

| 字段名 | 类型 | 长度 | 是否默认 null | 约束 | 描述 |
|---|---|---|---|---|---|
| `id` | INTEGER | 1~8 字节 | 否 | `PRIMARY KEY AUTOINCREMENT` | 主键 |
| `totalAsset` | REAL | 8 字节 | 否 | `NOT NULL` | 总资产 |
| `cashAsset` | REAL | 8 字节 | 否 | `NOT NULL` | 现金资产 |
| `stockAsset` | REAL | 8 字节 | 否 | `NOT NULL` | 股票资产 |
| `timestamp` | INTEGER | 1~8 字节 | 否 | `NOT NULL` | 快照时间 |

---

## 表：`sync_metadata`

同步元数据表用于记录各业务表的同步进度与状态。在该表中，tableName 字段作为主键，用于唯一标识某一业务表；lastSyncTimestamp 用于记录最近一次成功同步时间；lastSyncAttempt 用于记录最近一次同步尝试时间；pendingCount 用于表示当前待同步记录数量；lastError 用于保存最近一次同步错误信息，以支持状态提示与问题排查。

| 字段名 | 类型 | 长度 | 是否默认 null | 约束 | 描述 |
|---|---|---|---|---|---|
| `tableName` | TEXT | 变长 | 否 | `PRIMARY KEY` | 表名（每个业务表一条元数据） |
| `lastSyncTimestamp` | INTEGER | 1~8 字节 | 否 | `NOT NULL` | 最近成功同步时间 |
| `lastSyncAttempt` | INTEGER | 1~8 字节 | 否 | `NOT NULL` | 最近同步尝试时间 |
| `pendingCount` | INTEGER | 1~8 字节 | 否 | `NOT NULL` | 待同步记录数 |
| `lastError` | TEXT | 变长 | 是 | 无 | 最近错误信息 |

---

## 表：`asset_baseline`

资产基准表用于存储实时资产计算所依赖的基准点数据，通常仅维护一条当前有效记录。在该表中，id 字段作为主键，并约定固定取值为 1；baselineTimestamp 用于表示基准时间；baselineAmount 用于表示该时刻的基准资产值；updatedAt 用于记录该基准值最后一次更新时间。

| 字段名 | 类型 | 长度 | 是否默认 null | 约束 | 描述 |
|---|---|---|---|---|---|
| `id` | INTEGER | 1~8 字节 | 否 | `PRIMARY KEY` | 固定单行 ID（默认值 1） |
| `baselineTimestamp` | INTEGER | 1~8 字节 | 否 | `NOT NULL` | 基准时间戳 |
| `baselineAmount` | REAL | 8 字节 | 否 | `NOT NULL` | 基准资产值 |
| `updatedAt` | INTEGER | 1~8 字节 | 否 | `NOT NULL` | 更新时间 |

---

## 表：`investments`

投资条目表用于存储股票及其他类型的投资记录。在该表中，id 字段作为主键，用于唯一标识一条投资数据，category 用于表示投资分类，description 用于描述投资标的；quantity 用于表示数量，主要适用于股票类投资；investment 用于表示累计投入金额；currentPrice 与 currentValue 用于记录当前价格与当前价值，其中非股票类投资可直接维护 currentValue；createdAt 与 updatedAt 分别用于记录创建时间和更新时间。

| 字段名 | 类型 | 长度 | 是否默认 null | 约束 | 描述 |
|---|---|---|---|---|---|
| `id` | INTEGER | 1~8 字节 | 否 | `PRIMARY KEY AUTOINCREMENT` | 主键 |
| `category` | TEXT（枚举） | 变长 | 否 | `NOT NULL` | 投资分类（`STOCK`/`OTHER`） |
| `description` | TEXT | 变长 | 否 | `NOT NULL` | 描述（股票代码或说明） |
| `quantity` | REAL | 8 字节 | 否 | `NOT NULL` | 数量（股票使用） |
| `investment` | REAL | 8 字节 | 否 | `NOT NULL` | 投入金额 |
| `currentPrice` | REAL | 8 字节 | 否 | `NOT NULL` | 当前单价 |
| `currentValue` | REAL | 8 字节 | 否 | `NOT NULL` | 当前价值 |
| `createdAt` | INTEGER | 1~8 字节 | 否 | `NOT NULL` | 创建时间 |
| `updatedAt` | INTEGER | 1~8 字节 | 否 | `NOT NULL` | 更新时间 |

---

## 额外说明（数据库约束 vs 业务约束）

- 数据库层约束（可确认）：主键、是否 `NOT NULL`、自增（`AUTOINCREMENT`）。
- 业务层约束（代码语义）：
  - `asset_baseline.id` 约定只使用 `1`。
  - `transactions.accountId` 语义上关联账户，但数据库无外键限制。
  - 枚举字段通过 `Converters` 存储为 `TEXT`。
- 如需进一步增强一致性，可后续加迁移：`FOREIGN KEY`、`INDEX`、`UNIQUE`、`CHECK`。

