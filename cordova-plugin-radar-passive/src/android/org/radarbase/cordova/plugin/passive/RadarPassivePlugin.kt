package org.radarbase.cordova.plugin.passive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.util.SparseArray
import androidx.core.util.valueIterator
import androidx.lifecycle.Lifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaInterface
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.CordovaWebView
import org.apache.cordova.PluginResult
import org.json.JSONArray
import org.json.JSONObject
import org.radarbase.android.IRadarBinder
import org.radarbase.android.RadarConfiguration
import org.radarbase.android.RadarConfiguration.Companion.BASE_URL_KEY
import org.radarbase.android.RadarService
import org.radarbase.android.auth.AuthService
import org.radarbase.android.data.DataHandler
import org.radarbase.android.source.SourceProvider
import org.radarbase.android.source.SourceService
import org.radarbase.android.source.SourceService.Companion.SERVER_RECORDS_SENT_TOPIC
import org.radarbase.android.source.SourceService.Companion.SERVER_STATUS_CHANGED
import org.radarbase.android.source.SourceService.Companion.SOURCE_PLUGIN_NAME
import org.radarbase.android.source.SourceService.Companion.SOURCE_STATUS_CHANGED
import org.radarbase.android.source.SourceService.Companion.SOURCE_STATUS_NAME
import org.radarbase.android.source.SourceStatusListener
import org.radarbase.android.util.BluetoothStateReceiver.Companion.bluetoothPermissionList
import org.radarbase.android.util.ManagedServiceConnection
import org.radarbase.monitor.application.ApplicationStatusProvider
import org.radarbase.passive.audio.OpenSmileAudioProvider
import org.radarbase.passive.bittium.FarosProvider
import org.radarbase.passive.empatica.E4Provider
import org.radarbase.passive.phone.PhoneSensorProvider
import org.radarbase.passive.phone.usage.PhoneUsageProvider
import org.radarbase.passive.weather.WeatherApiProvider
import java.util.*

/**
 * This class echoes a string called from JavaScript.
 */
