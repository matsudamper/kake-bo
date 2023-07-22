import androidx.compose.runtime.Immutable
import net.matsudamper.money.frontend.common.base.ImmutableList

public data class MailScreenUiState(
    val isLoading: Boolean,
    val mails: ImmutableList<Mail>,
    val htmlDialog: String?,
    val event: Event,
) {
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
        }
    }

    @Immutable
    public interface Event {
        public fun onViewInitialized()
        public fun htmlDismissRequest()
        public fun onClickImport()
        public fun onClickBackButton()
    }
}