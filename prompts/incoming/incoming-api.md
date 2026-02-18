
# 入庫処理機能の作成

現在ログイン後に表示される入庫ボタンをクリックするとwebviewになるが、
次のサーバのAPIを利用して、native 入庫画面を作る。
画面仕様書、APIの仕様書は以下のファイルリストを確認する。

使うuser id / pw = 1/1
url : http://10.0.2.2:8000

When complete:
- 入庫処理に必要なＡＰＩを全て利用
- 入稿処理に必要な全ての画面の実装が完了
- 画面による入庫処理が成功
- 全てのAPIの利用テストが成功

│ /Users/jungsinyu/Projects/sakemaru-wms/storage/api-docs/api-docs.json                                  │ Swagger仕様書    │                                                                                                                     
├─────────────────────────────────────────────────────────────────┼──────────────────┤                                                                                                                     
│ /Users/jungsinyu/Projects/sakemaru-wms/storage/specifications/api/incoming-api-android-prompt.md       │ Android API解説  │                                                                                                                     
├─────────────────────────────────────────────────────────────────┼──────────────────┤                                                                                                                     
│ /Users/jungsinyu/Projects/sakemaru-wms/storage/specifications/api/incoming-android-ui-specification.md │ Android UI仕様書 │                                                                                                                     
├─────────────────────────────────────────────────────────────────┼──────────────────┤                                                                                                                     
│ /Users/jungsinyu/Projects/sakemaru-wms/storage/specifications/api/incoming.md                          │ 完了報告サマリー │                                                                                                                     
└─────────────────────────────────────────────


| API | 説明 |
|-----|------|
| `POST /api/auth/login` | ログイン |
| `POST /api/auth/logout` | ログアウト |
| `GET /api/me` | 認証ユーザー情報取得 |
| `GET /api/master/warehouses` | 倉庫一覧取得 |
| `GET /api/incoming/schedules` | 入庫予定一覧取得 |
| `GET /api/incoming/schedules/{id}` | 入庫予定詳細取得 |
| `GET /api/incoming/work-items` | 作業データ一覧取得 |
| `POST /api/incoming/work-items` | 作業開始 |
| `PUT /api/incoming/work-items/{id}` | 作業データ更新 |
| `POST /api/incoming/work-items/{id}/complete` | 作業完了 |
| `DELETE /api/incoming/work-items/{id}` | 作業キャンセル |
| `GET /api/incoming/locations` | ロケーション検索 |
