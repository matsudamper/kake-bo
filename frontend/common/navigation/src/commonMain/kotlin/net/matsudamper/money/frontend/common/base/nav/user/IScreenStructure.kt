package net.matsudamper.money.frontend.common.base.nav.user

public interface IScreenStructure {
    public val direction: Direction

    /**
     * Stackのグループを管理する
     * タブを切り替えて戻った時も、そのグループ内で戻る処理を管理するために使う
     */
    public val stackGroupId: Any?

    /***
     * 同じ画面かを判定する為に使用する。例えば、クエリパラメータが変わるだけでは違う画面と言えない。
     */
    public val sameScreenId: String

    public fun createUrl(): String {
        return direction.placeholderUrl
    }
}
