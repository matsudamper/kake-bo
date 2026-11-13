package net.matsudamper.money.frontend.graphql

/**
 * 管理セッションは永続化せず、アプリのプロセスが生きている間だけ保持する。
 * 必要になったら設定画面の導線から都度ログインし直す想定。
 */
public object AdminSessionMemoryStore {
    @Volatile
    public var adminSessionId: String? = null
}
