# 出庫検品 API仕様書

作成日: 2026-07-26
対象: HANDYアプリの新機能「出庫検品」
サーバ実装対象: `sakemaru-wms`
アプリ実装対象: `sakemaru-handy-denso`

## 目的

HANDY端末で商品をスキャンし、WMSの当日営業出荷ピッキングリストに含まれる商品かどうかを確認する。
このAPIは検品用スナップショットの取得専用とし、WMS側の既存ピッキング結果、出荷、在庫、履歴、ログは更新しない。

## 確定事項

| 項目 | 内容 |
|---|---|
| 機能名 | 出庫検品 |
| 対象倉庫 | 91番倉庫のみ |
| 対象日 | WMS営業日 `ClientSetting::systemDateYMD()` |
| 対象出荷 | 営業出荷のみ |
| 対象外 | 物流出荷、倉庫間移動、ITFコード、削除済み検索コード |
| 取得タイミング | アプリで午前/午後を選択した時点で全件取得 |
| 午前/午後判定 | `wms_wave_groups.created_at < 12:00` が午前、`>= 12:00` が午後 |
| 複数WaveGroup | 同一営業日・同一時間帯の最新WaveGroupのみ採用 |
| リスト基準 | `/admin/waves` の保存済み `secondary_v2`、つまり「2次リスト2」相当 |
| フロア区分 | `1F` / `2F` / `YX` |
| YX判定 | 棚番 `code1` が `YA` / `YB` / `YC` / `YX` 始まりの場合は `YX` |
| 数量表示 | 注文数量とピッキング数量の両方を表示 |
| 数量内訳 | ケース、バラ、総バラを表示 |
| JAN照合 | JAN、SDP、OTHERを対象。ITFは除外 |
| 商品コード照合 | `items.code` はスキャン対象に含めない |
| 更新処理 | なし。WMS DBの業務データ更新なし |
| 権限 | 既存HANDYログインユーザー全員に許可 |
| オフライン | データ取得後は通信なしでスキャン可能 |

## 既存API規格

既存アプリAPIは `ApiEnvelope` 形式で統一されている。
出庫検品APIも同じ形式を使う。

```json
{
  "is_success": true,
  "code": "SUCCESS",
  "result": {
    "data": {},
    "message": "",
    "debug_message": null
  }
}
```

エラー時も既存 `ApiController::error()` 形式に合わせる。

```json
{
  "is_success": false,
  "code": "OUTBOUND_INSPECTION_SNAPSHOT_NOT_FOUND",
  "result": {
    "data": null,
    "error_message": "対象の出庫検品データがありません"
  }
}
```

## エンドポイント

```http
GET /api/outbound-inspections/snapshot
```

### 認証

既存HANDY APIと同じ。

| ヘッダー | 必須 | 内容 |
|---|---:|---|
| `X-API-Key` | 必須 | WMS APIキー |
| `Authorization` | 必須 | `Bearer {sanctum_token}` |
| `Accept` | 任意 | `application/json` |

### Query Parameters

| パラメータ | 必須 | 型 | 値 | 説明 |
|---|---:|---|---|---|
| `warehouse_id` | 必須 | integer | `91` | 91番倉庫限定。91以外はエラー |
| `period` | 必須 | string | `morning` / `afternoon` | 午前/午後選択 |

### Request Example

```http
GET /api/outbound-inspections/snapshot?warehouse_id=91&period=morning
X-API-Key: {api_key}
Authorization: Bearer {token}
Accept: application/json
```

## 成功レスポンス

### トップレベル

| フィールド | 型 | 説明 |
|---|---|---|
| `warehouse` | object | 対象倉庫 |
| `business_date` | string | WMS営業日 `YYYY-MM-DD` |
| `period` | string | `morning` / `afternoon` |
| `period_label` | string | `午前` / `午後` |
| `generated_at` | string | APIレスポンス生成日時 ISO-8601 |
| `source` | object | 参照元WaveGroup情報 |
| `courses` | array | 配送コース一覧 |
| `summary` | object | 件数サマリ |

