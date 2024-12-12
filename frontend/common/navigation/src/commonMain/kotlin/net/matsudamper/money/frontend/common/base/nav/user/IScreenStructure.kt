package net.matsudamper.money.frontend.common.base.nav.user

public interface IScreenStructure {
    public val direction: Direction
    public val groupId: Any?

    /***
     * 同じ画面かを判定する。例えば、クエリパラメータが変わるだけでは違う画面と言えない。
     */
    public fun equalScreen(other: IScreenStructure): Boolean = equals(other)

    public fun createUrl(): String {
        return direction.placeholderUrl
    }
}
