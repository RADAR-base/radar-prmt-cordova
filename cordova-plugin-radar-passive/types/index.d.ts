interface Window {
  RadarPassivePlugin: RadarPassivePlugin
}

interface RadarPassivePlugin {
  exec<T>(command: string, callback?: RadarPassiveCallback<T>, args?: any[])

  /**
   * Supply configuration parameters.
   */
  configure(configuration: {[key: string]: string}, callback?: RadarPassiveCallback<void>)
  /**
   * Set authentication settings. Without this information, the plugin cannot start collecting
   * data and no data can be sent to the server. If null is provided, all plugins will remain active
   * but stop data collection and stop data sending.
   */
  setAuthentication(auth?: Authentication, callback?: RadarPassiveCallback<void>)

  /**
   * Start the cordova plugin. This will set up all handlers. If the RadarPassivePlugin was already
   * running, this has no effect.
   */
  start(callback?: RadarPassiveCallback<void>)

  /** Start scanning for devices. This should be called to enable all configured plugins. */
  startScanning(callback?: RadarPassiveCallback<void>)

  /**
   * Stop scanning for devices. This will not affect any connected plugins, but it will stop any
   * plugins that are currently using bluetooth to scan for new devices.
   */
  stopScanning(callback?: RadarPassiveCallback<void>)

  /**
   * Completely stop the plugin. This will stop all plugins, remove the foreground service and stop
   * data sending.
   */
  stop(callback?: RadarPassiveCallback<void>)

  /**
   * Get the server status. If the status is UNAUTHENTICATED, provide a new access token via
   * {@see setAuthentication(Authentication)}.
   */
  serverStatus(callback: RadarPassiveCallback<ServerStatus>)

  /** Add a server status listener.
   *
   * @return registration that should be unregistered when no longer needed.
   */
  registerServerStatusListener(callback: RadarPassiveCallback<ServerStatus>): ListenerRegistration

  /**
   * Get the status of the different plugins.
   */
  sourceStatus(callback: RadarPassiveCallback<{[plugin: string]: SourceStatusEvent}>)
  registerSourceStatusListener(callback: RadarPassiveCallback<SourceStatusEvent>): ListenerRegistration

  /** Get notified of any attempt to send data to the server. */
  registerSendListener(callback: RadarPassiveCallback<SendEvent>): ListenerRegistration

  /**
   * Get the number of records in cache. This number is an approximation because
   * data have been sent while the function is called.
   */
  recordsInCache(callback: RadarPassiveCallback<{[topic: string]: number}>)

  /**
   * Permissions that are currently still required by the plugins. Only if all the permission
   * requests of a plugin have been met, can the plugin start. The key is the permission name and
   * the value is a list of plugins that require that permission.
   */
  permissionsNeeded(callback: RadarPassiveCallback<{[permission: string]: string[]}>)

  /**
   * Whenever your app has acquired a permission, provide it here to notify the plugin.
   */
  onAcquiredPermissions(permissions: string[], callback?: RadarPassiveCallback<void>)

  /**
   * Which plugins require bluetooth to function. If this list is non-empty and Bluetooth is turned
   * off, then the app should request the user to turn on Bluetooth again.
   */
  bluetoothNeeded(callback: RadarPassiveCallback<string[]>)

  /**
   * Configure given plugin to only connect to sources that match a substring of any of the provided
   * sourceIds. This is used mainly for Bluetooth devices, to limit the mac-address that they can
   * scan for.
   */
  setAllowedSourceIds(plugin: string, sourceIds: string[], callback?: RadarPassiveCallback<void>)

  /**
   * Flush cashes. This can be useful to run from settings or when a user logs out. If
   */
  flushCaches(callback: FlushCachesCallback)
}

/** Authentication infromation. */
interface Authentication {
  /** Base URL for RADAR-base, without trailing slash. */
  baseUrl: string
  /** Access token for RADAR-base. If not available, no data will be sent. */
  token: string | null
  /** Project ID. */
  projectId: string
  /** User ID. */
  userId: string
}

type ServerStatus = 'CONNECTING' | 'CONNECTED' | 'DISCONNECTED' | 'UPLOADING' | 'DISABLED' | 'READY' | 'UPLOADING_FAILED' | 'UNAUTHORIZED'

type SourceStatus = 'UNAVAILABLE' | 'CONNECTING' | 'DISCONNECTING' | 'DISCONNECTED' | 'CONNECTED' | 'READY' | 'DISABLED'

interface SourceStatusEvent {
  plugin: string
  status: SourceStatus
  sourceName?: string
}

/** How many records are sent. */
interface SendEvent {
  status: 'SUCCESS' | 'ERROR'
  topic: string
  /** Number of records sent. If the status is ERROR, then this is null. */
  numberOfRecords?: number
}

/** Callback on flushing data. */
interface RadarPassiveCallback<T> {
  success?: (value: T) => any
  error?: (message: string) => any
}

/** Callback on flushing data. */
interface FlushCachesCallback extends RadarPassiveCallback<void> {
  /**
   * Track the progress of the flush. Current is the number of records sent, total is the number
   * of records still in cache.
   */
  progress?: (current: number, total: number) => any
}

interface ListenerRegistration {
  unregister(callback: RadarPassiveCallback<void>)
}
