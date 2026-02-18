# 酒丸蔵 API 仕様書

> **Warehouse Management System API for Android picking terminals**
>
> - **バージョン**: 1.0.0
> - **サーバー**: `https://wms.lw-hana.net`
> - **OpenAPI**: 3.0.0

---

## 接続情報

| 項目 | 値 |
|------|-----|
| API Swagger URL | `https://wms.lw-hana.net/api/documentation` |
| Swagger Basic認証 | `local.properties` の `SWAGGER_USER` / `SWAGGER_PW` を参照 |
| テストユーザー | `local.properties` の `API_TEST_USER` / `API_TEST_PW` を参照 |
| API Key | `local.properties` の `WMS_API_KEY` を参照 |

> **注意**: 認証情報は `local.properties` に記載（gitignore対象、コミットしない）

---

## 認証方式

本APIでは2種類の認証スキームを使用します。

| スキーム名 | タイプ | 説明 | 送信方法 |
|---|---|---|---|
| `apiKey` | API Key | WMS API アクセス用 API キー（全エンドポイントで必須） | ヘッダー `X-API-Key` |
| `sanctum` | HTTP Bearer | `/auth/login` エンドポイントで取得した Bearer トークン | `Authorization: Bearer {token}` |

---

## 共通レスポンス形式

### 成功時

```json
{
  "is_success": true,
  "code": "SUCCESS",
  "result": {
    "data": { ... },
    "message": "...",
    "debug_message": null
  }
}
```

### エラー時

```json
{
  "is_success": false,
  "code": "UNAUTHORIZED" | "VALIDATION_ERROR",
  "result": {
    "data": null,
    "error_message": "...",
    "errors": { ... }
  }
}
```

---

## 1. Authentication（認証）

### 1-1. POST `/api/auth/login` -- ログイン

| 項目 | 内容 |
|---|---|
| **Summary** | Login to WMS |
| **Description** | Authenticate picker with code and password, returns API token |
| **認証** | `apiKey` のみ（Bearer トークン不要） |

#### リクエストボディ (`application/x-www-form-urlencoded`)

| フィールド名 | 型 | 必須/任意 | 説明 | 例 |
|---|---|---|---|---|
| `code` | string | **必須** | ピッカーコード | `TEST001` |
| `password` | string | **必須** | パスワード | `password123` |
| `device_id` | string | 任意 | デバイスID | `ANDROID-12345` |

#### レスポンス

##### 200 -- Successful login

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `is_success` | boolean | 成功フラグ | `true` |
| `code` | string | レスポンスコード | `LOGIN_SUCCESS` |
| `result.data.token` | string | API トークン | `1\|abcdef123456...` |
| `result.data.picker.id` | integer | ピッカーID | `2` |
| `result.data.picker.code` | string | ピッカーコード | `TEST001` |
| `result.data.picker.name` | string | ピッカー名 | `テストピッカー` |
| `result.data.picker.default_warehouse_id` | integer | デフォルト倉庫ID | `991` |
| `result.message` | string | メッセージ | `Login successful` |
| `result.debug_message` | string (nullable) | デバッグメッセージ | `null` |

##### 401 -- Invalid credentials or inactive account

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `is_success` | boolean | 成功フラグ | `false` |
| `code` | string | レスポンスコード | `UNAUTHORIZED` |
| `result.data` | object (nullable) | データ | `null` |
| `result.error_message` | string | エラーメッセージ | `Invalid credentials` |

##### 422 -- Validation error

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `is_success` | boolean | 成功フラグ | `false` |
| `code` | string | レスポンスコード | `VALIDATION_ERROR` |
| `result.data` | object (nullable) | データ | `null` |
| `result.error_message` | string | エラーメッセージ | `Validation failed` |
| `result.errors.code` | string[] | code フィールドのエラー | `["validation.required"]` |
| `result.errors.password` | string[] | password フィールドのエラー | `["validation.required"]` |

---

### 1-2. POST `/api/auth/logout` -- ログアウト

| 項目 | 内容 |
|---|---|
| **Summary** | Logout from WMS |
| **Description** | Revoke current API token |
| **認証** | `apiKey` + `sanctum`（Bearer トークン必須） |

#### リクエストボディ

なし

#### レスポンス

