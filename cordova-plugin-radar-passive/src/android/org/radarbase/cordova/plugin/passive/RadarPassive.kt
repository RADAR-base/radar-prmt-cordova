package org.radarbase.cordova.plugin.passive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.apache.cordova.CallbackContext
import org.json.JSONArray
import org.json.JSONObject
import org.radarbase.android.IRadarBinder
import org.radarbase.android.RadarConfiguration
import org.radarbase.android.RadarConfiguration.Companion.BASE_URL_KEY
import org.radarbase.android.auth.AuthService
import org.radarbase.android.kafka.ServerStatusListener
import org.radarbase.android.source.SourceService
import org.radarbase.android.source.SourceService.Companion.SERVER_RECORDS_SENT_TOPIC
import org.radarbase.android.source.SourceService.Companion.SERVER_STATUS_CHANGED
import org.radarbase.android.source.SourceService.Companion.SOURCE_STATUS_CHANGED
import org.radarbase.android.source.SourceStatusListener
import org.radarbase.android.util.BluetoothStateReceiver.Companion.bluetoothPermissionList
import org.radarbase.android.util.ManagedServiceConnection

/**
 * This class echoes a string called from JavaScript.
 */
class RadarPassive(context: Context) {
    private val broadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(context)
    private val radarServiceConnection: ManagedServiceConnection<IRadarBinder> = ManagedServiceConnection(context, RadarServiceImpl::class.java)
    private val authServiceConnection: ManagedServiceConnection<AuthService.AuthServiceBinder> = ManagedServiceConnection(context, AuthServiceImpl::class.java)

    private val config: RadarConfiguration = RadarConfiguration.getInstance(context)
    private var localAuthentication: Authentication? = null
    private val statusReceiver: BroadcastReceiver
    private val bindListeners = SynchronizedList<CallbackContext>()
    private val serverStatusListeners = SynchronizedSparseArray<CallbackContext>()
    private val sourceStatusListeners = SynchronizedSparseArray<CallbackContext>()
    private val sendListeners = SynchronizedSparseArray<CallbackContext>()

    private val radarService: IRadarBinder
        get() = checkNotNull(radarServiceConnection.binder) {
            "RadarService is not connected yet"
        }

