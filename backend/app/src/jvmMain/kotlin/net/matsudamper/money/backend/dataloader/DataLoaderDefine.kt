package net.matsudamper.money.backend.dataloader

interface DataLoaderDefine<Key : Any, R : Any> {
    val key: String

    fun load(keys: Set<Key>): Map<Key, R>
}
