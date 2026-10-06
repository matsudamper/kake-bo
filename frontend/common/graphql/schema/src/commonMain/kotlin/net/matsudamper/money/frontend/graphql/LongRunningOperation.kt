package net.matsudamper.money.frontend.graphql

/**
 * サーバーのスキーマで @longRunning が付いたフィールドを含む操作。
 * apollo-compiler-plugin が生成コードに実装させる。
 */
public interface LongRunningOperation {
    public val timeoutSeconds: Int
}
