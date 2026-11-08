# Paparazzi

## 共通ルール

### UI 変更時
- `@Preview` を追加/更新しスナップショットを撮影する
- 撮影結果はセッションのチャットに貼る
- 画像なしで UI 作業完了にしない

## リポジトリ固有ルール

### 画像の扱い
- `frontend/common/ui/src/androidHostTest/snapshots/images/` の生成画像はコミットしない
- README から参照されている `README/` の画像は、撮影結果を使って更新しコミットする
- README 用の更新手順は `.claude/skills/update-paparazzi-screenshots/SKILL.md` も参照する

### 実行
```shell
./gradlew :frontend:common:ui:recordPaparazziAndroidMain
```
