package net.matsudamper.money.backend.base.element

import java.time.Instant
import net.matsudamper.money.element.MailId

public data class MailResult(
    val subject: String,
    val messageID: MailId,
    val content: List<Content>,
    val sender: String?,
    val from: List<String>,
    val personal: List<String>,
    val forwardedFor: List<String>,
    val forwardedTo: List<String>,
    val sendDate: Instant,
) {
    public sealed interface Content {
        public data class Text(val text: String) : Content

        public data class Html(val html: String) : Content

        public data class Other(val contentType: String) : Content
    }
}
