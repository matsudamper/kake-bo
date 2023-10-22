package net.matsudamper.money.frontend.common.viewmodel.lib

public abstract class EqualsImpl(
    public vararg val values: Any
) {
    override fun equals(other: Any?): Boolean {
        if (other !is EqualsImpl) {
            return false
        }

        return values.contentDeepEquals(other.values)
    }

    override fun hashCode(): Int {
        return values.contentDeepHashCode()
    }
}
