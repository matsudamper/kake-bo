package net.matsudamper.money.frontend.common.base.nav.user

import androidx.navigation3.runtime.NavKey

public interface IScreenStructure : NavKey {
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

    /**
     * 画面を保存するための一意なID
     */
    public val scopeKey: String get() = sameScreenId

    public fun createUrl(): String {
        return direction.placeholderUrl
    }
}
