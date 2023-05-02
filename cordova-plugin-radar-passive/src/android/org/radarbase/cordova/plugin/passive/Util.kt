package org.radarbase.cordova.plugin.passive

import android.util.SparseArray
import org.apache.cordova.CallbackContext
import org.apache.cordova.PluginResult
import org.json.JSONArray
import org.json.JSONObject
import org.radarbase.android.data.DataHandler

data class Authentication(
    val baseUrl: String,
    val userId: String,
    val projectId: String,
    val token: String?,
)

class ContextFlushCallback(private val context: CallbackContext) : DataHandler.FlushCallback {
    override fun success() {
        context.success(JSONObject(mapOf("type" to "success")))
    }

    override fun error(ex: Throwable) {
        context.error(ex.toString())
    }

    override fun progress(current: Long, total: Long) {
        context.next(
            JSONObject(
                mapOf(
                    "type" to "progress",
                    "current" to current,
                    "total" to total,
                )
            )
        )
    }
}

class SynchronizedSparseArray<T> {
    private val sparseArray = SparseArray<T>()

    @Synchronized
    fun forEach(block: (T) -> Unit) {
        repeat(sparseArray.size()) { idx ->
            block(sparseArray.valueAt(idx))
        }
    }

    @Synchronized
    operator fun set(key: Int, value: T) {
        sparseArray[key] = value
    }

    @Synchronized
    operator fun minusAssign(key: Int) {
        sparseArray.remove(key)
    }

    @Synchronized
    fun clear() {
        sparseArray.clear()
    }
}

class SynchronizedList<T> {
    private val list = mutableListOf<T>()

    @Synchronized
    fun forEach(block: (T) -> Unit) {
        list.forEach(block)
    }

    @Synchronized
    fun clear() {
        list.clear()
    }

    @Synchronized
    operator fun plusAssign(value: T) {
        list += value
    }
}

internal fun CallbackContext.next(result: String) {
    sendPluginResult(PluginResult(PluginResult.Status.OK, result).apply {
        keepCallback = true
    })
}

internal fun CallbackContext.next(result: JSONObject) {
    sendPluginResult(PluginResult(PluginResult.Status.OK, result).apply {
        keepCallback = true
    })
}

internal fun jsonObject(builder: JSONObject.() -> Unit) = JSONObject().apply(builder)

internal fun JSONObject.toStringMap(): Map<String, String?> = buildMap(length()) {
    keys().forEach { key ->
        put(
            key,
            get(key).takeIf { it != JSONObject.NULL }?.toString()
        )
    }
}

internal fun JSONArray.toStringList(): List<String> = buildList(length()) {
    repeat(length()) { idx ->
        add(getString(idx))
    }
}


internal fun JSONObject.toAuthentication() = Authentication(
    baseUrl = getString("baseUrl"),
    userId = getString("userId"),
    projectId = getString("projectId"),
    token = optString("token").takeUnless { it == "" || it == "null" },
)