##### 200 -- Successfully logged out

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `is_success` | boolean | 成功フラグ | `true` |
| `code` | string | レスポンスコード | `LOGOUT_SUCCESS` |
| `result.data` | object (nullable) | データ | `null` |
| `result.message` | string | メッセージ | `Logged out successfully` |
| `result.debug_message` | string (nullable) | デバッグメッセージ | `null` |

##### 401 -- Unauthenticated

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `is_success` | boolean | 成功フラグ | `false` |
| `code` | string | レスポンスコード | `UNAUTHORIZED` |
| `result.data` | object (nullable) | データ | `null` |
| `result.error_message` | string | エラーメッセージ | `Unauthenticated` |

---

### 1-3. GET `/api/me` -- 現在のピッカー情報取得

| 項目 | 内容 |
|---|---|
| **Summary** | Get current picker info |
| **Description** | Returns information about the authenticated picker |
| **認証** | `apiKey` + `sanctum`（Bearer トークン必須） |

#### リクエストパラメータ

なし

#### レスポンス

##### 200 -- Picker information

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `is_success` | boolean | 成功フラグ | `true` |
| `code` | string | レスポンスコード | `SUCCESS` |
| `result.data.id` | integer | ピッカーID | `2` |
| `result.data.code` | string | ピッカーコード | `TEST001` |
| `result.data.name` | string | ピッカー名 | `テストピッカー` |
| `result.data.default_warehouse_id` | integer | デフォルト倉庫ID | `991` |
| `result.debug_message` | string (nullable) | デバッグメッセージ | `null` |

##### 401 -- Unauthenticated

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `is_success` | boolean | 成功フラグ | `false` |
| `code` | string | レスポンスコード | `UNAUTHORIZED` |
| `result.data` | object (nullable) | データ | `null` |
| `result.error_message` | string | エラーメッセージ | `Unauthenticated` |

---

## 2. Master Data（マスタデータ）

### 2-1. GET `/api/master/warehouses` -- 倉庫マスタ一覧取得

| 項目 | 内容 |
|---|---|
| **Summary** | Get warehouse master list |
| **Description** | Retrieve all warehouses with id, code, name, kana_name, and out_of_stock_option |
| **認証** | `apiKey` + `sanctum`（Bearer トークン必須） |

#### リクエストパラメータ

なし

#### レスポンス

##### 200 -- Successful response

`result.data` は倉庫オブジェクトの配列です。

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `is_success` | boolean | 成功フラグ | `true` |
| `code` | string | レスポンスコード | `SUCCESS` |
| `result.data[].id` | integer | 倉庫ID | `991` |
| `result.data[].code` | string | 倉庫コード | `991` |
| `result.data[].name` | string | 倉庫名 | `酒丸本社` |
| `result.data[].kana_name` | string | 倉庫名カナ | `サケマルホンシャ` |
| `result.data[].out_of_stock_option` | string | 在庫切れ時オプション | `IGNORE_STOCK` または `UP_TO_STOCK` |

##### 401 -- Unauthorized - Invalid or missing token

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `is_success` | boolean | 成功フラグ | `false` |
| `code` | string | レスポンスコード | `UNAUTHORIZED` |
| `result.data` | object (nullable) | データ | `null` |
| `result.error_message` | string | エラーメッセージ | `Unauthenticated` |

---

## 3. Incoming（入荷作業関連）

### 3-1. GET `/api/incoming/schedules` -- 入庫予定一覧取得

| 項目 | 内容 |
|---|---|
| **Summary** | 入庫予定一覧取得 |
| **Description** | 倉庫別の入庫予定を検索。商品コード、JANコード、商品名で検索可能。仮想倉庫に紐づく入庫予定も含む。 |
| **認証** | `apiKey` + `sanctum`（Bearer トークン必須） |

#### クエリパラメータ

| パラメータ名 | 型 | 必須/任意 | 説明 | 例 |
|---|---|---|---|---|
| `warehouse_id` | integer | **必須** | 作業倉庫ID（実倉庫を指定すると仮想倉庫分も取得） | `991` |
| `search` | string | 任意 | 検索キーワード（商品コード、JANコード、商品名） | `4901234567890` |

#### レスポンス

##### 200 -- 成功

`result.data` は入庫予定商品オブジェクトの配列です。

