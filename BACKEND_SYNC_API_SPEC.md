# FunnyExpenseTracking 后端数据库与接口设计文档（V1）

> 目标：为 Android 端提供**离线优先同步**、**股票行情聚合**、**AI 分析代理**能力。  
> 依据：项目现有 `Repository`、`SyncManager`、`Entity/DTO` 结构反推后端契约。

---

## 1. 设计原则

1. **离线优先**：客户端先写本地，后端提供增量同步与冲突处理。
2. **统一时间格式**：所有时间戳使用 `Unix epoch milliseconds`（与 Android `Long` 对齐）。
3. **统一响应包装**：兼容当前客户端 `ApiResponse<T>` 结构（`code/message/data`）。
4. **幂等性**：写接口支持 `Idempotency-Key`，避免重试导致重复写入。
5. **可扩展性**：先满足 `transactions/accounts/stock/ai`，后续可扩展 fixed income、investment 等模块同步。

---

## 2. 客户端当前契约（必须兼容）

### 2.1 已存在接口（Android 已调用）

- 交易同步：
  - `POST /transactions/sync`
  - `GET /transactions?start_date={ms}&end_date={ms}`
  - `POST /transactions`
  - `PUT /transactions/{id}`
  - `DELETE /transactions/{id}`
- 股票行情：
  - `GET /stock/quote/{symbol}`
  - `GET /stock/realtime/{symbol}`
  - `POST /stock/quotes`
  - `GET /stock/search?keyword=...`
- AI 分析：
  - `POST /ai/analyze`
  - `GET /ai/suggestions`
  - `GET /ai/history?limit=...`
  - `GET /ai/result/{id}`

### 2.2 客户端同步关键字段

当前本地实体（交易、账户）具备：

- `serverId`: 服务端主键映射
- `syncStatus`: `SYNCED | PENDING_UPLOAD | PENDING_DELETE | CONFLICT`
- `lastSyncAt`: 最近同步时间
- `updatedAt`: 本地更新时间（冲突判定基础）

后端必须支持：

- 按 `updatedAt` 做增量拉取
- 软删除（或 tombstone）
- 冲突检测并返回冲突详情

---

## 3. 数据库设计（建议 PostgreSQL）

> 说明：字段名可按后端规范调整，以下为推荐逻辑模型。

## 3.1 用户与认证

### `users`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigserial PK | 用户ID |
| email | varchar(128) unique | 邮箱（可选） |
| phone | varchar(32) unique | 手机号（可选） |
| password_hash | varchar(255) | 密码哈希 |
| nickname | varchar(64) | 昵称 |
| status | varchar(16) | active/disabled |
| created_at | bigint | 创建时间(ms) |
| updated_at | bigint | 更新时间(ms) |

### `user_sessions`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigserial PK | 会话ID |
| user_id | bigint FK users(id) | 用户ID |
| device_id | varchar(128) | 设备标识 |
| refresh_token_hash | varchar(255) | refresh token 哈希 |
| expires_at | bigint | 过期时间(ms) |
| created_at | bigint | 创建时间(ms) |
| revoked_at | bigint nullable | 吊销时间(ms) |

## 3.2 核心业务表（同步主数据）

### `accounts`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigserial PK | 服务端账号ID（对应客户端 `serverId`） |
| user_id | bigint FK users(id) | 归属用户 |
| name | varchar(64) | 账户名 |
| icon | varchar(64) | 图标 |
| balance | numeric(18,2) | 余额 |
| is_default | boolean | 默认账户 |
| sort_order | int | 排序 |
| deleted_at | bigint nullable | 软删除时间 |
| version | bigint | 乐观锁版本号 |
| created_at | bigint | 创建时间 |
| updated_at | bigint | 更新时间 |

索引建议：`(user_id, updated_at)`、`(user_id, deleted_at)`。

### `transactions`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigserial PK | 服务端交易ID（对应客户端 `serverId`） |
| user_id | bigint FK users(id) | 归属用户 |
| account_id | bigint FK accounts(id) | 账户ID |
| amount | numeric(18,2) | 金额 |
| type | varchar(16) | INCOME/EXPENSE |
| category | varchar(64) | 分类 |
| note | text | 备注 |
| date | bigint | 交易时间 |
| deleted_at | bigint nullable | 软删除时间 |
| version | bigint | 乐观锁版本号 |
| created_at | bigint | 创建时间 |
| updated_at | bigint | 更新时间 |

索引建议：`(user_id, date desc)`、`(user_id, updated_at)`、`(user_id, deleted_at)`。

### `sync_tombstones`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigserial PK | 主键 |
| user_id | bigint | 用户ID |
| entity_type | varchar(32) | account/transaction |
| entity_id | bigint | 被删除实体ID |
| deleted_at | bigint | 删除时间 |

> 用途：客户端增量拉取时获知删除事件。

### `sync_cursors`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigserial PK | 主键 |
| user_id | bigint | 用户ID |
| table_name | varchar(32) | accounts/transactions |
| cursor_time | bigint | 上次同步游标 |
| updated_at | bigint | 更新时间 |

