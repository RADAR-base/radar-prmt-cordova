package org.radarbase.cordova.plugin.passive

import org.radarbase.android.source.SourceStatusListener

data class SourceStatus(
    val plugin: String,
    val status: SourceStatusListener.Status,
    val sourceName: String? = null,
)

sealed class SendStatus(val topic: String)
class SendSuccess(topic: String, val numberOfRecords: Int) : SendStatus(topic)
class SendError(topic: String) : SendStatus(topic)

sealed interface FlushResult

object FlushSuccess : FlushResult
class FlushProgress(val current: Long, val total: Long) : FlushResult

data class Authentication(
    val baseUrl: String,
    val userId: String,
    val projectId: String,
    val token: String?,
)