**商品情報（data[] 直下）**

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `item_id` | integer | 商品ID | `123` |
| `item_code` | string | 商品コード | `10001` |
| `item_name` | string | 商品名 | `商品A` |
| `search_code` | string | 検索コード（カンマ区切り） | `4901234567890,4901234567891` |
| `jan_codes` | string[] | JANコード一覧 | -- |
| `volume` | string | 容量 | `720ml` |
| `temperature_type` | string | 温度帯 | `常温` |
| `images` | string[] | 商品画像URL一覧 | -- |
| `total_expected_quantity` | integer | 合計予定数量 | `100` |
| `total_received_quantity` | integer | 合計入庫済数量 | `20` |
| `total_remaining_quantity` | integer | 合計残数量 | `80` |

**倉庫別入庫予定（data[].warehouses[]）**

| フィールド | 型 | 説明 |
|---|---|---|
| `warehouse_id` | integer | 倉庫ID |
| `warehouse_code` | string | 倉庫コード |
| `warehouse_name` | string | 倉庫名 |
| `expected_quantity` | integer | 予定数量 |
| `received_quantity` | integer | 入庫済数量 |
| `remaining_quantity` | integer | 残数量 |

**個別入庫予定（data[].schedules[]）**

| フィールド | 型 | 説明 |
|---|---|---|
| `id` | integer | 入庫予定ID |
| `warehouse_id` | integer | 倉庫ID |
| `warehouse_name` | string | 倉庫名 |
| `expected_quantity` | integer | 予定数量 |
| `received_quantity` | integer | 入庫済数量 |
| `remaining_quantity` | integer | 残数量 |
| `quantity_type` | string | 数量タイプ（`PIECE` / `CASE`） |
| `expected_arrival_date` | string (date) | 入荷予定日 |
| `status` | string | ステータス（`PENDING` / `PARTIAL`） |

##### 422 -- バリデーションエラー

---

### 3-2. GET `/api/incoming/schedules/{id}` -- 入庫予定詳細取得

| 項目 | 内容 |
|---|---|
| **Summary** | 入庫予定詳細取得 |
| **Description** | 入庫予定の詳細情報を取得 |
| **認証** | `apiKey` + `sanctum`（Bearer トークン必須） |

#### パスパラメータ

| パラメータ名 | 型 | 必須/任意 | 説明 |
|---|---|---|---|
| `id` | integer | **必須** | 入庫予定ID |

#### レスポンス

##### 200 -- 成功

| フィールド | 型 | 説明 |
|---|---|---|
| `result.data.id` | integer | 入庫予定ID |
| `result.data.warehouse_id` | integer | 倉庫ID |
| `result.data.warehouse_code` | string | 倉庫コード |
| `result.data.warehouse_name` | string | 倉庫名 |
| `result.data.item_id` | integer | 商品ID |
| `result.data.item_code` | string | 商品コード |
| `result.data.item_name` | string | 商品名 |
| `result.data.search_code` | string | 検索コード |
| `result.data.jan_codes` | string[] | JANコード一覧 |
| `result.data.expected_quantity` | integer | 予定数量 |
| `result.data.received_quantity` | integer | 入庫済数量 |
| `result.data.remaining_quantity` | integer | 残数量 |
| `result.data.quantity_type` | string | 数量タイプ |
| `result.data.expected_arrival_date` | string (date) | 入荷予定日 |
| `result.data.status` | string | ステータス |

##### 404 -- 入庫予定が見つかりません

---

### 3-3. GET `/api/incoming/work-items` -- 作業データ一覧取得

| 項目 | 内容 |
|---|---|
| **Summary** | 作業データ一覧取得 |
| **Description** | 指定倉庫の入荷作業データを取得。statusパラメータで作業中・完了・キャンセル済みを絞り込み可能。 |
| **認証** | `apiKey` + `sanctum`（Bearer トークン必須） |

#### クエリパラメータ