> 可选，若采用“客户端传 last_sync_time + 服务端即时查询”可不建表。

### `sync_conflicts`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigserial PK | 主键 |
| user_id | bigint | 用户ID |
| entity_type | varchar(32) | transaction/account |
| entity_id | bigint | 冲突实体ID |
| client_payload | jsonb | 客户端版本 |
| server_payload | jsonb | 服务端版本 |
| strategy | varchar(32) | lww/manual/merge |
| resolved | boolean | 是否解决 |
| created_at | bigint | 创建时间 |
| resolved_at | bigint nullable | 解决时间 |

## 3.3 股票与 AI 模块

### `stock_quote_cache`

| 字段 | 类型 | 说明 |
|---|---|---|
| symbol | varchar(32) PK | 股票代码（统一规范） |
| name | varchar(128) | 名称 |
| current_price | numeric(18,6) | 最新价 |
| open_price | numeric(18,6) | 开盘价 |
| high_price | numeric(18,6) | 最高价 |
| low_price | numeric(18,6) | 最低价 |
| close_price | numeric(18,6) | 昨收/收盘 |
| change_amount | numeric(18,6) | 涨跌额 |
| change_percent | numeric(10,4) | 涨跌幅 |
| volume | bigint | 成交量 |
| source | varchar(32) | sina/yahoo/other |
| quote_time | bigint | 行情时间 |
| updated_at | bigint | 缓存更新时间 |

### `ai_analysis_records`

| 字段 | 类型 | 说明 |
|---|---|---|
| analysis_id | varchar(64) PK | 分析ID（UUID） |
| user_id | bigint | 用户ID |
| analysis_type | varchar(32) | habit/... |
| input_snapshot | jsonb | 输入数据快照（可脱敏） |
| summary | text | 分析摘要 |
| habits | jsonb | 习惯洞察数组 |
| suggestions | jsonb | 建议数组 |
| predictions | jsonb | 预测对象 |
| model_name | varchar(64) | deepseek-chat 等 |
| token_usage | jsonb | token统计 |
| created_at | bigint | 创建时间 |

### `ai_provider_logs`（可选）

记录第三方 AI 请求耗时、状态码、错误信息，便于审计与限流调优。

---

## 4. API 设计（后端实现清单）

## 4.1 通用约定

### 统一响应结构（建议）

```json
{
  "code": 200,
  "message": "ok",
  "data": {},
  "request_id": "req_xxx"
}
```

### 通用错误码建议

- `200` 成功
- `400` 参数错误
- `401` 未认证
- `403` 无权限
- `404` 资源不存在
- `409` 数据冲突
- `422` 业务校验失败
- `429` 频率限制
- `500` 服务端错误

---

## 4.2 认证接口（新增）

> 当前 Android 代码尚未接入鉴权，但后端建议先具备，后续可平滑接入。

### `POST /auth/register`
- 入参：`email/phone + password + nickname`
- 出参：用户信息

### `POST /auth/login`
- 入参：账号+密码+`device_id`
- 出参：`access_token`、`refresh_token`、`expires_in`

### `POST /auth/refresh`
- 入参：`refresh_token`
- 出参：新 `access_token`

### `POST /auth/logout`
- 入参：`refresh_token` 或会话ID
- 出参：成功状态

---

## 4.3 同步接口（核心）

## A. 交易同步（兼容现有客户端）

### `POST /transactions/sync`

入参（当前客户端）：

```json
{
  "transactions": [
    {
      "id": 0,
      "amount": 88.5,
      "type": "EXPENSE",
      "category": "餐饮",
      "note": "午饭",
      "date": 1713600000000,
      "created_at": 1713600000000
    }
  ],
  "last_sync_time": 1713500000000
}
```

返回：`List<TransactionDto>`，用于客户端回填 `serverId` 和状态。

服务端行为要求：

1. 按用户隔离写入。
2. 客户端 `id=0` 视为新建，返回服务端 `id`。
3. 支持增量返回 `updated_at > last_sync_time` 的服务端变更。
4. 冲突时返回 `409` 与冲突详情（建议返回 server/client 两版本）。

## B. 交易查询/增删改

- `GET /transactions?start_date=&end_date=`
- `POST /transactions`
- `PUT /transactions/{id}`
- `DELETE /transactions/{id}`（软删除）

## C. 账户同步（建议新增，当前客户端 `SyncManager` 已预留）

> 现在 `syncAccounts()` 仍是 TODO 的占位逻辑，后端需要提供真实接口。

- `POST /accounts/sync`
- `GET /accounts`
- `POST /accounts`
- `PUT /accounts/{id}`
- `DELETE /accounts/{id}`

推荐 `POST /accounts/sync` 入参结构与交易同步一致：

```json
{
  "accounts": [
    {
      "id": 0,
      "name": "微信",
      "icon": "ic_wechat",
      "balance": 1000.0,
      "is_default": true,
      "sort_order": 1,
      "updated_at": 1713600000000
    }
  ],
  "last_sync_time": 1713500000000
}
```

## D. 增量拉取总线（可选但推荐）

### `GET /sync/changes?since={ms}`

