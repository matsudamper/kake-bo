---
name: download-graphql-schema
description: Guide for updating the frontend GraphQL schema by downloading it from a locally running backend. Use when the backend GraphQL schema has changed and the frontend schema file needs to be updated.
---

# GraphQL スキーマ更新ガイド

バックエンドのGraphQLスキーマが変更されたとき、フロントエンドの `schema.graphqls` を最新化する手順。

---

## 仕組み

フロントエンドの Apollo Client は `frontend/common/graphql/schema/src/commonMain/graphql/schema.graphqls` を参照してコードを生成する。このファイルは、ローカルで起動したバックエンドのイントロスペクションエンドポイントから取得する。

| ファイル | 役割 |
|---|---|
| `frontend/common/graphql/schema/src/commonMain/graphql/schema.graphqls` | Apollo が参照するスキーマファイル |
| `frontend/common/graphql/schema/build.gradle.kts` | ダウンロードタスクの定義（エンドポイント: `http://localhost/query`） |
| `schema_update_local.env` | スキーマ取得用のバックエンド起動環境変数（`IS_DEBUG=true` を含む） |

---

## 前提条件

- `IS_DEBUG=true` が必須：バックエンドの `MoneyGraphQlSchema` はこのフラグが `true` の場合のみイントロスペクションを有効化する
- `schema_update_local.env` はダミー値でDBへの接続を設定しているが、イントロスペクションに DB 接続は不要なため問題ない
- バックエンドは `PORT=80`（`localhost` のポート80）で起動するため、**ルート権限が必要**

---

## 手順

### 1. バックエンドをビルドする

```shell
./gradlew :backend:assemble
```

実行可能スクリプトが `backend/build/bin/backend` に生成される。

### 2. バックエンドをバックグラウンドで起動する

```shell
env $(cat schema_update_local.env | grep -v '^#' | xargs) ./backend/build/bin/backend &
BACKEND_PID=$!
```

`schema_update_local.env` の内容：

| 変数 | 値 | 説明 |
|---|---|---|
| `PORT` | `80` | Nettyがリッスンするポート |
| `DOMAIN` | `localhost` | CORSホスト設定 |
| `IS_DEBUG` | `true` | イントロスペクションを有効化（必須） |
| `DB_*` | ダミー値 | DBには接続しないが変数が必要 |

### 3. バックエンドの起動を待つ

```shell
until curl -sf http://localhost/healthz > /dev/null 2>&1; do
  sleep 1
done
```

ヘルスチェックが成功するまで待機する（`/healthz` エンドポイントが `ok` を返せば準備完了）。

### 4. スキーマをダウンロードする

```shell
./gradlew :frontend:common:graphql:schema:downloadSchema
```

`frontend/common/graphql/schema/src/commonMain/graphql/schema.graphqls` が更新される。

### 5. バックエンドを停止する

```shell
kill $BACKEND_PID
```

### 6. Apollo ソースを再生成する

```shell
./gradlew generateApolloSources
```

スキーマの変更に合わせてフロントエンドのGraphQLクライアントコードが再生成される。

---

## スキーマ変更後の作業

スキーマが更新されたら、フロントエンドの各 `.graphql` ファイルが新しいスキーマに対応しているか確認する。

フロントエンドの `.graphql` ファイルは以下に格納されている：

```
frontend/common/graphql/schema/src/commonMain/graphql/
```

ビルドして型エラーがないことを確認する：

```shell
./gradlew :backend:assemble :frontend:app:jsBrowserDevelopmentWebpack :frontend:app:assembleDebug --warn
```
