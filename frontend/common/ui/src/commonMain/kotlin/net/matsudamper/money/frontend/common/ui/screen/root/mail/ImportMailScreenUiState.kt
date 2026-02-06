package net.matsudamper.money.frontend.common.ui.screen.root.mail

import androidx.compose.runtime.Immutable
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener

public data class ImportMailScreenUiState(
    val isLoading: Boolean,
    val showLoadMore: Boolean,
    val mails: ImmutableList<Mail>,
    val htmlDialog: String?,
    val mailDeleteDialog: MailDeleteDialog?,
    val event: Event,
    val kakeboScaffoldListener: KakeboScaffoldListener,
) {
    public data class MailDeleteDialog(
        val event: Event,
        val errorText: String?,
        val isLoading: Boolean,
    ) {
        @Immutable
        public interface Event {
            public fun onClickDelete()

            public fun onClickCancel()

            public fun onDismiss()
        }
    }

    public data class Mail(
        val isSelected: Boolean,
        val from: String,
        val subject: String,
        val event: Event,
        val sender: String?,
    ) {
        @Immutable
        public interface Event {
            public fun onClickDetail()

            public fun onClick()

            public fun onClickDelete()
        }
    }

    @Immutable
    public interface Event {
        public fun onViewInitialized()

        public fun htmlDismissRequest()

        public fun onClickImport()

        public fun onClickLoadMore()
    }
}