| パラメータ名 | 型 | 必須/任意 | デフォルト | 説明 | 例 |
|---|---|---|---|---|---|
| `warehouse_id` | integer | **必須** | -- | 倉庫ID | -- |
| `picker_id` | integer | 任意 | -- | 作業者ID（指定時はその作業者のデータのみ） | -- |
| `status` | string | 任意 | `WORKING` | ステータス絞り込み | `WORKING`, `COMPLETED`, `CANCELLED`, `all` |
| `from_date` | string (date) | 任意 | -- | 開始日（履歴絞り込み用、YYYY-MM-DD形式） | `2026-01-01` |
| `to_date` | string (date) | 任意 | -- | 終了日（履歴絞り込み用、YYYY-MM-DD形式） | `2026-01-31` |
| `limit` | integer | 任意 | `100` | 取得件数 | -- |

#### レスポンス

##### 200 -- 成功

`result.data` は `IncomingWorkItem` オブジェクトの配列です。スキーマ詳細は下記「IncomingWorkItem スキーマ」を参照。

---

### 3-4. POST `/api/incoming/work-items` -- 入荷作業開始

| 項目 | 内容 |
|---|---|
| **Summary** | 入荷作業開始 |
| **Description** | 入庫予定に対する入荷作業を開始し、作業データを作成 |
| **認証** | `apiKey` + `sanctum`（Bearer トークン必須） |

#### リクエストボディ (`application/json`)

| フィールド名 | 型 | 必須/任意 | 説明 |
|---|---|---|---|
| `incoming_schedule_id` | integer | **必須** | 入庫予定ID |
| `picker_id` | integer | **必須** | 作業者ID |
| `warehouse_id` | integer | **必須** | 作業倉庫ID |

#### レスポンス

##### 200 -- 成功

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `is_success` | boolean | 成功フラグ | `true` |
| `code` | string | レスポンスコード | `SUCCESS` |
| `result.data` | IncomingWorkItem | 作成された作業データ | -- |
| `result.message` | string | メッセージ | `作業を開始しました` |

##### 400 -- 既に作業中 / 作業不可

##### 404 -- 入庫予定が見つかりません

##### 422 -- バリデーションエラー

---

### 3-5. PUT `/api/incoming/work-items/{id}` -- 作業データ更新

| 項目 | 内容 |
|---|---|
| **Summary** | 作業データ更新 |
| **Description** | 入荷作業中のデータ（数量、入荷日、賞味期限）を更新 |
| **認証** | `apiKey` + `sanctum`（Bearer トークン必須） |

#### パスパラメータ

| パラメータ名 | 型 | 必須/任意 | 説明 |
|---|---|---|---|
| `id` | integer | **必須** | 作業データID |

#### リクエストボディ (`application/json`)

| フィールド名 | 型 | 必須/任意 | 説明 |
|---|---|---|---|
| `work_quantity` | integer | 任意 | 入荷数量 |
| `work_arrival_date` | string (date) | 任意 | 入荷日 |
| `work_expiration_date` | string (date) | 任意 | 賞味期限 |
| `location_id` | integer | 任意 | 入庫ロケーションID |

#### レスポンス

##### 200 -- 成功

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `is_success` | boolean | 成功フラグ | `true` |
| `code` | string | レスポンスコード | `SUCCESS` |
| `result.data` | IncomingWorkItem | 更新された作業データ | -- |
| `result.message` | string | メッセージ | `更新しました` |

##### 400 -- 編集不可

##### 404 -- 作業データが見つかりません

---

### 3-6. DELETE `/api/incoming/work-items/{id}` -- 作業キャンセル

| 項目 | 内容 |
|---|---|
| **Summary** | 作業キャンセル |
| **Description** | 入荷作業をキャンセル。作業中のデータのみキャンセル可能。 |
| **認証** | `apiKey` + `sanctum`（Bearer トークン必須） |

#### パスパラメータ

| パラメータ名 | 型 | 必須/任意 | 説明 |
|---|---|---|---|
| `id` | integer | **必須** | 作業データID |

#### リクエストボディ

なし

#### レスポンス

##### 200 -- 成功

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `is_success` | boolean | 成功フラグ | `true` |
| `code` | string | レスポンスコード | `SUCCESS` |
| `result.data` | null | データなし | `null` |
| `result.message` | string | メッセージ | `キャンセルしました` |

##### 400 -- キャンセル不可

##### 404 -- 作業データが見つかりません

---

### 3-7. POST `/api/incoming/work-items/{id}/complete` -- 入荷作業完了

| 項目 | 内容 |
|---|---|
| **Summary** | 入荷作業完了 |
| **Description** | 入荷作業を完了し、入庫確定処理を実行。全量入庫または一部入庫を判定して処理。 |
| **認証** | `apiKey` + `sanctum`（Bearer トークン必須） |

