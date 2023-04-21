interface Window {
  RadarPassivePlugin: RadarPassivePlugin
}

interface RadarPassivePlugin {
  /**
   * Supply configuration parameters.
   */
  configure(configuration: {[key: string]: string})
  /**
   * Set authentication settings. Without this information, the plugin cannot start collecting
   * data and no data can be sent to the server. If null is provided, all plugins will remain active
   * but stop data collection and stop data sending.
   */
  updateAuthentication(auth?: Authentication)

  /**
   * Start the cordova plugin. This will set up all handlers. If the RadarPassivePlugin was already
   * running, this has no effect.
   */
  start(): void

  /** Start scanning for devices. This should be called to enable all configured plugins. */
  startScanning(): void

  /**
   * Stop scanning for devices. This will not affect any connected plugins, but it will stop any
   * plugins that are currently using bluetooth to scan for new devices.
   */
  stopScanning(): void

  /**
   * Completely stop the plugin. This will stop all plugins, remove the foreground service and stop
   * data sending.
   */
  stop(): void

  /**
   * Get the server status. If the status is UNAUTHENTICATED, provide a new access token via
   * {@see setAuthentication(Authentication)}.
   */
  getServerStatus(): ServerStatus;
  addServerStatusListener(listener: (ev: ServerStatus) => any);
  removeServerStatusListener(listener: (ev: ServerStatus) => any);

  /**
   * Get the status of the different plugins.
   */
  getSourceStatus(): {[plugin: string]: SourceStatus};
  addSourceStatusListener(listener: (ev: SourceStatusEvent) => any): void;
  removeSourceStatusListener(listener: (ev: SourceStatusEvent) => any): void;

  /** Get notified of any attempt to send data to the server. */
  addSendListener(listener: (ev: SendEvent) => any): void;
  removeSendListener(listener: (ev: SendEvent) => any): void;

  /**
   * Get the number of records in cache. This number is an approximation because
   * data have been sent while the function is called.
   */
  getCacheStatus(): {
    [topic: string]: number
  }

  /**
   * Permissions that are currently still required by the plugins. Only if all the permission
   * requests of a plugin have been met, can the plugin start. The key is the permission name and
   * the value is a list of plugins that require that permission.
   */
  permissionsNeeded(): {[permission: string]: string[]}

  /**
   * Whenever your app has acquired a permission, provide it here to notify the plugin.
   * @param permissions permission names.
   */
  onAcquiredPermissions(permissions: string[])

  /**
   * Which plugins require bluetooth to function. If this list is non-empty and Bluetooth is turned
   * off, then the app should request the user to turn on Bluetooth again.
   */
  bluetoothNeeded(): string[]

  /**
   * Configure given plugin to only connect to sources that match a substring of any of the provided
   * sourceIds. This is used mainly for Bluetooth devices, to limit the mac-address that they can
   * scan for.
   */
  setAllowedSourceIds(plugin: string, sourceIds: string[]): void

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
  /** Project ID. */
  sourceId: string
}

type ServerStatus = 'CONNECTING' | 'CONNECTED' | 'DISCONNECTED' | 'UPLOADING' | 'DISABLED' | 'READY' | 'UPLOADING_FAILED' | 'UNAUTHORIZED'

type SourceStatus = 'UNAVAILABLE' | 'CONNECTING' | 'DISCONNECTING' | 'DISCONNECTED' | 'CONNECTED' | 'READY' | 'DISABLED'

interface SourceStatusEvent {
  plugin: string
  status: SourceStatus
}

/** How many records are sent. */
interface SendEvent {
  status: 'SUCCESS' | 'ERROR'
  topic: string
  /** Number of records sent. If the status is ERROR, then this is null. */
  numberOfRecordsSent?: number
}

/** Callback on flushing data. */
interface FlushCachesCallback {
  success?: () => any
  error?: (message: string) => any
  /**
   * Track the progress of the flush. Current is the number of records sent, total is the number
   * of records still in cache.
   */
  progress?: (current: number, total: number) => any
}
