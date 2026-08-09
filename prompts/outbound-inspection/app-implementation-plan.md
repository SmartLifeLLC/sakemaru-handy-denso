# 出庫検品 アプリ実装計画

作成日: 2026-07-26
対象リポジトリ: `sakemaru-handy-denso`
作業ブランチ: `codex/outbound-inspection-app`

## 作業方針

アプリ側から実装を進める。
サーバ側 `sakemaru-wms` は当面コード修正しない。
APIは本番想定の既存API規格 `ApiEnvelope` で設計し、ローカルでは同じドメイン構造の仮データでテストする。

## スコープ

| 区分 | 対象 |
|---|---|
| 対象 | HANDYアプリの新機能 `出庫検品` |
| 対象 | API Interface / DTO / Repository interface / Fake Repository |
| 対象 | 午前午後選択、配送コース選択、フロア選択、スキャン画面 |
| 対象 | スキャン照合、数量表示、成功音、エラー音、振動 |
| 対象 | F1確認、F2戻る |
| 対象外 | WMSサーバコード変更 |
| 対象外 | WMS DB更新 |
| 対象外 | 検品履歴保存 |

## 前提

| 項目 | 内容 |
|---|---|
| API形式 | `core/network/model/ApiEnvelope.kt` の形式を使用 |
| 認証 | 既存の `X-API-Key` と Bearer token interceptor を使用 |
| 通信 | 本番API未実装のためRepositoryはFakeをバインド |
| 画面サイズ | DENSO BHT-M60想定、320dp幅 |
| デザイン | 全体フォントは既存方針に合わせて小さく、ラウンドなし、青系 |
| 既存機能 | 既存の出庫処理 P20-P22 は壊さない |

## 画面フロー

```text
メイン画面
  -> 出庫検品
  -> 午前/午後選択
  -> データ取得
  -> 配送コース選択
  -> 1F / 2F / YX 選択
  -> スキャン待機
  -> スキャン結果表示
  -> F1確認
  -> スキャン待機
```

リスト表示は不要。
スキャン画面では「今スキャンした商品が対象かどうか」と数量を表示する。

## ルート案

既存の `Routes.kt` に追加する。

| 画面 | route | 内容 |
|---|---|---|
| Period | `outbound_inspection_period` | 午前/午後選択 |
| Course | `outbound_inspection_course` | 配送コース選択 |
| Floor | `outbound_inspection_floor` | `1F` / `2F` / `YX` 選択 |
| Scan | `outbound_inspection_scan` | スキャン待機・結果表示 |

ViewModelは一連の画面で共有する。
既存入庫画面と同じく、最初のrouteのback stack entryにscoped ViewModelをぶら下げる。

## 推奨パッケージ構成

```text
app/src/main/java/biz/smt_life/android/sakemaru_handy_denso/outboundinspection/
  OutboundInspectionPeriodScreen.kt
  OutboundInspectionCourseScreen.kt
  OutboundInspectionFloorScreen.kt
  OutboundInspectionScanScreen.kt
  OutboundInspectionState.kt
  OutboundInspectionViewModel.kt
```

または既存feature分割に合わせる場合は以下。

```text
feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/inspection/
  ...
```

推奨は `feature/outbound/.../inspection`。
理由は既存の出庫処理と同一ドメインで、app module直下の機能追加を増やさないため。
ただし既存の棚卸し実装が `app/.../inventory` にあるため、短期実装優先なら `app/.../outboundinspection` でも可。

## データ層設計

### Domain Models

```kotlin
data class OutboundInspectionSnapshot(
    val warehouse: OutboundInspectionWarehouse,
    val businessDate: String,
    val period: OutboundInspectionPeriod,
    val periodLabel: String,
    val source: OutboundInspectionSource,
    val courses: List<OutboundInspectionCourse>
)

data class OutboundInspectionCourse(
    val deliveryCourseId: Int,
    val deliveryCourseCode: String,
    val deliveryCourseName: String,
    val floors: List<OutboundInspectionFloor>
)

data class OutboundInspectionFloor(
    val floorKey: String,
    val floorLabel: String,
    val floorSort: Int,
    val items: List<OutboundInspectionItem>
)

data class OutboundInspectionItem(
    val inspectionItemId: String,
    val itemId: Int,
    val itemCode: String,
    val itemName: String,
    val packaging: String?,
    val capacityCase: Int,
    val location: OutboundInspectionLocation,
    val orderedQuantity: OutboundInspectionQuantity,
    val plannedQuantity: OutboundInspectionQuantity,
    val scanCodes: List<OutboundInspectionScanCode>
)
```

### Repository