#### パスパラメータ

| パラメータ名 | 型 | 必須/任意 | 説明 |
|---|---|---|---|
| `id` | integer | **必須** | 作業データID |

#### リクエストボディ

なし

#### レスポンス

##### 200 -- 成功

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `is_success` | boolean | 成功フラグ | `true` |
| `code` | string | レスポンスコード | `SUCCESS` |
| `result.data` | null | データなし | `null` |
| `result.message` | string | メッセージ | `入庫を確定しました` |

##### 400 -- 完了不可 / 入庫予定が見つかりません

##### 404 -- 作業データが見つかりません

##### 500 -- 入庫確定に失敗

---

### 3-8. GET `/api/incoming/locations` -- ロケーション検索

| 項目 | 内容 |
|---|---|
| **Summary** | ロケーション検索 |
| **Description** | 倉庫内のロケーションを検索。code1, code2, code3, nameで検索可能。 |
| **認証** | `apiKey` + `sanctum`（Bearer トークン必須） |

#### クエリパラメータ

| パラメータ名 | 型 | 必須/任意 | デフォルト | 説明 | 例 |
|---|---|---|---|---|---|
| `warehouse_id` | integer | **必須** | -- | 倉庫ID | -- |
| `search` | string | 任意 | -- | 検索キーワード（code1, code2, code3, nameで検索） | `A-1` |
| `limit` | integer | 任意 | `50` | 取得件数 | -- |

#### レスポンス

##### 200 -- 成功

`result.data` はロケーションオブジェクトの配列です。

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `result.data[].id` | integer | ロケーションID | -- |
| `result.data[].code1` | string | コード1 | -- |
| `result.data[].code2` | string | コード2 | -- |
| `result.data[].code3` | string | コード3 | -- |
| `result.data[].name` | string | ロケーション名 | -- |
| `result.data[].display_name` | string | 表示名 | `A 1 01` |

##### 422 -- バリデーションエラー

---

### IncomingWorkItem スキーマ（共通）

入荷作業関連APIで共通利用される作業データオブジェクトのスキーマです。

#### 作業データ本体

| フィールド | 型 | Nullable | 説明 |
|---|---|---|---|
| `id` | integer | No | 作業データID |
| `incoming_schedule_id` | integer | No | 入庫予定ID |
| `picker_id` | integer | No | 作業者ID |
| `warehouse_id` | integer | No | 倉庫ID |
| `location_id` | integer | Yes | 入庫ロケーションID |
| `work_quantity` | integer | No | 作業数量 |
| `work_arrival_date` | string (date) | No | 入荷日 |
| `work_expiration_date` | string (date) | Yes | 賞味期限（デフォルト: 商品のdefault_expiration_daysから計算） |
| `status` | string | No | ステータス（`WORKING` / `COMPLETED` / `CANCELLED`） |
| `started_at` | string (date-time) | No | 作業開始日時 |

#### ネストオブジェクト: `location`（nullable）

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `id` | integer | ロケーションID | -- |
| `code1` | string | コード1 | -- |
| `code2` | string | コード2 | -- |
| `code3` | string | コード3 | -- |
| `name` | string | ロケーション名 | -- |
| `display_name` | string | 表示名 | `A 1 01` |

#### ネストオブジェクト: `schedule`（nullable）

| フィールド | 型 | 説明 |
|---|---|---|
| `id` | integer | 入庫予定ID |
| `item_id` | integer | 商品ID |
| `item_code` | string | 商品コード |
| `item_name` | string | 商品名 |
| `warehouse_id` | integer | 倉庫ID |
| `warehouse_name` | string | 倉庫名 |
| `expected_quantity` | integer | 予定数量 |
| `received_quantity` | integer | 入庫済数量 |
| `remaining_quantity` | integer | 残数量 |
| `quantity_type` | string | 数量タイプ |

---

## 4. Picking Tasks（ピッキング作業）

### 4-1. GET `/api/picking/tasks` -- ピッキングタスク一覧取得

| 項目 | 内容 |
|---|---|
| **Summary** | Get picking task list |
| **Description** | Retrieve picking tasks grouped by delivery course and picking area, optimized by walking order |
| **認証** | `apiKey` + `sanctum`（Bearer トークン必須） |

