import net.matsudamper.money.frontend.common.base.ImmutableList

public data class MailScreenUiState(
    val isLoading: Boolean,
    val mails: ImmutableList<Mail>,
    val htmlDialog: String?,
    val event: Event
) {
    public data class Mail(
        val subject: String,
        val text: String,
        val onClick: () -> Unit,
    )
    public interface Event {
        public fun htmlDismissRequest()
    }
}