package net.matsudamper.money.backend.dataloader

import org.dataloader.DataLoader

interface DataLoaderDefine<Key, R> {
    val key: String

    fun getDataLoader(): DataLoader<Key, R>
}