#### クエリパラメータ

| パラメータ名 | 型 | 必須/任意 | 説明 | 例 |
|---|---|---|---|---|
| `warehouse_id` | integer | **必須** | 倉庫ID | `991` |
| `picker_id` | integer | 任意 | ピッカーID（指定時はそのピッカーのタスクのみ） | `1` |
| `picking_area_id` | integer | 任意 | ピッキングエリアID（指定時はそのエリアのタスクのみ） | `1` |

#### レスポンス

##### 200 -- Successful response

`result.data` はタスクオブジェクトの配列です。各タスクは配送コース・ピッキングエリア・ウェーブ・ピッキングリストで構成されます。

**タスクオブジェクト (data[])**

| フィールド | 型 | 説明 |
|---|---|---|
| `course` | object | 配送コース情報 |
| `picking_area` | object | ピッキングエリア情報 |
| `wave` | object | ウェーブ情報 |
| `picking_list` | array | ピッキング対象商品リスト |

**course オブジェクト**

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `code` | string | コースコード | `910072` |
| `name` | string | コース名 | `佐藤　尚紀` |

**picking_area オブジェクト**

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `code` | string | エリアコード | `B` |
| `name` | string | エリア名 | `エリアB（バラ）` |

**wave オブジェクト**

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `wms_picking_task_id` | integer | ピッキングタスクID | `1` |
| `wms_wave_id` | integer | ウェーブID | `5` |

**picking_list[] 各要素**

| フィールド | 型 | Nullable | 説明 | 例 |
|---|---|---|---|---|
| `wms_picking_item_result_id` | integer | No | ピッキング結果ID | `1` |
| `item_id` | integer | No | 商品ID | `111110` |
| `item_name` | string | No | 商品名 | `×白鶴特撰　本醸造生貯蔵酒７２０ｍｌ（ギフト）` |
| `jan_code` | string | Yes | 代表JANコード（最新更新のもの） | `4901681115008` |
| `jan_code_list` | string[] | No | 全JANコード一覧（updated_at降順） | `["4901681115008", "4901681115015"]` |
| `volume` | string | Yes | 容量（単位付き） | `720ml` |
| `capacity_case` | integer | Yes | ケース入数 | `12` |
| `packaging` | string | Yes | 包装タイプ | `瓶` |
| `temperature_type` | string | Yes | 温度帯 | `常温` |
| `images` | string[] | No | 商品画像URL一覧（image_url_1, image_url_2, image_url_3） | `["https://example.com/image1.jpg"]` |
| `planned_qty_type` | string | No | 数量タイプ | `CASE` または `PIECE` |
| `planned_qty` | string | No | 予定数量 | `2.00` |
| `picked_qty` | string | No | ピッキング済数量 | `0.00` |
| `status` | string | No | ステータス（`PENDING`: 未開始, `PICKING`: 作業中, `COMPLETED`: 完了, `SHORTAGE`: 欠品） | `PENDING` |
| `slip_number` | integer | No | 伝票番号（Earning IDを使用） | `1` |

##### 400 -- Validation error

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `message` | string | エラーメッセージ | `The warehouse id field is required.` |
| `errors.warehouse_id` | string[] | warehouse_id のバリデーションエラー | `["The warehouse id field is required."]` |

##### 401 -- Unauthorized - Invalid or missing token

---

### 4-2. GET `/api/picking/tasks/{id}` -- ピッキングタスク単体取得

| 項目 | 内容 |
|---|---|
| **Summary** | Get single picking task |
| **Description** | Retrieve a single picking task with course, picking area, wave, and picking list |
| **認証** | `apiKey` + `sanctum`（Bearer トークン必須） |

#### パスパラメータ

| パラメータ名 | 型 | 必須/任意 | 説明 | 例 |
|---|---|---|---|---|
| `id` | integer | **必須** | ピッキングタスクID | `1` |

#### レスポンス

##### 200 -- Successful response

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `is_success` | boolean | 成功フラグ | `true` |
| `code` | string | レスポンスコード | `SUCCESS` |

> **注**: レスポンスデータ構造は4-1のタスク一覧と同様のタスクオブジェクト形式です。

##### 404 -- Task not found

---

