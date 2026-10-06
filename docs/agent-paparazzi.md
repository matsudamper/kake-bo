# Paparazzi

フロントエンド（`frontend/common/ui`）の見た目を変える作業では、このドキュメントに従う。手順の詳細は `.claude/skills/update-paparazzi-screenshots/SKILL.md` も参照してよい。

## UI 変更時

- 対象画面に `@Preview` を追加または更新する
- `./gradlew :frontend:common:ui:recordPaparazziAndroidMain` でスナップショットを生成する
- 生成した参照 PNG（`frontend/common/ui/src/androidHostTest/snapshots/images/`）はリポジトリにコミットしない
- 作業完了前に、変更内容が分かる画像を提示する。画像なしで UI 変更を完了にしない

## エージェント向け（提出物）

- 生成した画像のコピーを `/opt/cursor/artifacts/` に置く（PR 用 walkthrough 用）
- PR 本文とユーザーへの完了報告に、その画像を載せる
- チャット上でも同じ画像を見せる
