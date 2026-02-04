---
name: update-claude-skills
description: Guide for reviewing and updating CLAUDE.md and SKILL.md files when they become outdated. Use when the codebase has changed and documentation may no longer reflect the actual project structure, build commands, module layout, or coding conventions.
---

# CLAUDE.md / SKILL.md 更新ガイド

コードベースの変更に伴い、CLAUDE.md や SKILL.md の内容が実態と乖離していないかを確認し、必要に応じて更新する手順。

---

## 対象ファイル

| ファイル | 内容 |
|---|---|
| `CLAUDE.md` | プロジェクト概要、Tech Stack、モジュール構成、ビルド・テスト手順 |
| `docs/coding_style.md` | コーディングガイドライン |
| `.claude/skills/*/SKILL.md` | 各スキルの手順書 |

---

## 手順

### 1. 実態の調査

まずコードベースの現状を調査し、ドキュメントとの差分を把握する。

#### モジュール構成の確認

```sh
# backend / frontend / shared のディレクトリ構成を確認
find backend -maxdepth 3 -type d | head -50
find frontend -maxdepth 4 -type d | head -50
ls shared/
ls build-logic/
```

#### Tech Stack の確認

```sh
# build.gradle.kts や version catalog から依存ライブラリを確認
cat gradle/libs.versions.toml
```

#### ビルドコマンドの確認

```sh
# Gradle タスク一覧から利用可能なタスクを確認
./gradlew tasks --all 2>/dev/null | grep -E "assemble|build|test|ktlint|generate"
```

#### GraphQL スキーマファイルの確認

```sh
ls backend/graphql/src/commonMain/resources/graphql/
```

#### CI/CD の確認

```sh
ls .github/workflows/
```

### 2. CLAUDE.md の各セクションを検証

以下の各セクションについて、調査結果と照合する。

| セクション | 確認ポイント |
|---|---|
| プロジェクトについて | アプリの説明が現状と合っているか |
| Tech Stack | 使用ライブラリ・フレームワークが正しいか |
| Module Structure | ディレクトリ構成が実際と一致しているか。新規モジュールが追加・削除されていないか |
| メールのパース | パーサーのパスが正しいか |
| GraphQL スキーマ | スキーマファイル一覧が正しいか |
| FIDO認証 | 関連ファイルが存在し、説明が正しいか |
| ビルドとテスト | ビルド・テスト・フォーマットのコマンドが正しく動作するか |
| コード生成 | コード生成コマンドが正しいか |
| CI/CD | ワークフロー一覧が正しいか |

### 3. SKILL.md の各セクションを検証

各 `.claude/skills/*/SKILL.md` について以下を確認する。

| 確認ポイント | 詳細 |
|---|---|
| 参照パスの存在確認 | SKILL.md 内で参照しているファイルパスが実在するか |
| インターフェースの整合性 | コード内のインターフェース定義と SKILL.md のテンプレートが一致するか |
| 登録手順 | 登録先ファイルの構造が変わっていないか |
| ユーティリティ | 記載されているメソッドが実在し、シグネチャが正しいか |

#### 確認コマンド例

```sh
# SKILL.md 内のパスを抽出して存在確認
grep -oP '`[^`]*\.(kt|graphqls|toml)`' .claude/skills/*/SKILL.md | while read -r path; do
  clean=$(echo "$path" | tr -d '`')
  if [ ! -f "$clean" ]; then
    echo "NOT FOUND: $clean"
  fi
done
```

### 4. 更新の実施

差分が見つかった場合、以下の方針で更新する。

- **追加**: 新しいモジュール、ファイル、コマンドがある場合は追記
- **削除**: 存在しなくなったモジュール、ファイル、コマンドは削除
- **修正**: パス、コマンド、インターフェース定義などが変わっている場合は修正
- **重複排除**: 同じ内容が複数箇所に書かれている場合は一箇所にまとめる

#### 更新時の注意点

- CLAUDE.md の `@docs/coding_style.md` のようなインクルード参照は維持する
- SKILL.md の frontmatter（`name`, `description`）は必ず残す
- コーディングガイドラインの内容自体は、コードベースの慣習が変わった場合のみ更新する
- 日本語で記述する（コメント・ドキュメントの言語方針に従う）

### 5. 検証

更新後、記載されているビルドコマンドやパスが正しいことを改めて確認する。

```sh
# ビルドコマンドが動作するか確認
./gradlew assemble assembleDebug -x jsBrowserProductionWebpack
```

---

## よくある乖離パターン

| パターン | 例 |
|---|---|
| モジュール追加・削除 | 新しい feature モジュールが追加されたが CLAUDE.md に記載がない |
| パス変更 | パッケージのリネームにより SKILL.md 内のパスが無効になっている |
| インターフェース変更 | メソッドの引数が変わったが SKILL.md のテンプレートが古いまま |
| 依存ライブラリ変更 | Tech Stack に記載のライブラリが使われなくなった、または新規追加された |
| ビルドコマンド変更 | Gradle タスク名が変わった |
| GraphQL スキーマ変更 | スキーマファイルの追加・削除・リネーム |
| CI/CD 変更 | ワークフローの追加・削除・リネーム |
