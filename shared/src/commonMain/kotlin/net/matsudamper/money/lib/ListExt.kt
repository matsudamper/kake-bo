package net.matsudamper.money.lib

public fun <K, V> List<Map<K, V>>.flatten(): Map<K, V> {
    return this.fold(emptyMap()) { result, item ->
        result + item
    }
}
