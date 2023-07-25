package net.matsudamper.money.backend.dataloader

import org.dataloader.DataLoader

interface DataLoaderProvider<Key, R> {
    val displayName: String
    fun getDataLoader(): DataLoader<Key, R>
}
