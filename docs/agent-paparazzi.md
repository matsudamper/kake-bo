# Paparazzi

## UI 変更時
- `@Preview` を追加/更新しスナップショットを撮影する
- スナップショット画像はコミットしない。セッションのチャットに貼る
- Cloud Agent では `/opt/cursor/artifacts/` にもコピーし、完了報告で参照する
- 画像なしで UI 作業完了にしない

## 撮影コマンド

```sh
./gradlew :frontend:common:ui:recordPaparazziAndroidMain
```

特定の Preview だけ撮る場合は `--tests` でメソッド名を絞る。生成物は `frontend/common/ui/src/androidHostTest/snapshots/images/`（レポートは `frontend/common/ui/build/reports/paparazzi/`）。Preview 名が長いとファイル名上限で失敗するため、短い関数名にする。
