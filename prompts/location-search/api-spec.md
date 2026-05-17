# Handy ロケ検索 API 共有仕様

## 目的

倉庫選択後、Android Handy 端末で商品CD・商品名・JAN・社内JANを検索し、選択倉庫内だけの商品の基本情報・在庫状況・ロケーション情報を表示する。

価格系情報（単価、税、原価、標準売価など）はAPIレスポンスに含めない。

## 参照元

- WMS Swagger JSON: `/Users/jungsinyu/Projects/sakemaru-wms/storage/api-docs/api-docs.json`
- WMS 詳細仕様: `/Users/jungsinyu/Projects/sakemaru-wms/storage/specifications/handy-location-search-api.md`
- Android 共通API仕様: `prompts/api.md`

## 認証

既存APIと同じ。

```http
X-API-Key: {local.properties の WMS_API_KEY}
Authorization: Bearer {POST /api/auth/login で取得した token}
Accept: application/json
```

## エンドポイント

`GET /api/master/item-locations`

### Query

| name | required | type | description |
| --- | --- | --- | --- |
| `warehouse_id` | yes | integer | 選択中の倉庫ID |
| `search` | yes | string | 商品CD、商品名、JAN、社内JAN |
| `limit` | no | integer | 最大商品件数。デフォルト10、最大50 |

### 検索対象

- `items.code`
- `items.name`
- `item_search_information.search_string`
- `item_search_information.search_string` の13桁ゼロ埋め一致
- `item_quantity_information.product_code`
- `item_quantity_information.product_code` の13桁ゼロ埋め一致
- `item_quantity_information.own_code`
- `item_quantity_information.own_code` の13桁ゼロ埋め一致

## Response

成功時は既存API共通形式。

```json
{
  "is_success": true,
  "code": "SUCCESS",
  "result": {
    "data": [
      {
        "item": {
          "id": 123,
          "code": "10001",
          "name": "商品A 720ml",
          "kana": "ショウヒンエー",
          "volume": "720",
          "volume_unit": "ML",
          "capacity_case": 12,
          "capacity_carton": null,
          "packaging": "瓶",
          "temperature_type": "NORMAL",
          "uses_expiration_date": true,
          "images": [],
          "search_codes": [
            {
              "code": "4901234567890",
              "code_type": "JAN",
              "quantity_type": "PIECE",
              "priority": 1
            }
          ],
          "jan_codes": ["4901234567890"],
          "item_quantity_codes": [
            {
              "product_code": "100010000",
              "own_code": "10001",
              "quantity_code": "00",
              "quantity": 1,
              "can_order": true
            }
          ]
        },
        "warehouse": {
          "id": 91,
          "code": "91",
          "name": "華むすびの蔵センター",
          "kana_name": "ハナムスビノクラセンター"
        },
        "stock": {
          "status": "IN_STOCK",
          "has_stock": true,
          "lot_count": 2,
          "location_count": 1,
          "current_quantity": 24,
          "reserved_quantity": 4,
          "available_quantity": 20,
          "earliest_expiration_date": "2026-08-31",
          "latest_expiration_date": "2026-09-30"
        },
        "locations": {
          "suggested": {
            "id": 456,
            "warehouse_id": 91,
            "floor_id": 1,
            "code": "A-1-01",
            "display_name": "A-1-01 常温棚A",
            "name": "常温棚A",
            "source": "item_default",
            "is_no_location": false
          },
          "default": {
            "id": 456,
            "warehouse_id": 91,
            "floor_id": 1,
            "code": "A-1-01",
            "display_name": "A-1-01 常温棚A",
            "name": "常温棚A",
            "source": "item_default",
            "is_no_location": false
          },
          "stock": [
            {
              "id": 789,
              "warehouse_id": 91,
              "floor_id": 1,
              "code": "B-2-03",
              "display_name": "B-2-03 冷蔵棚B",
              "name": "冷蔵棚B",
              "source": "stock_lot",
              "is_no_location": false,
              "lot_count": 2,
              "current_quantity": 24,
              "reserved_quantity": 4,
              "available_quantity": 20
            }
          ]
        }
      }
    ]
  }
}
```

## 在庫ステータス

| value | description |
| --- | --- |
| `IN_STOCK` | 指定倉庫で引当可能数がある |
| `RESERVED_ONLY` | 現在庫はあるが、全数引当済み |
| `NO_STOCK` | 指定倉庫に有効在庫ロットがない |

数量はすべて `warehouse_id` で指定した倉庫のみの合計。

## Android 実装メモ

- 倉庫選択後に呼ぶ。`warehouse_id` は選択中倉庫を渡す。
- バーコードスキャン値は加工せず `search` に渡す。
- `result.data` が0件なら「該当商品なし」。
- 複数件なら商品選択リストを出す。
- 商品詳細は `item` / `stock` / `locations` の3ブロックで表示する。
- 推奨ロケは `locations.suggested`。在庫ロケ一覧は `locations.stock`。
- `locations.suggested.is_no_location` が `true` の場合は、大きいロケ表示を `code` ではなく「フリーロケ」にする。`id` と `code` は実在ロケーション値として保持する。
- 入荷作業のロケ更新には既存 `PUT /api/incoming/work-items/{id}` の `location_id` を使う。
