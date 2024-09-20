package net.matsudamper.money.frontend.common.feature.localstore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import com.google.protobuf.InvalidProtocolBufferException
import net.matsudamper.money.frontend.common.feature.localstore.generated.Session

internal object SessionSerializer : Serializer<Session> {
    override val defaultValue: Session = Session.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): Session {
        try {
            return Session.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: Session, output: java.io.OutputStream) {
        t.writeTo(output)
    }
}
