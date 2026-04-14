package net.matsudamper.money.frontend.common.feature.uploader

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array

internal class ImageUploadLocalStorageJsImpl : ImageUploadLocalStorage {
    private companion object {
        const val DB_NAME = "image_upload_local_storage"
        const val DB_VERSION = 1
        const val STORE_RAW_IMAGES = "rawImages"
        const val STORE_PREVIEWS = "previews"
    }

    private var cachedDb: dynamic = null

    private suspend fun openDb(): dynamic {
        cachedDb?.let { return it }
        val db = suspendCancellableCoroutine { cont ->
            val request = js("self.indexedDB || self.webkitIndexedDB || self.mozIndexedDB").open(DB_NAME, DB_VERSION)
            request.onupgradeneeded = { event: dynamic ->
                val db = event.target.result
                if (!db.objectStoreNames.contains(STORE_RAW_IMAGES)) {
                    db.createObjectStore(STORE_RAW_IMAGES)
                }
                if (!db.objectStoreNames.contains(STORE_PREVIEWS)) {
                    db.createObjectStore(STORE_PREVIEWS)
                }
            }
            request.onsuccess = { event: dynamic ->
                cont.resume(event.target.result)
            }
            request.onerror = { event: dynamic ->
                cont.resumeWithException(Exception("IndexedDB open error: ${event.target.error}"))
            }
        }
        cachedDb = db
        return db
    }

    private suspend fun put(storeName: String, key: String, bytes: ByteArray) {
        val db = openDb()
        suspendCancellableCoroutine { cont ->
            val tx = db.transaction(storeName, "readwrite")
            val store = tx.objectStore(storeName)
            store.put(Uint8Array(bytes.unsafeCast<Int8Array>().buffer), key)
            tx.oncomplete = {
                cont.resume(Unit)
            }
            tx.onerror = { event: dynamic ->
                cont.resumeWithException(Exception("IndexedDB put error: ${event.target.error}"))
            }
        }
    }

    private suspend fun get(storeName: String, key: String): ByteArray? {
        val db = openDb()
        return suspendCancellableCoroutine { cont ->
            val tx = db.transaction(storeName, "readonly")
            val store = tx.objectStore(storeName)
            val request = store.get(key)
            request.onsuccess = { event: dynamic ->
                val result = event.target.result
                if (result == null) {
                    cont.resume(null)
                } else {
                    cont.resume(Int8Array(result.unsafeCast<Uint8Array>().buffer).unsafeCast<ByteArray>())
                }
            }
            request.onerror = { event: dynamic ->
                cont.resumeWithException(Exception("IndexedDB get error: ${event.target.error}"))
            }
        }
    }

    override suspend fun writeRawImage(id: String, bytes: ByteArray) {
        put(STORE_RAW_IMAGES, id, bytes)
    }

    override suspend fun writePreview(id: String, bytes: ByteArray) {
        put(STORE_PREVIEWS, id, bytes)
    }

    override suspend fun readRawImage(id: String): ByteArray? {
        return get(STORE_RAW_IMAGES, id)
    }

    override suspend fun readPreview(id: String): ByteArray? {
        return get(STORE_PREVIEWS, id)
    }

    override suspend fun deleteImages(id: String) {
        val db = openDb()
        suspendCancellableCoroutine { cont ->
            val storeNames = js("[]")
            storeNames.push(STORE_RAW_IMAGES)
            storeNames.push(STORE_PREVIEWS)
            val tx = db.transaction(storeNames, "readwrite")
            tx.objectStore(STORE_RAW_IMAGES).delete(id)
            tx.objectStore(STORE_PREVIEWS).delete(id)
            tx.oncomplete = {
                cont.resume(Unit)
            }
            tx.onerror = { event: dynamic ->
                cont.resumeWithException(Exception("IndexedDB delete error: ${event.target.error}"))
            }
        }
    }
}
