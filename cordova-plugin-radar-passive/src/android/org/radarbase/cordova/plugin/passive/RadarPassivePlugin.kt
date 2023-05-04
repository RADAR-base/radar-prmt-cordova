package org.radarbase.cordova.plugin.passive

import android.util.Log
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaInterface
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.CordovaWebView
import org.json.JSONArray
import java.util.concurrent.atomic.AtomicInteger

/**
 * This class echoes a string called from JavaScript.
 */
class RadarPassivePlugin : CordovaPlugin() {
    private lateinit var radarPassive: RadarPassive
    private val startCallbackCounter = AtomicInteger(0)

    override fun initialize(cordova: CordovaInterface, webView: CordovaWebView) {
        super.initialize(cordova, webView)

        radarPassive = RadarPassive(cordova.context)
    }

    override fun execute(
        action: String,
        args: JSONArray,
        callbackContext: CallbackContext,
    ): Boolean {
        try {
            with(radarPassive) {
                when (action) {
                    "configure" -> {
                        configure(args.getJSONObject(0).toStringMap())
                        callbackContext.success()
                    }
                    "setAuthentication" -> {
                        setAuthentication(
                            args.optJSONObject(0)?.toAuthentication()
                        )
                        callbackContext.success()
                    }
                    "start" -> start(CallbackResultListener(startCallbackCounter.incrementAndGet(), callbackContext))
                    "startScanning" -> {
                        startScanning()
                        callbackContext.success()
                    }
                    "stopScanning" -> {
                        stopScanning()
                        callbackContext.success()
                    }
                    "stop" -> {
                        stop()
                        callbackContext.success()
                    }
                    "serverStatus" -> callbackContext.success(serverStatus().toString())
                    "registerServerStatusListener" -> {
                        serverStatusListeners += CallbackResultListener(args.getInt(0), callbackContext) {
                            name
                        }
                    }
                    "unregisterServerStatusListener" -> {
                        serverStatusListeners -= args.getInt(0)
                        callbackContext.success()
                    }
                    "sourceStatus" -> {
                        callbackContext.success(jsonObject {
                            sourceStatus().forEach { sourceStatus ->
                                put(sourceStatus.plugin, sourceStatus.toJSONObject())
                            }
                        })
                    }
                    "registerSourceStatusListener" -> {
                        sourceStatusListeners += CallbackResultListener(args.getInt(0), callbackContext) {
                            toJSONObject()
                        }
                    }
                    "unregisterSourceStatusListener" -> {
                        sourceStatusListeners -= args.getInt(0)
                        callbackContext.success()
                    }
                    "registerSendListener" -> {
                        sendListeners += CallbackResultListener(args.getInt(0), callbackContext) {
                            toJSONObject()
                        }
                    }
                    "unregisterSendListener" -> {
                        sendListeners -= args.getInt(0)
                        callbackContext.success()
                    }
                    "recordsInCache" -> callbackContext.success(
                        jsonObject {
                            recordsInCache().forEach { (k, v) ->
                                put(k, v)
                            }
                        }
                    )
                    "permissionsNeeded" -> callbackContext.success(
                        jsonObject {
                            permissionsNeeded().forEach { (k, v) ->
                                put(k, JSONArray(v))
                            }
                        }
                    )
                    "onAcquiredPermissions" -> {
                        onAcquiredPermissions()
                        callbackContext.success()
                    }
                    "bluetoothNeeded" -> callbackContext.success(JSONArray(bluetoothNeeded()))
                    "setAllowedSourceIds" -> {
                        setAllowedSourceIds(
                            args.getString(0),
                            args.getJSONArray(1).toStringList(),
                        )
                        callbackContext.success()
                    }
                    "flushCaches" -> flushCaches(
                        CallbackResultListener(startCallbackCounter.incrementAndGet(), callbackContext) {
                            toJSONObject()
                        }
                    )
                    else -> {
                        Log.w(TAG, "Unknown command $action")
                        return false
                    }
                }
            }
        } catch (ex: Exception) {
            callbackContext.error("Failed on action $action: $ex")
        }

        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        radarPassive.onDestroy()
    }

    companion object {
        private const val TAG = "RadarPassivePlugin"
    }
}
