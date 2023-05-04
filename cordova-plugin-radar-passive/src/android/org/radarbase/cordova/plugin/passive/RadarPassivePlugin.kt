package org.radarbase.cordova.plugin.passive

import android.util.Log
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaInterface
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.CordovaWebView
import org.json.JSONArray
import org.radarbase.android.plugin.RadarPassive

/**
 * This class echoes a string called from JavaScript.
 */
class RadarPassivePlugin : CordovaPlugin() {
    private lateinit var radarPassive: RadarPassive

    override fun initialize(cordova: CordovaInterface, webView: CordovaWebView) {
        super.initialize(cordova, webView)

        radarPassive = RadarPassive(
            cordova.context,
            RadarServiceImpl::class.java,
            AuthServiceImpl::class.java
        )
    }

    override fun execute(
        action: String,
        args: JSONArray,
        callbackContext: CallbackContext,
    ): Boolean {
        try {
            with(radarPassive) {
                when (action) {
                    // put in a threadpool because configure can be slow
                    "configure" -> cordova.threadPool.execute {
                        configure(args.getJSONObject(0).toStringMap())
                        callbackContext.success()
                    }
                    // put in a threadpool because authentication can be slow
                    "setAuthentication" -> cordova.threadPool.execute {
                        setAuthentication(args.optJSONObject(0)?.toAuthentication())
                        callbackContext.success()
                    }
                    "start" -> start(callbackContext.toListener())
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
                        serverStatusListeners += callbackContext.toListener(args.getInt(0)) {
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
                        sourceStatusListeners += callbackContext.toListener(args.getInt(0)) {
                            toJSONObject()
                        }
                    }
                    "unregisterSourceStatusListener" -> {
                        sourceStatusListeners -= args.getInt(0)
                        callbackContext.success()
                    }
                    "registerSendListener" -> {
                        sendListeners += callbackContext.toListener(args.getInt(0)) {
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
                        callbackContext.toListener {
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