### `source`

| フィールド | 型 | 説明 |
|---|---|---|
| `wave_group_id` | integer | 最新WaveGroup ID |
| `group_no` | string | WaveGroup番号 |
| `shipping_date` | string | 出荷日 |
| `created_at` | string | WaveGroup生成日時 |
| `list_type` | string | 固定値 `secondary_v2` |
| `wave_ids` | array<int> | 対象Wave ID一覧 |

### `courses[]`

| フィールド | 型 | 説明 |
|---|---|---|
| `delivery_course_id` | integer | 配送コースID |
| `delivery_course_code` | string | 配送コースコード |
| `delivery_course_name` | string | 配送コース名 |
| `floors` | array | `1F` / `2F` / `YX` 単位の明細 |
| `summary` | object | コース内サマリ |

### `floors[]`

| フィールド | 型 | 説明 |
|---|---|---|
| `floor_key` | string | `1F` / `2F` / `YX` |
| `floor_label` | string | 画面表示名 |
| `floor_sort` | integer | 1F=1、2F=2、YX=999 |
| `items` | array | スキャン照合対象の商品 |
| `summary` | object | フロア内サマリ |

### `items[]`

| フィールド | 型 | 説明 |
|---|---|---|
| `inspection_item_id` | string | アプリ内一意キー。例 `91-2421-100-1F-12345-10` |
| `item_id` | integer | 商品ID |
| `item_code` | string | 商品コード。表示用のみ。スキャン照合には使わない |
| `item_name` | string | 商品名 |
| `packaging` | string|null | 規格・荷姿 |
| `capacity_case` | integer | ケース入数。NULL/0の場合は1として扱う |
| `capacity_carton` | integer|null | ボール入数。今回は表示計算では必須にしない |
| `location` | object | 棚番情報 |
| `ordered_quantity` | object | 注文数量 |
| `planned_quantity` | object | ピッキング数量 |
| `scan_codes` | array | 照合対象コード |
| `source` | object | 元明細ID |

### `quantity`

数量は注文数量・ピッキング数量とも同じ構造にする。

| フィールド | 型 | 説明 |
|---|---|---|
| `quantity` | integer | 元数量 |
| `quantity_type` | string | `CASE` / `PIECE` / `CARTON` |
| `case_qty` | integer | ケース表示数量 |
| `piece_qty` | integer | バラ表示数量 |
| `total_piece_qty` | integer | 総バラ数 |

換算ルールはWMS側で計算済みを返す。

| `quantity_type` | `total_piece_qty` | `case_qty` / `piece_qty` |
|---|---|---|
| `CASE` | `quantity * capacity_case` | `case_qty=quantity`, `piece_qty=0` |
| `PIECE` | `quantity` | `case_qty=intdiv(quantity, capacity_case)`, `piece_qty=quantity % capacity_case` |
| `CARTON` | `quantity * capacity_carton` | 初期表示では総バラ中心。必要な場合だけ表示拡張 |

### `scan_codes[]`

| フィールド | 型 | 説明 |
|---|---|---|
| `code` | string | `item_search_information.search_string` を文字列で返す。先頭0を落とさない |
| `code_type` | string | `JAN` / `SDP` / `OTHER` |
| `quantity_type` | string|null | 検索コードの数量区分 |
| `priority` | integer|null | 既存優先度 |

サーバは `code` を加工せず、文字列で返す。
アプリ側で全角半角、ハイフン、空白、先頭ゼロ差分を正規化して照合する。

## Response Example

