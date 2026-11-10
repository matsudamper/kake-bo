package lib.js

import kotlin.js.JsAny
import kotlin.js.get
import kotlin.js.unsafeCast
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.events.CompositionEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.InputEvent
import org.w3c.dom.events.KeyboardEvent

/**
 * IME 確定直後に Compose の入力要素へ届く、確定文字列と同じ insertText を捨てる。
 *
 * Compose Multiplatform Web の NativeInputEventsProcessor は compositionend と
 * beforeinput(inputType: insertText) の両方で確定するため、IME が確定時に両方を送ると同じ文字が二重に入る。
 * Windows の Google 日本語入力や Gecko 系ブラウザで起き、MS IME では起きない。
 * Compose より先に受け取るため window のキャプチャ段階で止める。
 * 対象は Compose が生成する contenteditable だけにし、HTML の input / textarea には触らない。
 * Compose 側で直ったら削除する(#1171)。
 */
internal fun installDuplicateCompositionCommitGuard() {
    DuplicateCompositionCommitGuard().install()
}

private class DuplicateCompositionCommitGuard {
    private var committedText: String? = null

    fun install() {
        window.addEventListener("compositionstart", { committedText = null }, true)
        window.addEventListener("compositionend", { event -> onCompositionEnd(event) }, true)
        window.addEventListener("keydown", { event -> onKeyDown(event) }, true)
        window.addEventListener("beforeinput", { event -> onBeforeInput(event) }, true)
    }

    private fun onCompositionEnd(event: Event) {
        committedText = (event as CompositionEvent).data.takeIf { it.isNotEmpty() }
    }

    private fun onKeyDown(event: Event) {
        event as KeyboardEvent
        if (!event.isComposing && event.keyCode != IME_PROCESS_KEY_CODE) {
            committedText = null
        }
    }

    private fun onBeforeInput(event: Event) {
        val expected = committedText
        committedText = null
        if (expected == null || event !is InputEvent) return

        val isDuplicateCommit = !event.isComposing &&
            event.unsafeCast<InputEventWithInputType>().inputType == "insertText" &&
            event.data == expected &&
            isComposeBackingField(event)
        if (isDuplicateCommit) {
            event.preventDefault()
            event.stopImmediatePropagation()
        }
    }

    // Compose の入力要素は Shadow DOM 内にあり、window では target がホストに付け替わるため composedPath で実際の要素を見る
    private fun isComposeBackingField(event: Event): Boolean {
        val realTarget = event.composedPath()[0] ?: event.target
        val element = realTarget as? Element ?: return false
        return element.classList.contains(COMPOSE_BACKING_FIELD_CLASS)
    }

    companion object {
        private const val IME_PROCESS_KEY_CODE = 229
        private const val COMPOSE_BACKING_FIELD_CLASS = "compose-backing-field"
    }
}

private external interface InputEventWithInputType : JsAny {
    val inputType: String
}
