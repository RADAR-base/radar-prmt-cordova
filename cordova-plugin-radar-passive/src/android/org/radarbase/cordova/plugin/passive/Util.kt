package org.radarbase.cordova.plugin.passive

import org.apache.cordova.CallbackContext
import org.apache.cordova.PluginResult
import org.json.JSONArray
import org.json.JSONObject
import org.radarbase.android.plugin.Authentication
import org.radarbase.android.plugin.FlushProgress
import org.radarbase.android.plugin.FlushResult
import org.radarbase.android.plugin.ResultListener
import org.radarbase.android.plugin.SendStatus
import org.radarbase.android.plugin.SendSuccess
import org.radarbase.android.plugin.SourceStatus

internal class CallbackResultListener<T: Any>(
    private val callbackContext: CallbackContext,
    override val id: Int? = null,
    private val transform: T.() -> Any = { this },
) : ResultListener<T> {
    override fun next(value: T) {
        callbackContext.sendPluginResult(value.toPluginResult().apply { keepCallback = true })
    }
    override fun error(message: String) {
        callbackContext.error(message)
    }
    override fun success(value: T) {
        callbackContext.sendPluginResult(value.toPluginResult())
    }

    private fun T.toPluginResult(
        status: PluginResult.Status = PluginResult.Status.OK,
    ) = when (val result = transform()) {
        is String -> PluginResult(status, result)
        is JSONObject -> PluginResult(status, result)
        is JSONArray -> PluginResult(status, result)
        is Unit -> PluginResult(status)
        else -> throw IllegalArgumentException("Unknown result type ${result.javaClass}")
    }
}

internal fun <T: Any> CallbackContext.toListener(
    id: Int? = null,
    transform: T.() -> Any = { this },
): ResultListener<T> = CallbackResultListener(this, id, transform)

internal fun jsonObject(builder: JSONObject.() -> Unit) = JSONObject().apply(builder)

internal fun JSONObject.toStringMap(): Map<String, String?> = buildMap(length()) {
    this@toStringMap.keys().forEach { key ->
        put(
            key,
            this@toStringMap.get(key).takeIf { it != JSONObject.NULL }
                ?.toString()
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
