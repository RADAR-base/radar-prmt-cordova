package org.radarbase.cordova.plugin.passive

import android.util.SparseArray
import org.apache.cordova.CallbackContext
import org.apache.cordova.PluginResult
import org.json.JSONArray
import org.json.JSONObject
import org.radarbase.android.data.DataHandler
import java.lang.IllegalArgumentException

class ContextFlushCallback(private val listener: ResultListener<FlushResult>) : DataHandler.FlushCallback {
    override fun success() {
        listener.success(FlushSuccess)
    }
    override fun error(ex: Throwable) {
        listener.error(ex.toString())
    }
    override fun progress(current: Long, total: Long) {
        listener.next(FlushProgress(current, total))
    }
}

interface ResultListener<T> {
    val id: Int
    fun next(value: T)
    fun error(message: String)
    fun success(value: T)
}

class CallbackResultListener<T: Any>(
    override val id: Int,
    private val callbackContext: CallbackContext,
    private val transform: T.() -> Any = { this },
) : ResultListener<T> {
    override fun next(value: T) {
        when (val result = value.transform()) {
            is String -> callbackContext.next(result)
            is JSONObject -> callbackContext.next(result)
            else -> throw IllegalArgumentException("Unknown result type ${result.javaClass}")
        }
    }
    override fun error(message: String) {
        callbackContext.error(message)
    }

    override fun success(value: T) {
        when (val result = value.transform()) {
            is String -> callbackContext.success(result)
            is JSONObject -> callbackContext.success(result)
            else -> throw IllegalArgumentException("Unknown result type ${result.javaClass}")
        }
    }
}

class ResultListeners<T> {
    private val sparseArray = SparseArray<ResultListener<T>>()

    @Synchronized
    fun next(value: T) {
        repeat(sparseArray.size()) { idx ->
            sparseArray.valueAt(idx).next(value)
        }
    }

    @Synchronized
    fun success(value: T) {
        repeat(sparseArray.size()) { idx ->
            sparseArray.valueAt(idx).success(value)
        }
        clear()
    }

    @Synchronized
    fun error(message: String) {
        repeat(sparseArray.size()) { idx ->
            sparseArray.valueAt(idx).error(message)
        }
        clear()
    }

    @Synchronized
    operator fun plusAssign(listener: ResultListener<T>) {
        sparseArray[listener.id] = listener
    }

    @Synchronized
    operator fun minusAssign(id: Int) {
        sparseArray.remove(id)
    }

    @Synchronized
    fun clear() {
        sparseArray.clear()
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

fun SourceStatus.toJSONObject(): JSONObject = jsonObject {
    put("plugin", plugin)
    put("status", status)
    if (sourceName != null) {
        put("sourceName", sourceName)
    }
}

fun SendStatus.toJSONObject(): JSONObject = jsonObject {
    put("topic", topic)
    if (this@toJSONObject is SendSuccess) {
        put("status", "SUCCESS")
        put("numberOfRecords", numberOfRecords)
    } else {
        put("status", "ERROR")
    }
}

fun FlushResult.toJSONObject(): JSONObject = if (this is FlushProgress) {
    jsonObject {
        put("type", "progress")
        put("current", current)
        put("total", total)
    }
} else {
    jsonObject {
        put("type", "success")
    }
}

internal fun JSONObject.toAuthentication() = Authentication(
    baseUrl = getString("baseUrl"),
    userId = getString("userId"),
    projectId = getString("projectId"),
    token = optString("token").takeUnless { it == "" || it == "null" },
)