```json
{
  "is_success": true,
  "code": "SUCCESS",
  "result": {
    "data": {
      "warehouse": {
        "id": 91,
        "code": "091",
        "name": "華むすびの蔵センター"
      },
      "business_date": "2026-07-26",
      "period": "morning",
      "period_label": "午前",
      "generated_at": "2026-07-26T10:20:30+09:00",
      "source": {
        "wave_group_id": 2421,
        "group_no": "WG-20260726-ABCDEFGH",
        "shipping_date": "2026-07-26",
        "created_at": "2026-07-26T09:30:00+09:00",
        "list_type": "secondary_v2",
        "wave_ids": [10001, 10002]
      },
      "courses": [
        {
          "delivery_course_id": 100,
          "delivery_course_code": "C910017",
          "delivery_course_name": "服部　卓哉",
          "floors": [
            {
              "floor_key": "1F",
              "floor_label": "1F",
              "floor_sort": 1,
              "items": [
                {
                  "inspection_item_id": "91-2421-100-1F-12345-10",
                  "item_id": 12345,
                  "item_code": "ITEM001",
                  "item_name": "テスト商品",
                  "packaging": "720ml",
                  "capacity_case": 12,
                  "capacity_carton": 1,
                  "location": {
                    "location_id": 10,
                    "location_code": "A-01-01",
                    "floor_id": 1,
                    "floor_name": "1F"
                  },
                  "ordered_quantity": {
                    "quantity": 1,
                    "quantity_type": "CASE",
                    "case_qty": 1,
                    "piece_qty": 0,
                    "total_piece_qty": 12
                  },
                  "planned_quantity": {
                    "quantity": 10,
                    "quantity_type": "PIECE",
                    "case_qty": 0,
                    "piece_qty": 10,
                    "total_piece_qty": 10
                  },
                  "scan_codes": [
                    {
                      "code": "04901234567890",
                      "code_type": "JAN",
                      "quantity_type": "PIECE",
                      "priority": 1
                    },
                    {
                      "code": "901234567890",
                      "code_type": "OTHER",
                      "quantity_type": "PIECE",
                      "priority": 2
                    }
                  ],
                  "source": {
                    "wms_picking_item_result_ids": [900001, 900002],
                    "source_type": "EARNING"
                  }
                }
              ],
              "summary": {
                "item_count": 1,
                "total_case_qty": 0,
                "total_piece_qty": 10,
                "total_pieces": 10
              }
            }
          ],
          "summary": {
            "floor_count": 1,
            "item_count": 1,
            "total_pieces": 10
          }
        }
      ],
      "summary": {
        "course_count": 1,
        "floor_count": 1,
        "item_count": 1,
        "scan_code_count": 2
      }
    },
    "message": "出庫検品データを取得しました",
    "debug_message": null
  }
}
```

## エラーコード

| HTTP | `code` | 条件 | アプリ表示 |
|---:|---|---|---|
| 400 | `VALIDATION_ERROR` | `warehouse_id` / `period` 不正 | APIエラー内容を表示 |
| 403 | `OUTBOUND_INSPECTION_WAREHOUSE_NOT_ALLOWED` | 91以外の倉庫 | 対象倉庫ではありません |
| 404 | `OUTBOUND_INSPECTION_SNAPSHOT_NOT_FOUND` | 対象WaveGroupなし | 対象の出庫検品データがありません |
| 500 | `OUTBOUND_INSPECTION_SNAPSHOT_ERROR` | 想定外エラー | データ取得に失敗しました |

## WMS側抽出設計

サーバ実装時の想定。現時点ではサーバコードを変更しない。

### 最新WaveGroup選定

```php
$businessDate = ClientSetting::systemDateYMD();
$period = $request->query('period');

$query = WaveGroup::query()
    ->where('warehouse_id', 91)
    ->whereDate('shipping_date', $businessDate)
    ->whereNull('cancelled_at')
    ->whereJsonContains('target_document_types', 'shipment')
    ->whereRaw("COALESCE(JSON_EXTRACT(generation_result, '$.earning_count'), 0) > 0");

if ($period === 'morning') {
    $query->whereTime('created_at', '<', '12:00:00');
} else {
    $query->whereTime('created_at', '>=', '12:00:00');
}

$waveGroup = $query
    ->orderByDesc('created_at')
    ->orderByDesc('id')
    ->first();
```

### 明細抽出

基準は `PickingListService::generateCourseGroupedListV2ByWaveIds()`。
ただし出庫検品では以下を追加する。

