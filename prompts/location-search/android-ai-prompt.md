# Android 開発AIプロンプト: ロケ検索機能

## 依頼

Sakemaru Handy Androidアプリに、倉庫選択後の商品ロケ検索機能を実装してください。

## 必読ファイル

作業前に必ず読む:

1. `prompts/api.md`
2. `prompts/design.md`
3. `prompts/pages.md`
4. `prompts/location-search/api-spec.md`

認証情報・接続先は `local.properties` を参照し、コミットしない。

## 使用API

既存認証:

- `POST /api/auth/login`
- `GET /api/master/warehouses`

追加API:

- `GET /api/master/item-locations?warehouse_id={warehouseId}&search={keyword}&limit=10`

認証ヘッダー:

```http
X-API-Key: {WMS_API_KEY}
Authorization: Bearer {token}
Accept: application/json
```

## 機能要件

- 倉庫選択済みの状態でロケ検索画面を開く。
- バーコードスキャン、商品CD、商品名、JAN、社内JANで検索できる。
- APIには選択中倉庫IDだけを渡し、結果も該当倉庫だけを表示する。
- 商品基本情報、在庫状況、ロケ情報を表示する。
- 単価・税・原価・標準売価など価格系情報は画面にもモデルにも持たせない。
- 複数商品が返った場合は商品選択リストを表示する。
- 0件の場合は「該当商品なし」を表示する。
- `stock.status` を画面上で判別できるようにする。
- `locations.suggested` を推奨ロケとして最上段に表示する。
- `locations.suggested.is_no_location == true` の場合は、推奨ロケの大表示を `code` ではなく「フリーロケ」にする。登録や後続処理では `id` と `code` は実在ロケーションの値として扱う。
- `locations.stock` を在庫ロケ一覧として表示し、ロケ別の `available_quantity` を表示する。

## 画面構成

画面制約は既存設計に合わせる:

- 320 x 534, 160dpi
- portrait固定
- Compose
- Scaffold + TopAppBar + FunctionKeyBar
- DENSO Handy実機で操作しやすい大きさ

推奨画面:

1. 倉庫選択済みホームまたはメニューから「ロケ検索」へ遷移
2. ロケ検索画面
   - 検索入力
   - スキャン入力受付
   - 商品候補リスト
3. 商品ロケ詳細画面または同画面内詳細パネル
   - 商品基本情報
   - 在庫状況
   - 推奨ロケ
   - 在庫ロケ一覧

## APIモデル

レスポンスは `result.data[]` に以下の構造で来る:

- `item`
- `warehouse`
- `stock`
- `locations`

詳細なフィールドは `prompts/location-search/api-spec.md` を正とする。

## 実装手順

1. API疎通確認
   - login
   - warehouse list
   - item-locations
   - 可能なら商品CD、JAN、`item_quantity_information.product_code` / `own_code` の検索をそれぞれ確認
2. `core/network` にAPI DTOとエンドポイントを追加
3. `core/domain` に画面用モデルを追加
4. Repository / UseCase を既存パターンに合わせて追加
5. `feature` 配下にロケ検索画面を追加
6. ルーティング・メニュー導線を追加
7. APIエラー、0件、複数件、在庫なし、推奨ロケなしの状態を実装
8. `prompts/pages.md` に画面一覧・遷移を追記

## テスト

- APIテスト結果を作業ディレクトリの `error.log` または `boot.md` に記録する。
- 正常系:
  - 倉庫選択後に商品CDで検索できる
  - JANで検索できる
  - 社内JAN `product_code` / `own_code` で検索できる
  - 在庫状況とロケ一覧が表示される
- 異常系:
  - 0件
  - API 401
  - API 422
  - ネットワークエラー

## 完了条件

- ロケ検索画面が実機想定サイズで使える。
- 追加APIを既存認証方式で呼べる。
- 商品基本情報・在庫状況・ロケ情報が表示される。
- 価格系情報を表示しない。
- `prompts/pages.md` が更新されている。
- ビルドと該当テストが通っている。
