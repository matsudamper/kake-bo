package net.matsudamper.money.detekt

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class NativeImageRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("native-image")

    override fun instance(): RuleSet {
        return RuleSet(
            ruleSetId,
            listOf(::KtorRuntimeSerializerLookup),
        )
    }
}
