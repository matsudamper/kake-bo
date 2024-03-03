package lib.js

import org.w3c.dom.DOMRectReadOnly
import org.w3c.dom.Element

external class ResizeObserver(callback: (entries: Array<ResizeObserverEntry>, observer: ResizeObserver) -> Unit) {
    fun observe(target: Element)

    fun disconnect()
}

external interface ResizeObserverEntry {
    val target: Element
    val contentRect: DOMRectReadOnly
}
