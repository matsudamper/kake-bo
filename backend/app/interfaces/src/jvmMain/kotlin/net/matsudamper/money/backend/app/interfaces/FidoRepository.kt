package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.element.FidoId
import net.matsudamper.money.element.UserId

interface FidoRepository {

    data class RegisterdFido(
        val fidoId: FidoId,
        val name: String,
        val attestedCredentialData: String,
        val attestedStatement: String,
        val counter: Long,
        val attestedStatementFormat: String,
    )

    fun deleteFido(userId: UserId, id: FidoId): Boolean
    fun updateCounter(fidoId: FidoId, userId: UserId, counter: Long)
    fun getFidoList(userId: UserId): List<RegisterdFido>
    fun addFido(
        name: String,
        userId: UserId,
        attestationStatement: String,
        attestationStatementFormat: String,
        attestedCredentialData: String,
        counter: Long,
        authenticatorExtensions: String?,
    ): RegisterdFido
}
