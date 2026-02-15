package net.matsudamper.money.element

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
public value class ImageId(
    val value: Int,
)