class RadarPassivePlugin : CordovaPlugin() {
    private lateinit var broadcastManager: LocalBroadcastManager
    private lateinit var radarServiceConnection: ManagedServiceConnection<IRadarBinder>
    private lateinit var authServiceConnection: ManagedServiceConnection<AuthService.AuthServiceBinder>
    private val radarService: IRadarBinder
        get() = checkNotNull(radarServiceConnection.binder) {
            "RadarService is not connected yet"
        }
    private lateinit var config: RadarConfiguration
    private var localAuthentication: Authentication? = null
    private val statusReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                SERVER_RECORDS_SENT_TOPIC -> {
                    val result = JSONObject().apply {
                        put("topic", intent.getStringExtra(SERVER_RECORDS_SENT_TOPIC))
                        val number = intent.getIntExtra(SourceService.SERVER_RECORDS_SENT_NUMBER, 0)
                        if (number >= 0) {
                            put("status", "SUCCESS")
                            put("numberOfRecordsSent", number)
                        } else {
                            put("status", "ERROR")
                        }
                    }

                    sendListeners.forEach {
                        it.next(result)
                    }
                }
                SERVER_STATUS_CHANGED -> {
                    val result = intent.getStringExtra(SERVER_STATUS_CHANGED) ?: return
                    serverStatusListeners.forEach {
                        it.next(result)
                    }
                }
                SOURCE_STATUS_CHANGED -> {
                    val result = JSONObject().apply {
                        put("plugin", intent.getStringExtra(SOURCE_PLUGIN_NAME))
                        put("status", SourceStatusListener.Status.values()[intent.getIntExtra(SOURCE_STATUS_CHANGED, 0)].name)
                        intent.getStringExtra(SOURCE_STATUS_NAME)?.let {
                            put("sourceName", it)
                        }
                    }
                    sourceStatusListeners.forEach {
                        it.next(result)
                    }
                }
            }
        }
    }

    private val bindListeners = Collections.synchronizedList(mutableListOf<CallbackContext>())
    private val serverStatusListeners = SynchronizedSparseArray<CallbackContext>()
    private val sourceStatusListeners = SynchronizedSparseArray<CallbackContext>()
    private val sendListeners = SynchronizedSparseArray<CallbackContext>()

    override fun execute(
        action: String,
        args: JSONArray,
        callbackContext: CallbackContext,
    ): Boolean {
        try {
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
        } catch (ex: Exception) {
            callbackContext.error("Failed on action $action: $ex")
        }

        return true
    }

    private fun setAllowedSourceIds(pluginName: String, sourceIds: List<String>, callbackContext: CallbackContext) {
        val radarService = radarService
        val provider = radarService.connections.find { it.pluginName == pluginName } ?: run {
            callbackContext.error("Plugin $pluginName not found to set source IDs for")
            return
        }
        radarService.setAllowedSourceIds(provider.connection, sourceIds)
        callbackContext.success()
    }

    private fun registerServerStatusListener(id: Int, callbackContext: CallbackContext) {
        serverStatusListeners[id] = callbackContext
    }

    private fun unregisterServerStatusListener(id: Int, callbackContext: CallbackContext) {
        serverStatusListeners -= id
        callbackContext.success()
    }

    private fun registerSourceStatusListener(id: Int, callbackContext: CallbackContext) {
        sourceStatusListeners[id] = callbackContext
    }

    private fun unregisterSourceStatusListener(id: Int, callbackContext: CallbackContext) {
        sourceStatusListeners -= id
        callbackContext.success()
    }

    private fun registerSendListener(id: Int, callbackContext: CallbackContext) {
        sendListeners[id] = callbackContext
    }

    private fun unregisterSendListener(id: Int, callbackContext: CallbackContext) {
        sendListeners -= id
        callbackContext.success()
    }

    private fun serverStatus(callbackContext: CallbackContext) {
        callbackContext.success(radarService.serverStatus.toString())
    }

    private fun sourceStatus(callbackContext: CallbackContext) {
        val result = JSONObject()
        radarService.plugins.forEach { provider ->
            result.put(
                provider.pluginName,
                JSONObject().apply {
                    put("plugin", provider.pluginName)
                    if (provider.isBound) {
                        put("status", provider.connection.sourceStatus)
                        provider.connection.sourceName?.let {
                            put("sourceName", it)
                        }
                    } else {
                        put("status", SourceStatusListener.Status.DISABLED)
                    }
                },
            )
        }
        callbackContext.success(result)
    }

    private fun recordsInCache(callbackContext: CallbackContext) {
        val dataHandler = radarService.dataHandler ?: run {
            callbackContext.error("Data handler is not active yet")
            return
        }

        val result = JSONObject()
        dataHandler.caches.forEach {
            val topic = it.readTopic.name
            result.put(topic, it.numberOfRecords + result.optLong(topic))
        }
        callbackContext.success(result)
    }

    private fun configure(callbackContext: CallbackContext, configuration: Map<String, String?>) {
        val toReset = mutableListOf<String>()
        configuration.forEach { (k, v) ->
            if (v == null) {
                toReset += k
            } else {
                config.put(k, v)
            }
        }
        if (toReset.isNotEmpty()) {
            config.reset(*toReset.toTypedArray())
        } else {
            config.persistChanges()
        }
        callbackContext.success()
    }

    private fun setAuthentication(auth: Authentication?, callbackContext: CallbackContext) {
        this.localAuthentication = auth
        syncAuthentication()
        callbackContext.success()
    }

    private fun startScanning(callbackContext: CallbackContext) {
        radarService.startScanning()
        callbackContext.success()
    }

    private fun syncAuthentication() {
        val binder = authServiceConnection.binder ?: return
        val auth = localAuthentication
        binder.updateState {
            if (auth == null) {
                invalidate()
                userId = null
                projectId = null
                token = null
                attributes -= BASE_URL_KEY
                headers.clear()
            } else {
                userId = auth.userId
                projectId = auth.projectId
                token = auth.token
                attributes[BASE_URL_KEY] = auth.baseUrl
                if (token != null) {
                    headers.removeAll { (header, _) -> header == "Authorization" }
                    headers.add("Authorization" to "Bearer $token")
                }
            }
        }
    }

    private fun start(callbackContext: CallbackContext) {
        bindListeners += callbackContext
        broadcastManager.registerReceiver(statusReceiver, IntentFilter().apply {
            addAction(SERVER_RECORDS_SENT_TOPIC)
            addAction(SOURCE_STATUS_CHANGED)
            addAction(SERVER_STATUS_CHANGED)
        })
        radarServiceConnection.bind()
        authServiceConnection.bind()
    }

    private fun stop(callbackContext: CallbackContext) {
        // Causes logout and also
        detach(true)
        callbackContext.success()
    }

    private fun stopScanning(callbackContext: CallbackContext) {
        radarService.stopScanning()
        callbackContext.success()
    }

    override fun initialize(cordova: CordovaInterface, webView: CordovaWebView) {
        super.initialize(cordova, webView)
        broadcastManager = LocalBroadcastManager.getInstance(cordova.context)
        config = RadarConfiguration.getInstance(cordova.context)
        radarServiceConnection = ManagedServiceConnection(cordova.context, RadarServiceImpl::class.java)
        radarServiceConnection.onBoundListeners += {
            synchronized(bindListeners) {
                bindListeners.forEach { it.success() }
                bindListeners.clear()
            }
        }
        authServiceConnection = ManagedServiceConnection(cordova.context, AuthService::class.java)
    }

    private fun detach(invalidate: Boolean) {
        radarServiceConnection.unbind()
        if (invalidate) {
            authServiceConnection.binder?.invalidate(null, false)
        }
        authServiceConnection.unbind()
        try {
            broadcastManager.unregisterReceiver(statusReceiver)
        } catch (ex: Exception) {
            Log.w(TAG, "Cannot unregister status receiver")
        }
        sourceStatusListeners.clear()
        sendListeners.clear()
        serverStatusListeners.clear()
        bindListeners.clear()
    }

    private fun permissionsNeeded(callbackContext: CallbackContext) {
        val result = JSONObject()
        radarService.connections.forEach { provider ->
            provider.permissionsNeeded.forEach { permission ->
                val pluginList = result.optJSONArray(permission)
                    ?: JSONArray().also { result.put(permission, it) }
                pluginList.put(provider.pluginName)
            }
            result.put(
                provider.pluginName,
                JSONObject().apply {
                    put("plugin", provider.pluginName)
                    if (provider.isBound) {
                        put("status", provider.connection.sourceStatus)
                        provider.connection.sourceName?.let {
                            put("sourceName", it)
                        }
                    } else {
                        put("status", SourceStatusListener.Status.DISABLED)
                    }
                },
            )
        }
        callbackContext.success(result)
    }

    private fun flushCaches(callbackContext: CallbackContext) {
        radarService.flushCaches(ContextFlushCallback(callbackContext))
    }

    private fun bluetoothNeeded(callbackContext: CallbackContext) {
        val result = JSONArray()
        radarService.connections.forEach { provider ->
            if (provider.permissionsNeeded.any { it in bluetoothPermissionSet }) {
                result.put(provider.pluginName)
            }
        }
        callbackContext.success(result)
    }

    private fun onAcquiredPermissions(callbackContext: CallbackContext) {
        radarService.startScanning()
        callbackContext.success()
    }

    override fun onDestroy() {
        super.onDestroy()
        detach(false)
    }

    companion object {
        private const val TAG = "RadarPassivePlugin"
        private fun JSONObject.toStringMap(): Map<String, String?> = buildMap(length()) {
            keys().forEach { key ->
                put(
                    key,
                    get(key).takeIf { it != JSONObject.NULL }?.toString()
                )
            }
        }
        private fun JSONArray.toStringList(): List<String> = buildList(length()) {
            repeat(length()) { idx ->
                add(getString(idx))
            }
        }

        val bluetoothPermissionSet = bluetoothPermissionList.toHashSet()
        private fun JSONObject.toAuthentication(): Authentication = Authentication(
            baseUrl = getString("baseUrl"),
            userId = getString("userId"),
            projectId = getString("projectId"),
            token = optString("token").takeUnless { it == "" || it == "null" },
        )

        private fun CallbackContext.next(result: String) {
            sendPluginResult(PluginResult(PluginResult.Status.OK, result).apply {
                keepCallback = true
            })
        }

        private fun CallbackContext.next(result: JSONObject) {
            sendPluginResult(PluginResult(PluginResult.Status.OK, result).apply {
                keepCallback = true
            })
        }
    }

    class RadarServiceImpl : RadarService() {
        override val plugins: List<SourceProvider<*>> = listOf(
            PhoneSensorProvider(this),
            PhoneUsageProvider(this),
            ApplicationStatusProvider(this),
            FarosProvider(this),
            E4Provider(this),
            WeatherApiProvider(this),
            OpenSmileAudioProvider(this),
        )

        override fun getLifecycle(): Lifecycle = lifecycle
    }

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
            sparseArray.valueIterator().forEach {
                block(it)
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

    data class Authentication(
        val baseUrl: String,
        val userId: String,
        val projectId: String,
        val token: String?,
    )
}