### 4-3. GET `/api/picking/items/{id}` -- ピッキング商品結果単体取得

| 項目 | 内容 |
|---|---|
| **Summary** | Get single picking item result |
| **Description** | Retrieve a single picking item result with item details |
| **認証** | `apiKey` + `sanctum`（Bearer トークン必須） |

#### パスパラメータ

| パラメータ名 | 型 | 必須/任意 | 説明 | 例 |
|---|---|---|---|---|
| `id` | integer | **必須** | ピッキング結果ID | `1` |

#### レスポンス

##### 200 -- Successful response

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `is_success` | boolean | 成功フラグ | `true` |
| `code` | string | レスポンスコード | `SUCCESS` |

> **注**: レスポンスデータには商品詳細情報が含まれます。

##### 404 -- Item not found

---

### 4-4. POST `/api/picking/tasks/{id}/start` -- ピッキングタスク開始

| 項目 | 内容 |
|---|---|
| **Summary** | Start picking task |
| **Description** | Change task status to PICKING and set started_at timestamp |
| **認証** | `apiKey` + `sanctum`（Bearer トークン必須） |

#### パスパラメータ

| パラメータ名 | 型 | 必須/任意 | 説明 | 例 |
|---|---|---|---|---|
| `id` | integer | **必須** | ピッキングタスクID | `1` |

#### リクエストボディ

なし

#### レスポンス

##### 200 -- Task started successfully

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `is_success` | boolean | 成功フラグ | `true` |
| `code` | string | レスポンスコード | `SUCCESS` |
| `result.data.id` | integer | タスクID | `1` |
| `result.data.status` | string | 更新後ステータス | `PICKING` |
| `result.data.started_at` | string | 開始日時 | `2025-11-02 10:30:00` |
| `result.message` | string | メッセージ | `Picking task started` |
| `result.debug_message` | string (nullable) | デバッグメッセージ | `null` |

##### 404 -- Task not found

##### 422 -- Task already started or completed

---

### 4-5. POST `/api/picking/tasks/{wms_picking_item_result_id}/update` -- ピッキング結果更新

| 項目 | 内容 |
|---|---|
| **Summary** | Update picking result |
| **Description** | Update picked quantity for a specific item in the picking task |
| **認証** | `apiKey` + `sanctum`（Bearer トークン必須） |

#### パスパラメータ

| パラメータ名 | 型 | 必須/任意 | 説明 | 例 |
|---|---|---|---|---|
| `wms_picking_item_result_id` | integer | **必須** | ピッキング結果ID | `1` |

#### リクエストボディ (`application/json`)

| フィールド名 | 型 | 必須/任意 | 説明 | 例 |
|---|---|---|---|---|
| `picked_qty` | number | **必須** | ピッキング数量 | `5` |
| `picked_qty_type` | string | 任意 | 数量タイプ（`CASE` / `PIECE`） | `PIECE` |

#### レスポンス

##### 200 -- Picking result updated

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `is_success` | boolean | 成功フラグ | `true` |
| `code` | string | レスポンスコード | `SUCCESS` |
| `result.data.id` | integer | ピッキング結果ID | `1` |
| `result.data.picked_qty` | number | ピッキング済数量 | `5` |
| `result.data.shortage_qty` | number | 欠品数量 | `0` |
| `result.data.status` | string | 更新後ステータス | `COMPLETED` |
| `result.message` | string | メッセージ | `Picking result updated` |
| `result.debug_message` | string (nullable) | デバッグメッセージ | `null` |

##### 404 -- Item result not found

##### 422 -- Validation error

---

### 4-6. POST `/api/picking/tasks/{id}/complete` -- ピッキングタスク完了

| 項目 | 内容 |
|---|---|
| **Summary** | Complete picking task |
| **Description** | Mark task as completed. Status will be COMPLETED if all items are picked, SHORTAGE if any shortages exist |
| **認証** | `apiKey` + `sanctum`（Bearer トークン必須） |

#### パスパラメータ

| パラメータ名 | 型 | 必須/任意 | 説明 | 例 |
|---|---|---|---|---|
| `id` | integer | **必須** | ピッキングタスクID | `1` |

#### リクエストボディ

なし

#### レスポンス

