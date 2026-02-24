package net.matsudamper.money.frontend.common.ui.base

import androidx.compose.runtime.Immutable
import net.matsudamper.money.frontend.common.base.ImmutableList

public data class CategorySelectDialogUiState(
    val screenType: Screen,
    val event: Event,
) {
    @Immutable
    public sealed interface Screen {
        public data class Root(
            val category: String,
            val subCategory: String,
            val onClickCategory: () -> Unit,
            val onClickSubCategory: () -> Unit,
            val enableSubCategory: Boolean,
        ) : Screen

        public data class Category(
            val categories: ImmutableList<CategorySelectDialogUiState.Category>,
            val onBackRequest: () -> Unit,
        ) : Screen

        public data class SubCategory(
            val subCategories: ImmutableList<CategorySelectDialogUiState.Category>?,
            val onBackRequest: () -> Unit,
        ) : Screen
    }

    public data class Category(
        val id: String,
        val name: String,
        val isSelected: Boolean,
        val onSelected: () -> Unit,
    )

    @Immutable
    public interface Event {
        public fun dismissRequest()

        public fun selectCompleted()
    }
}