```kotlin
interface OutboundInspectionRepository {
    suspend fun getSnapshot(
        warehouseId: Int,
        period: OutboundInspectionPeriod
    ): Result<OutboundInspectionSnapshot>
}
```

初期実装では `FakeOutboundInspectionRepository` をHiltでバインドする。
本番API実装後に `OutboundInspectionRepositoryImpl` に切り替える。

### API Interface

サーバ未実装でも、規格を先に固定するためinterfaceは作成してよい。
ただしDIではFakeを使う。

```kotlin
interface OutboundInspectionApi {
    @GET("/api/outbound-inspections/snapshot")
    suspend fun getSnapshot(
        @Query("warehouse_id") warehouseId: Int,
        @Query("period") period: String
    ): ApiEnvelope<OutboundInspectionSnapshotResponse>
}
```

## 状態設計

```kotlin
data class OutboundInspectionUiState(
    val loadState: LoadState = LoadState.Idle,
    val selectedPeriod: OutboundInspectionPeriod? = null,
    val snapshot: OutboundInspectionSnapshot? = null,
    val selectedCourseId: Int? = null,
    val selectedFloorKey: String? = null,
    val scanState: ScanState = ScanState.Waiting,
    val scannedInspectionItemIds: Set<String> = emptySet()
)
```

### ScanState

| 状態 | 表示 | 音/振動 |
|---|---|---|
| `Waiting` | スキャン待機中 | なし |
| `Success` | 緑表示、商品名、注文数量、ピッキング数量 | 優しい成功音 |
| `NotFound` | `商品が違います` | エラー音 + 振動 |
| `DuplicateCode` | `JANが複数商品にひもづいています。` | エラー音 + 振動 |
| `AlreadyScanned` | 同じ数量を表示、赤字 `すでにスキャンしています。` | なし |

## スキャン照合仕様

検索範囲は選択中の配送コースと選択中のフロアのみ。
全コース横断検索はしない。

```text
scan raw value
  -> normalize(raw)
  -> selectedCourse.selectedFloor.items.flatMap(scanCodes)
  -> normalized code map lookup
  -> 0件: 商品が違います
  -> 1商品: 成功またはスキャン済み
  -> 2商品以上: JANが複数商品にひもづいています。
```

### 正規化

| 処理 | 内容 |
|---|---|
| 全角半角 | 数字・英字を半角化 |
| 空白 | 半角/全角スペース削除 |
| ハイフン | `-` / `ー` / `−` / `‐` 削除 |
| 大文字 | 英字を大文字化 |
| 先頭0 | 元コードと先頭0除去コードを両方index化 |

`items.code` はindex化しない。

## キー操作

| キー | 画面 | 動作 |
|---|---|---|
| F1 | スキャン結果表示中 | 確認して `Waiting` に戻る |
| F1 | スキャン待機中 | 何もしない |
| F2 | 全画面 | 戻る |
| 物理スキャンEnter | スキャン画面 | scan buffer確定 |

既存の `ScanKeyHandler` はEnter確定のscan bufferとして使える。
F1/F2は既存画面の物理キー処理を確認し、新画面で明示的に処理する。

## 音・振動

既存 `SoundUtils` を使う。

| 結果 | 処理 |
|---|---|
| 成功 | `SoundUtils.playSuccess()` |
| 商品違い | `SoundUtils.playErrorWithVibration(context)` |
| JAN重複 | `SoundUtils.playErrorWithVibration(context)` |
| スキャン済み | 音なし、振動なし |

成功音が強すぎる場合は後続で `playSoftSuccess()` を追加する。
初期実装では既存音源を使い、UIで緑表示を明確にする。

## UI方針

| 項目 | 方針 |
|---|---|
| 色 | 青系を主色にする |
| 角丸 | 使用しない。四角形ベース |
| 文字サイズ | 既存より小さめ。端末画面に収める |
| 成功表示 | 緑背景または緑帯で明確化 |
| エラー表示 | 赤背景または赤帯で明確化 |
| 数量表示 | 注文数量とピッキング数量を上下または左右に明確表示 |

## ローカル仮データ

Fake Repositoryに以下のデータを固定で持たせる。

| コース | フロア | 商品 | コード | 期待結果 |
|---|---|---|---|---|
| C910017 | 1F | 通常商品A | `04901234567890` | 成功 |
| C910017 | 1F | 通常商品A | `4901234567890` | 先頭0なし成功 |
| C910017 | 2F | 通常商品B | `490-2222-333333` | ハイフン正規化成功 |
| C910017 | YX | YX商品C | `４９０３３３３４４４４４４` | 全角正規化成功 |
| C910017 | 1F | 重複商品D/E | `4999999999999` | JAN重複エラー |
| C910017 | 1F | 対象外 | `1111111111111` | 商品違い |

