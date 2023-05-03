package org.radarbase.cordova.plugin.passive

import android.util.Log
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaInterface
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.CordovaWebView
import org.json.JSONArray

/**
 * This class echoes a string called from JavaScript.
 */
class RadarPassivePlugin : CordovaPlugin() {
    private lateinit var radarPassive: RadarPassive

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
                    "configure" -> configure(callbackContext, args.getJSONObject(0).toStringMap())
                    "setAuthentication" -> setAuthentication(
                        args.optJSONObject(0)?.toAuthentication(),
                        callbackContext
                    )

                    "start" -> start(callbackContext)
                    "startScanning" -> startScanning(callbackContext)
                    "stopScanning" -> stopScanning(callbackContext)
                    "stop" -> stop(callbackContext)
                    "serverStatus" -> serverStatus(callbackContext)
                    "registerServerStatusListener" -> registerServerStatusListener(
                        args.getInt(0),
                        callbackContext,
                    )

                    "unregisterServerStatusListener" -> unregisterServerStatusListener(
                        args.getInt(0),
                        callbackContext,
                    )

                    "sourceStatus" -> sourceStatus(callbackContext)
                    "registerSourceStatusListener" -> registerSourceStatusListener(
                        args.getInt(0),
                        callbackContext,
                    )

                    "unregisterSourceStatusListener" -> unregisterSourceStatusListener(
                        args.getInt(0),
                        callbackContext,
                    )

                    "registerSendListener" -> registerSendListener(
                        args.getInt(0),
                        callbackContext,
                    )

                    "unregisterSendListener" -> unregisterSendListener(
                        args.getInt(0),
                        callbackContext,
                    )

                    "recordsInCache" -> recordsInCache(callbackContext)
                    "permissionsNeeded" -> permissionsNeeded(callbackContext)
                    "onAcquiredPermissions" -> onAcquiredPermissions(callbackContext)
                    "bluetoothNeeded" -> bluetoothNeeded(callbackContext)
                    "setAllowedSourceIds" -> setAllowedSourceIds(
                        args.getString(0),
                        args.getJSONArray(1).toStringList(),
                        callbackContext,
                    )

                    "flushCaches" -> flushCaches(callbackContext)
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
