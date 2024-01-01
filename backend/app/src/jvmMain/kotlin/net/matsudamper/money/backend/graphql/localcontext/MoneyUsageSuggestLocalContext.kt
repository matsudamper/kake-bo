package net.matsudamper.money.backend.graphql.localcontext

import net.matsudamper.money.element.ImportedMailId

data class MoneyUsageSuggestLocalContext(
    val importedMailId: ImportedMailId,
)
