# DENSOハンディ デザイン仕様

## デバイス仕様

| 項目 | 値 |
|------|-----|
| 画面解像度 | 320 x 534 |
| 画面密度 | 160dpi |
| API Level | 33 (Android 13) |
| 画面方向 | portrait（縦固定） |

## テーマ・ツールバー制御

システム標準のActionBarは非表示。各画面でCompose独自ヘッダーを実装する。

**テーマ定義** (`app/src/main/res/values/themes.xml`)
```xml
<style name="Theme.Sakemaruhandydenso" parent="android:Theme.Material.Light.NoActionBar" />
```

| レイヤー | 制御方法 |
|---------|---------|
| システムActionBar | `themes.xml` で `NoActionBar` を指定して非表示 |
| 各画面ヘッダー | Compose の `Scaffold` + `TopAppBar` で独自実装 |
| 下部バー | `FunctionKeyBar` でファンクションキーを表示（入庫・出庫画面） |

## 画面構成パターン

各画面は `Scaffold` で以下の構成をとる:

```
┌─────────────────────┐
│ TopAppBar (ヘッダー)  │  ← タイトル、戻るボタン、アクション
├─────────────────────┤
│                     │
│  Content (本文)      │  ← スクロール可能な主要コンテンツ
│                     │
├─────────────────────┤
│ FunctionKeyBar      │  ← F1〜F4 ファンクションキー
└─────────────────────┘
```

## ファンクションキー共通仕様

| キー | 主な用途 |
|------|----------|
| F1 | 検索、自動入力などの補助機能 |
| F2 | 戻る、登録、リスト表示 |
| F3 | 履歴表示 |
| F4 | ログアウト |
