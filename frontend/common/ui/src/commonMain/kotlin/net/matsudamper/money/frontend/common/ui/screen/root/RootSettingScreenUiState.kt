package net.matsudamper.money.frontend.common.ui.screen.root

public data class RootSettingScreenUiState(
    val event: Event,
) {

    public interface Event {
        public fun onResume()
        public fun onClickImapButton()
        public fun onClickCategoryButton()
    }
}
