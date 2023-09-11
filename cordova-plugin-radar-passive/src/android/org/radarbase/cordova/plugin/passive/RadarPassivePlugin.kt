package org.radarbase.cordova.plugin.passive

import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions.Companion.ACTION_REQUEST_PERMISSIONS
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions.Companion.EXTRA_PERMISSIONS
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaInterface
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.CordovaWebView
import org.json.JSONArray
import org.json.JSONObject
import org.radarbase.android.RadarService
import org.radarbase.android.auth.AuthService
import org.radarbase.android.kafka.ServerStatusListener
import org.radarbase.android.plugin.RadarPassive
import org.radarbase.android.plugin.ResultListener
import org.radarbase.android.plugin.SourceStatus
import org.radarbase.android.util.PermissionRequester
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * This class echoes a string called from JavaScript.
 */
class RadarPassivePlugin : CordovaPlugin() {
    private lateinit var radarPassive: RadarPassive
    private val permissionRequests: ConcurrentMap<Int, PermissionRequestListener> = ConcurrentHashMap()
    private val permissionRequestName = AtomicInteger(49201)

    override fun initialize(cordova: CordovaInterface, webView: CordovaWebView) {
        super.initialize(cordova, webView)

        AuthService.serviceClass = AuthServiceImpl::class.java
        RadarService.serviceClass = RadarServiceImpl::class.java

        radarPassive = RadarPassive(
            cordova.context,
        )

        cordova.setActivityResultCallback(this)
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
                    "start" -> cordova.threadPool.execute {
                        start(callbackContext.toListener())
                    }
                    "startScanning" -> cordova.threadPool.execute {
                        startScanning()
                        callbackContext.success()
                    }
                    "stopScanning" -> cordova.threadPool.execute {
                        stopScanning()
                        callbackContext.success()
                    }
                    "stop" -> cordova.threadPool.execute {
                        stop()
                        callbackContext.success()
                    }
                    "serverStatus" -> callbackContext.success(serverStatus().toString())
                    "registerServerStatusListener" -> {
                        val listener = callbackContext.toListener<ServerStatusListener.Status>(args.getInt(0)) {
                            name
                        }
                        serverStatusListeners += listener
                        listener.next(serverStatus())
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
                        val listener = callbackContext.toListener<SourceStatus>(args.getInt(0)) {
                            toJSONObject()
                        }
                        sourceStatusListeners += listener
                        sourceStatus().forEach { listener.next(it) }
                    }
                    "unregisterSourceStatusListener" -> {
                        sourceStatusListeners -= args.getInt(0)
                        callbackContext.success()
                    }
                    "registerPluginListener" -> {
                        val listener = callbackContext.toListener<List<String>>(args.getInt(0)) {
                            JSONArray(this)
                        }
                        pluginsUpdatedListeners += listener
                        listener.next(pluginsActive)
                    }
                    "requestPermissions" -> {
                        val permissions = args.getJSONArray(0).toStringSet()
                        val listener = callbackContext.toListener<Set<String>> { toJSONArray() }
                        cordova.threadPool.execute {
                            requestPermissions(permissions, listener)
                        }
                    }
                    "requestPermissionsSupported" -> {
                        callbackContext.success(requestPermissionsSupported())
                    }
                    "unregisterPluginListener" -> {
                        pluginsUpdatedListeners -= args.getInt(0)
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
                    "recordsInCache" -> cordova.threadPool.execute {
                        callbackContext.success(
                            jsonObject {
                                recordsInCache().forEach { (k, v) ->
                                    put(k, v)
                                }
                            }
                        )
                    }
                    "permissionsNeeded" -> callbackContext.success(
                        jsonObject {
                            permissionsNeeded().forEach { (k, v) ->
                                put(k, JSONArray(v))
                            }
                        }
                    )
                    "onAcquiredPermissions" -> cordova.threadPool.execute {
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
            Log.e(TAG, "Failed on action $action", ex)
            callbackContext.error("Failed on action $action: $ex")
        }

        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        val (requester, listener, alreadyGranted) = permissionRequests.remove(requestCode)
            ?: return
        listener.success(
            requester.contract.parseResult(resultCode, intent) + alreadyGranted
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        radarPassive.onDestroy()
        permissionRequests.clear()
    }

    private fun requestPermissionsSupported() = JSONObject().apply {
        radarPassive.requestPermissionRequesters.forEachIndexed { idx, requester ->
            if (requester.permissions.isNotEmpty()) {
                this@apply.put(idx.toString(), requester.permissions.toJSONArray())
            }
        }
    }

    private fun requestPermissions(permissions: Set<String>, listener: ResultListener<Set<String>>) {
        val requester = radarPassive.requestPermissionRequesters
            .firstNotNullOfOrNull {
                val intersection = it.permissions.intersect(permissions)
                if (intersection.isEmpty()) {
                    null
                } else {
                    PermissionRequester(
                        intersection,
                        it.contract,
                        it.grantChecker,
                    )
                }
            }
        if (requester == null) {
            listener.error("No permissions can be requested.")
            return
        }
        val alreadyGranted = requester.grantChecker(cordova.context, permissions)
        if (alreadyGranted.size == permissions.size) {
            listener.success(alreadyGranted)
            return
        }
        val requestCode = permissionRequestName.getAndIncrement()
        permissionRequests[requestCode] = PermissionRequestListener(
            requester,
            listener,
            alreadyGranted,
        )
        val toRequestPermission = requester.permissions - alreadyGranted
        Log.i(TAG, "Starting request $requestCode for activity result $toRequestPermission")

        val intent = requester.contract.createIntent(cordova.context, toRequestPermission)

        if (intent.action == ACTION_REQUEST_PERMISSIONS) {
            cordova.requestPermissions(
                this@RadarPassivePlugin,
                requestCode,
                intent.getStringArrayExtra(EXTRA_PERMISSIONS)
            )
        } else {
            cordova.startActivityForResult(
                this@RadarPassivePlugin,
                intent,
                requestCode,
            )
        }
    }

    // WARNING: onRequestPermissionsResult is not called yet by the Cordova code, implementing this instead.
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onRequestPermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // WARNING: onRequestPermissionsResult is not called yet, implementing this instead.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val request = permissionRequests.remove(requestCode) ?: return
        Log.i(TAG, "Got request permissions result for code $requestCode and permissions ${permissions.contentToString()}")
        request.listener.success(buildSet {
            permissions.filterIndexedTo(this) { idx, _ ->
                grantResults[idx] == PERMISSION_GRANTED
            }
            addAll(request.alreadyGranted)
        })
    }

    companion object {
        private const val TAG = "RadarPassivePlugin"
    }

    data class PermissionRequestListener(
        val requester: PermissionRequester,
        val listener: ResultListener<Set<String>>,
        val alreadyGranted: Set<String>,
    )
}
