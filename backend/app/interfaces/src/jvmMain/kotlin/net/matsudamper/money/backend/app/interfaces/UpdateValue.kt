package net.matsudamper.money.backend.app.interfaces

sealed interface UpdateValue<out T> {
    data class Update<T>(val value: T?) : UpdateValue<T>
    object NotUpdate : UpdateValue<Nothing>
}
