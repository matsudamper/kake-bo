package net.matsudamper.money.frontend.feature.admin.viewmodel

internal interface AdminSessionStorage {
    fun readSession(): AdminSession?
}
