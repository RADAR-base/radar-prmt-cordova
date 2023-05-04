package org.radarbase.cordova.plugin.passive

import org.radarbase.android.RadarService
import org.radarbase.android.source.SourceProvider
import org.radarbase.monitor.application.ApplicationStatusProvider
import org.radarbase.passive.audio.OpenSmileAudioProvider
import org.radarbase.passive.bittium.FarosProvider
import org.radarbase.passive.empatica.E4Provider
import org.radarbase.passive.phone.PhoneBluetoothProvider
import org.radarbase.passive.phone.PhoneContactListProvider
import org.radarbase.passive.phone.PhoneLocationProvider
import org.radarbase.passive.phone.PhoneSensorProvider
import org.radarbase.passive.phone.usage.PhoneUsageProvider
import org.radarbase.passive.weather.WeatherApiProvider

class RadarServiceImpl : RadarService() {
    override val plugins: List<SourceProvider<*>> = listOf(
        PhoneSensorProvider(this),
        PhoneBluetoothProvider(this),
        PhoneUsageProvider(this),
        PhoneContactListProvider(this),
        PhoneLocationProvider(this),
        ApplicationStatusProvider(this),
        FarosProvider(this),
        E4Provider(this),
        WeatherApiProvider(this),
        OpenSmileAudioProvider(this),
    )
}