| 項目 | 方針 |
|---|---|
| 営業出荷限定 | `pir.source_type = 'EARNING'` かつ `pir.earning_id IS NOT NULL` |
| 物流出荷除外 | `pir.stock_transfer_id IS NULL` |
| 数量 | `ordered_qty` / `ordered_qty_type` と `planned_qty` / `planned_qty_type` を両方取得 |
| JANコード | `item_search_information` から `JAN` / `SDP` / `OTHER` を取得 |
| ITF除外 | `code_type != 'ITF'` |
| 削除済み除外 | `is_active = 1` を条件にする |
| 先頭0 | `search_string` は数値キャストせず文字列として返す |
| 集約 | 配送コース × フロアキー × 商品ID × ロケーションID 単位 |
| 並び | 1F、2F、YX。ただしアプリではリスト表示しないため順序は選択肢表示と内部安定化目的 |

### 集約単位

2次リスト2では `配送コース × フロア/YX × ロケーション × 商品` で数量合算している。
出庫検品APIでも同じ思想にする。

| 集約キー | 理由 |
|---|---|
| `delivery_course_id` | アプリで配送コース選択するため |
| `floor_key` | アプリで `1F` / `2F` / `YX` を選択するため |
| `item_id` | スキャン結果の商品特定単位 |
| `location_id` | 同一商品が複数棚にある場合の表示混乱を避けるため |
| `planned_qty_type` / `ordered_qty_type` | 数量換算の一貫性維持 |

同一配送コース・同一フロアで同一JANが複数商品に紐づく場合、APIはそのまま返す。
アプリ側で選択コース・選択フロア内の照合時に複数商品ヒットとして `JANが複数商品にひもづいています。` を表示する。

## アプリ側契約

アプリは以下だけをAPIに依存する。

| 契約 | 内容 |
|---|---|
| 取得単位 | 午前/午後選択時に1回だけ全件取得 |
| 検索範囲 | 選択中の配送コース + 選択中の `floor_key` の `items[].scan_codes[]` |
| 正規化 | アプリ側でスキャン値とAPIコードの両方に実施 |
| 照合結果 | 0件=商品違い、1件=成功、複数商品=JAN重複エラー |
| 更新 | APIへの送信なし |
| 再取得 | 自動更新なし。午前/午後を選び直した場合のみ再取得 |

## アプリ正規化仕様

以下の正規化キーを作り、照合する。

| 正規化 | 内容 |
|---|---|
| 全角半角 | 数字・英字を半角化 |
| 空白 | 半角/全角スペース除去 |
| ハイフン | `-` / `ー` / `−` / `‐` を除去 |
| 大文字小文字 | 英字は大文字化 |
| 先頭0 | 元コードに加えて、先頭0を除去したコードでも照合 |

例: `０４９０-１２３４ ５６７８９０` は `04901234567890` と `4901234567890` の両方でヒット可能にする。

## ローカル仮データ

サーバ実装前はアプリ側で `FakeOutboundInspectionRepository` を使う。
仮データはこのAPIレスポンスと同じドメイン構造に変換できる形で保持する。

ローカル仮データに必ず含めるケース。

| ケース | 目的 |
|---|---|
| 1F正常商品 | 成功表示確認 |
| 2F正常商品 | フロア選択確認 |
| YX正常商品 | YX判定確認 |
| 対象外JAN | 商品違いエラー確認 |
| 先頭0ありJAN | 0あり/なし両ヒット確認 |
| 全角・ハイフン入りJAN | 正規化確認 |
| 同一JAN複数商品 | JAN重複エラー確認 |
| スキャン済み商品 | 再スキャン表示確認 |

## 非対象

| 項目 | 理由 |
|---|---|
| WMSデータ更新 | 今回は誤ピック防止の確認機能のみ |
| 検品履歴保存 | 要件で不要 |
| アクセスログ追加 | 要件で不要 |
| オフライン永続保存 | アプリ再起動でリセットでよい |
| サーバ側コード修正 | 現在は他修正が進行中のため禁止 |