## 実装項目リスト

| ID | 項目 | 完了条件 |
|---|---|---|
| APP-01 | `prompts/outbound-inspection/api-spec.md` 作成 | API契約が確認できる |
| APP-02 | `OutboundInspectionApi` 追加 | Retrofit interfaceがある |
| APP-03 | API Response DTO追加 | `ApiEnvelope` でdecodeできる |
| APP-04 | Domain Model追加 | UIがAPI DTOに依存しない |
| APP-05 | Repository interface追加 | Fake/Realを差し替え可能 |
| APP-06 | Fake Repository追加 | サーバなしで全画面テスト可能 |
| APP-07 | DIバインド追加 | 初期はFakeを使用 |
| APP-08 | ルート追加 | 4画面へ遷移できる |
| APP-09 | メイン画面に出庫検品導線追加 | 既存出庫処理と区別できる |
| APP-10 | 午前/午後選択画面 | 選択時にsnapshot取得 |
| APP-11 | 配送コース選択画面 | snapshot内のcourseを表示 |
| APP-12 | フロア選択画面 | `1F` / `2F` / `YX` の存在する選択肢を表示 |
| APP-13 | スキャン画面 | スキャン待機状態を表示 |
| APP-14 | 正規化関数 | 全角/空白/ハイフン/先頭0対応 |
| APP-15 | scan index構築 | 選択course/floor内のみ検索 |
| APP-16 | 成功判定 | 対象商品なら数量表示 |
| APP-17 | 商品違い判定 | エラー音+振動+表示 |
| APP-18 | JAN重複判定 | 専用エラー表示 |
| APP-19 | スキャン済み判定 | 数量再表示+赤字、音/振動なし |
| APP-20 | F1確認 | 結果表示から待機へ戻る |
| APP-21 | F2戻る | 各画面で戻る |
| APP-22 | UI調整 | 青系、四角、文字小さめ |
| APP-23 | Unit Test | 正規化・照合ロジックを検証 |
| APP-24 | ViewModel Test | 成功/エラー/再スキャンを検証 |
| APP-25 | 手動確認 | DENSO想定サイズで表示確認 |

## テストリスト

| ID | テスト | 期待結果 |
|---|---|---|
| T-01 | 午前選択 | Fake snapshot morningを取得 |
| T-02 | 午後選択 | Fake snapshot afternoonを取得 |
| T-03 | 取得失敗 | エラー画面または再試行表示 |
| T-04 | コース選択 | 選択コースが保持される |
| T-05 | 1F選択 | 1Fスキャン画面へ遷移 |
| T-06 | 2F選択 | 2Fスキャン画面へ遷移 |
| T-07 | YX選択 | YXスキャン画面へ遷移 |
| T-08 | 対象JANスキャン | 商品名と数量を緑表示 |
| T-09 | 対象外JANスキャン | `商品が違います`、音、振動 |
| T-10 | 同一JAN複数商品 | `JANが複数商品にひもづいています。` |
| T-11 | 同一商品再スキャン | 同じ数量、`すでにスキャンしています。`、音/振動なし |
| T-12 | 先頭0ありAPI/なしscan | ヒット |
| T-13 | 先頭0なしAPI/ありscan | ヒット |
| T-14 | ハイフン入りscan | ヒット |
| T-15 | 全角scan | ヒット |
| T-16 | 別コースの商品scan | 商品違い |
| T-17 | 別フロアの商品scan | 商品違い |
| T-18 | F1確認 | 待機状態に戻る |
| T-19 | F2戻る | 前画面に戻る |
| T-20 | 午前/午後選び直し | スキャン済み状態がリセット |
| T-21 | アプリ再起動 | スキャン済み状態がリセット |
| T-22 | オフライン相当 | 取得後は追加通信なしで照合可能 |

## 実装時の注意

| 注意 | 内容 |
|---|---|
| 既存出庫処理 | `PickingTasksScreen` / `OutboundPickingScreen` / `PickingHistoryScreen` の挙動を変えない |
| 既存API | `PickingApi` とは分離し、新しい `OutboundInspectionApi` にする |
| Fake切替 | Real実装を作ってもDIはFakeのままにするか、BuildConfigで切替可能にする |
| 文字サイズ | 既存画面の大きすぎる文字を避ける |
| ラウンド | `RoundedCornerShape` は新画面では使わない |
| サーバ | `/Users/jungsinyu/Projects/sakemaru-wms` は編集しない |

## 確認コマンド

GradleはJVM 17以上が必要。
現在端末にJVM 8しか見えていない場合はビルド前にJDK 17設定が必要。

```sh
/usr/libexec/java_home -V
./gradlew :app:assembleDebug
./gradlew test
```

