package net.matsudamper.money.frontend.common.feature.localstore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import net.matsudamper.money.frontend.common.feature.localstore.generated.Session

public interface DataStores {
    public val sessionDataStore: DataStore<Session>

    public companion object {
        public fun create(context: Context): DataStores = DataStoresImpl(context)
    }
}

internal class DataStoresImpl(context: Context) : DataStores {
    override val sessionDataStore: DataStore<Session> = object {
        val Context.sessionDataStore: DataStore<Session> by dataStore(
            fileName = "Session.pb",
            serializer = SessionSerializer,
        )
    }.run { context.sessionDataStore }
}
