package lib.js

import kotlin.js.JsAny
import kotlin.js.get
import kotlin.js.unsafeCast
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.events.CompositionEvent
import org.w3c.dom.events.CompositionEventInit
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.InputEvent
import org.w3c.dom.events.KeyboardEvent

/**
 * IME 確定時に Compose の入力要素で確定文字列が二重に入るのを防ぐ。
 *
 * Compose Multiplatform Web の NativeInputEventsProcessor は、1 フレーム分のイベントを timeStamp 順に
 * 並べ替えてから処理する。Firefox(Gecko) は compositionend に確定イベント作成時刻を引き継がせる一方、
 * その直前に発火する beforeinput(insertCompositionText) は新しい時刻を持つため、ms 境界をまたぐと
 * compositionend が先に処理され、CommitText の後に SetComposingText が走って同じ文字が二重に入る。
 * そこで Compose より先(window のキャプチャ段階)で compositionend を止め、発火時刻のクローンを
 * 同じ要素に再ディスパッチして、Compose には beforeinput より新しい timeStamp で届ける。
 *
 * あわせて、compositionend の直後に同じ文字列の beforeinput(insertText) が届いた場合も捨てる
 * (pek0hara/ToriNos#4 で Gecko 系ブラウザから報告された列)。
 *
 * 対象は Compose が生成する contenteditable だけにし、HTML の input / textarea には触らない。
 * Compose 側で直ったら削除する(#1171)。
 */
internal fun installDuplicateCompositionCommitGuard() {
    DuplicateCompositionCommitGuard().install()
}

private class DuplicateCompositionCommitGuard {
    private var committedText: String? = null
    private var isRedispatchingCompositionEnd = false

    fun install() {
        window.addEventListener("compositionstart", { committedText = null }, true)
        window.addEventListener("compositionend", { event -> onCompositionEnd(event) }, true)
        window.addEventListener("keydown", { event -> onKeyDown(event) }, true)
        window.addEventListener("beforeinput", { event -> onBeforeInput(event) }, true)
    }

    private fun onCompositionEnd(event: Event) {
        event as CompositionEvent
        committedText = event.data.takeIf { it.isNotEmpty() }
        if (isRedispatchingCompositionEnd || !isComposeBackingField(event)) return

        val target = realTarget(event) ?: return
        event.stopImmediatePropagation()
        redispatchWithCurrentTimeStamp(event, target)
    }

    private fun redispatchWithCurrentTimeStamp(event: CompositionEvent, target: EventTarget) {
        val clone = CompositionEvent(
            event.type,
            CompositionEventInit(
                data = event.data,
                bubbles = event.bubbles,
                cancelable = event.cancelable,
                composed = true,
            ),
        )
        isRedispatchingCompositionEnd = true
        try {
            target.dispatchEvent(clone)
        } finally {
            isRedispatchingCompositionEnd = false
        }
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

    private fun isComposeBackingField(event: Event): Boolean {
        val element = realTarget(event) as? Element ?: return false
        return element.classList.contains(COMPOSE_BACKING_FIELD_CLASS)
    }

    // Compose の入力要素は Shadow DOM 内にあり、window では target がホストに付け替わるため composedPath で実際の要素を見る
    private fun realTarget(event: Event): EventTarget? {
        return event.composedPath()[0] ?: event.target
    }

    companion object {
        private const val IME_PROCESS_KEY_CODE = 229
        private const val COMPOSE_BACKING_FIELD_CLASS = "compose-backing-field"
    }
}

private external interface InputEventWithInputType : JsAny {
    val inputType: String
}
