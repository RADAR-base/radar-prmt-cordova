var exec = require('cordova/exec');

var RadarPassivePlugin = function () {
    this._serverStatusListenerId = 0
    this._sourceStatusListenerId = 0
    this._sendListenerId = 0
    this._pluginListenerId = 0
}

RadarPassivePlugin.prototype._error = function (err) {
    console.log("Failed to call RadarPassivePlugin: " + err)
}

RadarPassivePlugin.prototype.exec = function(command, callback, args) {
    exec(
        callback ? callback['success'] : null,
        (callback ? callback['error'] : null) || this._error,
        'RadarPassivePlugin',
        command,
        args || [],
    )
}

/**
 * Supply configuration parameters.
 */
RadarPassivePlugin.prototype.configure = function(configuration, callback) {
    this.exec('configure', callback, [configuration])
}

/**
 * Set authentication settings. Without this information, the plugin cannot start collecting
 * data and no data can be sent to the server. If null is provided, all plugins will remain active
 * but stop data collection and stop data sending.
 */
RadarPassivePlugin.prototype.setAuthentication = function(auth, callback) {
    this.exec('setAuthentication', callback, [auth])
}

/**
 * Start the cordova plugin. This will set up all handlers. If the RadarPassivePlugin was already
 * running, this has no effect.
 */
RadarPassivePlugin.prototype.start = function (callback) {
    this.exec('start', callback)
}

/** Start scanning for devices. This should be called to enable all configured plugins. */
RadarPassivePlugin.prototype.startScanning = function (callback) {
    this.exec('startScanning', callback)
}

/**
 * Stop scanning for devices. This will not affect any connected plugins, but it will stop any
 * plugins that are currently using bluetooth to scan for new devices.
 */
RadarPassivePlugin.prototype.stopScanning = function (callback) {
    this.exec('stopScanning', callback)
}

/**
 * Completely stop the plugin. This will stop all plugins, remove the foreground service and stop
 * data sending.
 */
RadarPassivePlugin.prototype.stop = function (callback) {
    this.exec('stop', callback)
}

/**
 * Get the server status. If the status is UNAUTHENTICATED, provide a new access token via
 * {@see setAuthentication(Authentication)}.
 */
RadarPassivePlugin.prototype.serverStatus = function(callback) {
    this.exec('serverStatus', callback)
}

RadarPassivePlugin.prototype._registerListener = function(id, type, callback) {
    var _exec = this.exec
    _exec('register' + type + 'Listener', callback, [id])
    return {
        unregister: function (callback) {
            _exec('unregister' + type + 'Listener', callback, [id])
        }
    }
}

RadarPassivePlugin.prototype.registerServerStatusListener = function(callback) {
    return this._registerListener(++this._serverStatusListenerId, 'ServerStatus', callback)
}

/**
 * Get the status of the different plugins.
 */
RadarPassivePlugin.prototype.sourceStatus = function(callback) {
    this.exec('sourceStatus', callback)
}

RadarPassivePlugin.prototype.registerSourceStatusListener = function(callback) {
    return this._registerListener(++this._sourceStatusListenerId, 'SourceStatus', callback)
}

/** Get notified of any attempt to send data to the server. */

RadarPassivePlugin.prototype.registerSendListener = function(callback) {
    return this._registerListener(++this._sendListenerId, 'Send', callback)
}


RadarPassivePlugin.prototype.registerPluginListener = function(callback) {
    return this._registerListener(++this._pluginListenerId, 'Plugin', callback)
}

/**
 * Get the number of records in cache. This number is an approximation because
 * data have been sent while the function is called.
 */
RadarPassivePlugin.prototype.recordsInCache = function (callback) {
    this.exec('recordsInCache', callback)
}

/**
 * Permissions that are currently still required by the plugins. Only if all the permission
 * requests of a plugin have been met, can the plugin start. The key is the permission name and
 * the value is a list of plugins that require that permission.
 */
RadarPassivePlugin.prototype.permissionsNeeded = function (callback) {
    this.exec('permissionsNeeded', callback)
}

/**
 * Whenever your app has acquired a permission, provide it here to notify the plugin.
 */
RadarPassivePlugin.prototype.onAcquiredPermissions = function (permissions, callback) {
    this.exec('onAcquiredPermissions', callback, [permissions])
}

/**
 * Which plugins require bluetooth to function. If this list is non-empty and Bluetooth is turned
 * off, then the app should request the user to turn on Bluetooth again.
 */
RadarPassivePlugin.prototype.bluetoothNeeded = function (callback) {
    this.exec('bluetoothNeeded', callback)
}

/**
 * Configure given plugin to only connect to sources that match a substring of any of the provided
 * sourceIds. This is used mainly for Bluetooth devices, to limit the mac-address that they can
 * scan for.
 */
RadarPassivePlugin.prototype.setAllowedSourceIds = function (plugin, sourceIds, callback) {
    this.exec('setAllowedSourceIds', callback, [plugin, sourceIds])
}

/**
 * Flush cashes. This can be useful to run from settings or when a user logs out. If
 */
RadarPassivePlugin.prototype.flushCaches = function (callback) {
    exec(
        function(result) {
            if (result['type'] === 'progress' && callback['progress']) {
                callback['progress'](result['current'], result['total'])
            } else if (result['type'] === 'success' && callback['success']) {
                callback['success']()
            }
        },
        callback['error'],
        'RadarPassivePlugin',
        'flushCaches',
        []
    )
}

var radarPassivePlugin = new RadarPassivePlugin()

module.exports = radarPassivePlugin;
