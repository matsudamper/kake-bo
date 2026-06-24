package net.matsudamper.money.frontend.common.base.nav.user

internal class BrowserNavBackstack(initial: IScreenStructure) {
    var entries: List<IScreenStructure> = listOf(initial)
        private set

    fun addScreen(screen: IScreenStructure) {
        entries = entries + screen
    }

    fun navigate(navigation: IScreenStructure): NavigationResult {
        if (navigation.stackGroupId != null && navigation.stackGroupId != entries.lastOrNull()?.stackGroupId) {
            val targetGroupTailIndex = entries.indexOfLast { it.stackGroupId == navigation.stackGroupId }
                .takeIf { it >= 0 }
                ?.plus(1)

            if (targetGroupTailIndex != null) {
                val targetGroupStartIndex = entries.take(targetGroupTailIndex)
                    .indexOfLast { it.stackGroupId != navigation.stackGroupId }
                    .plus(1)

                val list = entries.toMutableList()
                val targetRange = list.subList(targetGroupStartIndex, targetGroupTailIndex).toList()
                repeat(targetRange.size) {
                    list.removeAt(targetGroupStartIndex)
                }
                list.addAll(targetRange)
                entries = list
                return NavigationResult.Push(entries.last())
            }
        }

        return if (entries.lastOrNull()?.sameScreenId == navigation.sameScreenId) {
            entries = entries.dropLast(1) + navigation
            NavigationResult.Replace(navigation)
        } else {
            entries = entries + navigation
            NavigationResult.Push(navigation)
        }
    }

    fun navigateToLogin(navigation: IScreenStructure) {
        entries = listOf(navigation)
    }

    fun navigateReplace(navigation: IScreenStructure) {
        entries = if (entries.isEmpty()) {
            listOf(navigation)
        } else {
            entries.dropLast(1) + navigation
        }
    }

    /**
     * popstateイベント時のバックスタック更新。
     * delta が負 = ブラウザの戻るボタン、正 = ブラウザの進むボタン
     */
    fun handlePopState(delta: Int, currentScreen: IScreenStructure) {
        when {
            delta < 0 -> {
                val removeCount = minOf(-delta, entries.size - 1)
                entries = entries.dropLast(removeCount)
            }
            delta > 0 -> {
                entries = entries + currentScreen
            }
        }
    }

    /**
     * @return ホーム画面（Root）に戻るための history.go に渡すステップ数。
     *         負の値 = 戻る、0 = すでにホームにいる。
     */
    fun stepsToHome(): Int {
        val rootIndex = entries.indexOfLast { it is ScreenStructure.Root }
        return when {
            rootIndex < 0 -> -(entries.size - 1)
            else -> -(entries.size - 1 - rootIndex)
        }
    }

    sealed interface NavigationResult {
        val screen: IScreenStructure

        data class Push(override val screen: IScreenStructure) : NavigationResult

        data class Replace(override val screen: IScreenStructure) : NavigationResult
    }
}
