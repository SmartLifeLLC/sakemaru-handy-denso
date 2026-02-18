# スキル設置手順

別のPCでこのプロジェクトのClaude Codeスキルを使えるようにするための手順。

## 前提

- Claude Code がインストール済み
- このリポジトリが clone 済み

## 1. ral（プロジェクトコマンド）

リポジトリの `.claude/commands/ral.md` に含まれているため、**git clone するだけで使用可能**。追加作業不要。

## 2. android-plan（グローバルスキル）

ユーザーの `~/.claude/skills/` に設置が必要。

### 自動設置（Claude Codeに以下を貼り付けて実行）

```
以下のコマンドを実行して android-plan スキルを設置してください:

mkdir -p ~/.claude/skills/android-plan

次に、以下の内容で ~/.claude/skills/android-plan/SKILL.md を作成してください:
このリポジトリの prompts/setup-skills.md の末尾「SKILL.md 全文」セクションの内容をそのまま使用してください。
```

### 手動設置

```bash
mkdir -p ~/.claude/skills/android-plan
cp prompts/skill-android-plan.md ~/.claude/skills/android-plan/SKILL.md
```

## 3. 設置確認

Claude Code を起動して `/android-plan テスト` と入力し、スキルが認識されることを確認。

## 4. local.properties の設定

認証情報はリポジトリに含まれないため、プロジェクトルートの `local.properties` に以下のキーを追記する。
値はチームメンバーから別途共有を受けること。

```properties
WMS_API_KEY=
API_TEST_USER=
API_TEST_PW=
SWAGGER_USER=
SWAGGER_PW=
```
