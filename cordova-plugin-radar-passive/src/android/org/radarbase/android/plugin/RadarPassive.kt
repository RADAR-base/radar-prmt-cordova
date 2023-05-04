package org.radarbase.android.plugin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.radarbase.android.IRadarBinder
import org.radarbase.android.RadarConfiguration
import org.radarbase.android.RadarConfiguration.Companion.BASE_URL_KEY
import org.radarbase.android.RadarService
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
class RadarPassive(
    context: Context,
    radarServiceClass: Class<out RadarService>,
    authServiceClass: Class<out AuthService>,
) {
    private val broadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(context)
    private val radarServiceConnection: ManagedServiceConnection<IRadarBinder> = ManagedServiceConnection(context, radarServiceClass)
    private val authServiceConnection: ManagedServiceConnection<AuthService.AuthServiceBinder> = ManagedServiceConnection(context, authServiceClass)

    private val config: RadarConfiguration = RadarConfiguration.getInstance(context)
    private var localAuthentication: Authentication? = null
    private val statusReceiver: BroadcastReceiver
    private val bindListeners = ResultListenerList<Unit>()
    val serverStatusListeners = ResultListenerList<ServerStatusListener.Status>()
    val sourceStatusListeners = ResultListenerList<SourceStatus>()
    val sendListeners = ResultListenerList<SendStatus>()

    private val radarService: IRadarBinder
        get() = checkNotNull(radarServiceConnection.binder) {
            "RadarService is not connected yet"
        }

    init {
        AuthService.authServiceClass = authServiceClass
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
            bindListeners.success(Unit)
        }
        authServiceConnection.onBoundListeners += {
            syncAuthentication()
        }
    }

    fun configure(configuration: Map<String, String?>) {
        Log.i(TAG, "Setting configuration $configuration")
        val toReset = mutableListOf<String>()
        configuration.forEach { (k, v) ->
            if (v.isNullOrEmpty()) {
                toReset += k
            } else {
                config.put(k, v)
            }
        }
        if (toReset.isNotEmpty()) {
            config.reset(*toReset.toTypedArray())
        }
        config.persistChanges()
    }

    fun setAuthentication(auth: Authentication?) {
        this.localAuthentication = auth
        syncAuthentication()
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

    fun start(resultListener: ResultListener<Unit>) {
        bindListeners += resultListener
        broadcastManager.registerReceiver(statusReceiver, IntentFilter().apply {
            addAction(SERVER_RECORDS_SENT_TOPIC)
            addAction(SOURCE_STATUS_CHANGED)
            addAction(SERVER_STATUS_CHANGED)
        })
        radarServiceConnection.bind()
        authServiceConnection.bind()
    }

    fun startScanning() {
        radarService.startScanning()
    }

    fun stopScanning() {
        radarService.stopScanning()
    }

    fun stop() {
        // Causes logout and also stops foreground service
        detach(true)
    }

    fun onDestroy() {
        detach(false)
    }

    fun permissionsNeeded(): Map<String, List<String>> = buildMap {
        radarService.connections.forEach { provider ->
            provider.permissionsNeeded.forEach { permission ->
                val pluginList = computeIfAbsent(permission) { mutableListOf() } as MutableList<String>
                pluginList += provider.pluginName
            }
        }
    }

    fun flushCaches(resultListener: ResultListener<FlushResult>) {
        radarService.flushCaches(ContextFlushCallback(resultListener))
    }

    fun bluetoothNeeded(): List<String> = buildList {
        radarService.connections.forEach { provider ->
            if (provider.permissionsNeeded.any { it in bluetoothPermissionSet }) {
                add(provider.pluginName)
            }
        }
    }

    fun onAcquiredPermissions() {
        radarService.startScanning()
    }

    fun onSourceStatusEvent(
        pluginName: String,
        status: SourceStatusListener.Status,
        sourceName: String?,
    ) {
        sourceStatusListeners.next(SourceStatus(pluginName, status, sourceName))
    }

    fun onServerStatusEvent(
        status: ServerStatusListener.Status,
    ) {
        serverStatusListeners.next(status)
    }

    fun onSendEvent(topic: String, numberOfRecords: Int) {
        val result = if (numberOfRecords >= 0) {
            SendSuccess(topic, numberOfRecords)
        } else {
            SendError(topic)
        }

        sendListeners.next(result)
    }

    fun setAllowedSourceIds(pluginName: String, sourceIds: List<String>) {
        val radarService = radarService
        val provider = requireNotNull(radarService.connections.find { it.pluginName == pluginName }) {
            "Plugin $pluginName not found to set source IDs for"
        }
        radarService.setAllowedSourceIds(provider.connection, sourceIds)
    }

    fun serverStatus() = radarService.serverStatus

    fun sourceStatus(): List<SourceStatus> = radarService.plugins.map { provider ->
        SourceStatus(
            plugin = provider.pluginName,
            status = if (provider.isBound) {
                provider.connection.sourceStatus
                    ?: SourceStatusListener.Status.DISCONNECTED
            } else {
                SourceStatusListener.Status.DISABLED
            },
            sourceName = if (provider.isBound) provider.connection.sourceName else null
        )
    }

    fun recordsInCache(): Map<String, Long> {
        val dataHandler = checkNotNull(radarService.dataHandler) { "Data handler is not active yet" }
        val caches = dataHandler.caches

        return buildMap(caches.size) {
            caches.forEach {
                val topic = it.readTopic.name
                put(topic, it.numberOfRecords + (get(topic) ?: 0L))
            }
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

    companion object {
        private const val TAG = "RadarPassivePlugin"
        val bluetoothPermissionSet = bluetoothPermissionList.toHashSet()
    }
}
