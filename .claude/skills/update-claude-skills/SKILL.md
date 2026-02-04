---
name: update-claude-skills
description: Guide for reviewing and updating CLAUDE.md and SKILL.md files when they become outdated. Use when the codebase has changed and documentation may no longer reflect the actual project structure, build commands, module layout, or coding conventions.
---

# CLAUDE.md / SKILL.md 更新ガイド

コードベースの変更に伴い、CLAUDE.md や SKILL.md の内容が実態と乖離していないかを確認し、必要に応じて更新する。

---

## 対象ファイル

| ファイル | 内容 |
|---|---|
| `CLAUDE.md` | プロジェクト概要、Tech Stack、モジュール構成、ビルド・テスト手順 |
| `docs/*` | CLAUDE.mdから分割された内容 |
| `.claude/skills/*/SKILL.md` | 各スキルの手順書 |

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