    init {
        statusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action ?: return
                when (action) {
                    SERVER_RECORDS_SENT_TOPIC -> onSendEvent(
                        topic = intent.getStringExtra(SERVER_RECORDS_SENT_TOPIC) ?: return,
                        numberOfRecords = intent.getIntExtra(SourceService.SERVER_RECORDS_SENT_NUMBER, 0),
                    )
                    SERVER_STATUS_CHANGED -> onServerStatusEvent(
                        status = ServerStatusListener.Status.values()[intent.getIntExtra(SERVER_STATUS_CHANGED, 0)],
                    )
                    SOURCE_STATUS_CHANGED -> onSourceStatusEvent(
                        pluginName = intent.getStringExtra(SourceService.SOURCE_PLUGIN_NAME) ?: return,
                        status = SourceStatusListener.Status.values()[intent.getIntExtra(SOURCE_STATUS_CHANGED, 0)],
                        sourceName = intent.getStringExtra(SourceService.SOURCE_STATUS_NAME),
                    )
                }
            }
        }
        radarServiceConnection.onBoundListeners += {
            bindListeners.forEach { it.success() }
            bindListeners.clear()
        }
    }

    fun setAllowedSourceIds(pluginName: String, sourceIds: List<String>, callbackContext: CallbackContext) {
        val radarService = radarService
        val provider = radarService.connections.find { it.pluginName == pluginName } ?: run {
            callbackContext.error("Plugin $pluginName not found to set source IDs for")
            return
        }
        radarService.setAllowedSourceIds(provider.connection, sourceIds)
        callbackContext.success()
    }

    fun registerServerStatusListener(id: Int, callbackContext: CallbackContext) {
        serverStatusListeners[id] = callbackContext
    }

    fun unregisterServerStatusListener(id: Int, callbackContext: CallbackContext) {
        serverStatusListeners -= id
        callbackContext.success()
    }

    fun registerSourceStatusListener(id: Int, callbackContext: CallbackContext) {
        sourceStatusListeners[id] = callbackContext
    }

    fun unregisterSourceStatusListener(id: Int, callbackContext: CallbackContext) {
        sourceStatusListeners -= id
        callbackContext.success()
    }

    fun registerSendListener(id: Int, callbackContext: CallbackContext) {
        sendListeners[id] = callbackContext
    }

    fun unregisterSendListener(id: Int, callbackContext: CallbackContext) {
        sendListeners -= id
        callbackContext.success()
    }

    fun serverStatus(callbackContext: CallbackContext) {
        callbackContext.success(radarService.serverStatus.toString())
    }

    fun sourceStatus(callbackContext: CallbackContext) {
        val result = JSONObject()
        radarService.plugins.forEach { provider ->
            result.put(
                provider.pluginName,
                jsonObject {
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

    fun recordsInCache(callbackContext: CallbackContext) {
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

    fun configure(callbackContext: CallbackContext, configuration: Map<String, String?>) {
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

    fun setAuthentication(auth: Authentication?, callbackContext: CallbackContext) {
        this.localAuthentication = auth
        syncAuthentication()
        callbackContext.success()
    }

    fun startScanning(callbackContext: CallbackContext) {
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

    fun start(callbackContext: CallbackContext) {
        bindListeners += callbackContext
        broadcastManager.registerReceiver(statusReceiver, IntentFilter().apply {
            addAction(SERVER_RECORDS_SENT_TOPIC)
            addAction(SOURCE_STATUS_CHANGED)
            addAction(SERVER_STATUS_CHANGED)
        })
        radarServiceConnection.bind()
        authServiceConnection.bind()
    }

    fun stop(callbackContext: CallbackContext) {
        // Causes logout and also
        detach(true)
        callbackContext.success()
    }

    fun stopScanning(callbackContext: CallbackContext) {
        radarService.stopScanning()
        callbackContext.success()
    }

    fun permissionsNeeded(callbackContext: CallbackContext) {
        val result = JSONObject()
        radarService.connections.forEach { provider ->
            provider.permissionsNeeded.forEach { permission ->
                val pluginList = result.optJSONArray(permission)
                    ?: JSONArray().also { result.put(permission, it) }
                pluginList.put(provider.pluginName)
            }
            result.put(
                provider.pluginName,
                jsonObject {
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

    fun flushCaches(callbackContext: CallbackContext) {
        radarService.flushCaches(ContextFlushCallback(callbackContext))
    }

    fun bluetoothNeeded(callbackContext: CallbackContext) {
        val result = JSONArray()
        radarService.connections.forEach { provider ->
            if (provider.permissionsNeeded.any { it in bluetoothPermissionSet }) {
                result.put(provider.pluginName)
            }
        }
        callbackContext.success(result)
    }

    fun onAcquiredPermissions(callbackContext: CallbackContext) {
        radarService.startScanning()
        callbackContext.success()
    }

    fun onSourceStatusEvent(
        pluginName: String,
        status: SourceStatusListener.Status,
        sourceName: String?,
    ) {
        val result = jsonObject {
            put("plugin", pluginName)
            put("status", status.name)
            if (sourceName != null) {
                put("sourceName", sourceName)
            }
        }
        sourceStatusListeners.forEach {
            it.next(result)
        }
    }

    fun onServerStatusEvent(
        status: ServerStatusListener.Status,
    ) {
        serverStatusListeners.forEach {
            it.next(status.name)
        }
    }

    fun onSendEvent(topic: String, numberOfRecords: Int) {
        val result = jsonObject {
            put("topic", topic)
            if (numberOfRecords >= 0) {
                put("status", "SUCCESS")
                put("numberOfRecords", numberOfRecords)
            } else {
                put("status", "ERROR")
            }
        }

        sendListeners.forEach {
            it.next(result)
        }
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

    fun onDestroy() {
        detach(false)
    }

    companion object {
        private const val TAG = "RadarPassivePlugin"
        val bluetoothPermissionSet = bluetoothPermissionList.toHashSet()
    }
}