##### 200 -- Task completed

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `is_success` | boolean | 成功フラグ | `true` |
| `code` | string | レスポンスコード | `SUCCESS` |
| `result.data.id` | integer | タスクID | `1` |
| `result.data.status` | string | 更新後ステータス（`COMPLETED` または `SHORTAGE`） | `COMPLETED` |
| `result.data.completed_at` | string | 完了日時 | `2025-11-02 11:00:00` |
| `result.message` | string | メッセージ | `Picking task completed` |
| `result.debug_message` | string (nullable) | デバッグメッセージ | `null` |

##### 404 -- Task not found

##### 422 -- Task cannot be completed

---

### 4-7. POST `/api/picking/tasks/{wms_picking_item_result_id}/cancel` -- ピッキング結果キャンセル

| 項目 | 内容 |
|---|---|
| **Summary** | Cancel picking item result |
| **Description** | Reset picking item result to PENDING status with picked_qty = 0 |
| **認証** | `apiKey` + `sanctum`（Bearer トークン必須） |

#### パスパラメータ

| パラメータ名 | 型 | 必須/任意 | 説明 | 例 |
|---|---|---|---|---|
| `wms_picking_item_result_id` | integer | **必須** | ピッキング結果ID | `1` |

#### リクエストボディ

なし

#### レスポンス

##### 200 -- Picking item cancelled

| フィールド | 型 | 説明 | 例 |
|---|---|---|---|
| `is_success` | boolean | 成功フラグ | `true` |
| `code` | string | レスポンスコード | `SUCCESS` |
| `result.data.id` | integer | ピッキング結果ID | `1` |
| `result.data.picked_qty` | number | ピッキング済数量（リセット後） | `0` |
| `result.data.shortage_qty` | number | 欠品数量（リセット後） | `0` |
| `result.data.status` | string | 更新後ステータス | `PENDING` |
| `result.message` | string | メッセージ | `Picking item cancelled` |
| `result.debug_message` | string (nullable) | デバッグメッセージ | `null` |

##### 404 -- Item result not found

##### 422 -- Item cannot be cancelled (already completed)

---

## エンドポイント一覧（クイックリファレンス）

| # | メソッド | パス | 概要 | タグ |
|---|---|---|---|---|
| 1-1 | POST | `/api/auth/login` | ログイン | Authentication |
| 1-2 | POST | `/api/auth/logout` | ログアウト | Authentication |
| 1-3 | GET | `/api/me` | 現在のピッカー情報取得 | Authentication |
| 2-1 | GET | `/api/master/warehouses` | 倉庫マスタ一覧取得 | Master Data |
| 3-1 | GET | `/api/incoming/schedules` | 入庫予定一覧取得 | Incoming |
| 3-2 | GET | `/api/incoming/schedules/{id}` | 入庫予定詳細取得 | Incoming |
| 3-3 | GET | `/api/incoming/work-items` | 作業データ一覧取得 | Incoming |
| 3-4 | POST | `/api/incoming/work-items` | 入荷作業開始 | Incoming |
| 3-5 | PUT | `/api/incoming/work-items/{id}` | 作業データ更新 | Incoming |
| 3-6 | DELETE | `/api/incoming/work-items/{id}` | 作業キャンセル | Incoming |
| 3-7 | POST | `/api/incoming/work-items/{id}/complete` | 入荷作業完了 | Incoming |
| 3-8 | GET | `/api/incoming/locations` | ロケーション検索 | Incoming |
| 4-1 | GET | `/api/picking/tasks` | ピッキングタスク一覧取得 | Picking Tasks |
| 4-2 | GET | `/api/picking/tasks/{id}` | ピッキングタスク単体取得 | Picking Tasks |
| 4-3 | GET | `/api/picking/items/{id}` | ピッキング商品結果単体取得 | Picking Tasks |
| 4-4 | POST | `/api/picking/tasks/{id}/start` | ピッキングタスク開始 | Picking Tasks |
| 4-5 | POST | `/api/picking/tasks/{wms_picking_item_result_id}/update` | ピッキング結果更新 | Picking Tasks |
| 4-6 | POST | `/api/picking/tasks/{id}/complete` | ピッキングタスク完了 | Picking Tasks |
| 4-7 | POST | `/api/picking/tasks/{wms_picking_item_result_id}/cancel` | ピッキング結果キャンセル | Picking Tasks |