一次返回多资源变更，减少客户端多次请求：

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "server_time": 1713609999999,
    "transactions": [],
    "accounts": [],
    "tombstones": [
      { "entity_type": "transaction", "entity_id": 123, "deleted_at": 1713601111111 }
    ]
  }
}
```

---

## 4.4 股票行情接口（后端聚合）

> 建议由后端统一调用新浪/雅虎等外部源，客户端只连业务后端。

### `GET /stock/quote/{symbol}`
返回单只股票标准化行情（字段与 `StockQuoteDto` 对齐）。

### `GET /stock/realtime/{symbol}`
强制实时拉取上游（跳过缓存或缩短缓存TTL）。

### `POST /stock/quotes`
入参：

```json
{
  "symbols": ["sh600519", "sz000001", "hk00700", "gb_aapl"]
}
```

返回：

```json
{
  "quotes": [
    {
      "symbol": "sh600519",
      "name": "贵州茅台",
      "current_price": 1800.0,
      "open_price": 1795.0,
      "high_price": 1815.0,
      "low_price": 1790.0,
      "close_price": 1798.0,
      "change": 2.0,
      "change_percent": 0.11,
      "volume": 1234567,
      "timestamp": 1713600000000
    }
  ]
}
```

### `GET /stock/search?keyword=...`
返回股票搜索候选：`symbol/name/exchange`。

### 股票接口后端要求

1. 统一股票代码规范（A股/港股/美股），兼容客户端 `sh/sz/hk/gb_` 格式。
2. 配置缓存 TTL（如 5~30 秒）+ 熔断重试。
3. 保证批量接口单次最大 symbols 数（如 50）。
4. 上游失败时返回可读错误信息，并区分业务失败/网络失败。

---

## 4.5 AI 分析接口（后端代理大模型）

### `POST /ai/analyze`
入参与当前 `AIAnalysisRequest` 一致：

- `user_id`
- `transactions[]`
- `fixed_incomes[]`
- `analysis_type`

返回与 `AIAnalysisResultDto` 对齐：

- `analysis_id`
- `summary`
- `spending_habits[]`
- `suggestions[]`
- `predictions`
- `generated_at`

### `GET /ai/suggestions`
返回最近一次分析建议，或基于用户历史汇总建议。

### `GET /ai/history?limit=20`
分页返回历史分析。

### `GET /ai/result/{id}`
按分析ID查询结果。

### AI 接口后端要求

1. **密钥托管在后端**，客户端不直接持有 DeepSeek/OpenAI 密钥。
2. 对 prompt 做长度控制和脱敏（手机号、卡号、精确地址等）。
3. 保存分析快照与模型输出，支持审计与回放。
4. 增加限流（用户维度 + IP维度）。
5. 处理第三方错误码映射：`401/402/429/5xx`。

---

## 5. 冲突处理方案（同步必须实现）

## 5.1 冲突判定

满足任一条件视为冲突：

- 客户端提交的 `updated_at` 早于服务端当前版本。
- 服务端 `version` 与客户端上传 `version` 不一致。

## 5.2 建议策略

1. 默认：`LWW`（最后更新时间覆盖）。
2. 金融敏感字段（如余额）建议字段级合并或拒绝并提示人工处理。
3. 冲突响应使用 `409`，返回：
   - `entity_type`
   - `entity_id`
   - `client_version`
   - `server_version`
   - `suggested_resolution`

---

## 6. 安全与运维要求

1. 全链路 HTTPS。
2. JWT + Refresh Token（支持设备维度吊销）。
3. 接口审计日志（请求ID、用户ID、耗时、状态码）。
4. 数据库定期备份，支持按用户导出/删除（合规）。
5. 关键接口监控：同步成功率、冲突率、AI调用失败率、股票上游失败率。

---

## 7. 建议实施顺序（MVP -> 增强）

### Phase 1（MVP）

1. 交易同步接口 + 交易CRUD。
2. 账户同步接口（补齐 `SyncManager.syncAccounts()` 的后端支持）。
3. 股票 `quote/batch/search` 三个接口。
4. AI `analyze/history/result` 三个接口。

### Phase 2（增强）

1. `sync/changes` 总线接口。
2. 冲突记录中心 + 人工处理后台。
3. AI 异步任务化（`POST /ai/analyze` 返回 job_id）。
4. 引入 fixed income / investment 的云同步。

---

## 8. 与当前 Android 项目对齐清单

- 已对齐 DTO：`TransactionDto`、`StockQuoteDto`、`BatchQuoteRequest/Response`、`AIAnalysisRequest/ResultDto`。
- 已对齐响应包装：`ApiResponse<T>`。
- 已识别客户端待补点：`SyncManager.syncAccounts()` 仍为本地占位，需要后端 `accounts/sync` 支持。
- 建议改造点：将当前直连外部股票/AI 的调用迁移为“客户端 -> 业务后端 -> 外部服务”。

---

如果你希望，我可以下一步直接再给你一份：
- **可导入 Apifox/Postman 的 OpenAPI 3.1 YAML**（按本文件接口生成），以及
- **PostgreSQL 建表 SQL 初稿**（含索引与约束）。

