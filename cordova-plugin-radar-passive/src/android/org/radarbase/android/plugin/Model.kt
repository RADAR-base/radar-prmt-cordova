package org.radarbase.android.plugin

import org.radarbase.android.source.SourceStatusListener

interface ResultListener<T> {
    val id: Int?
    fun next(value: T)
    fun error(message: String)
    fun success(value: T)
}

data class SourceStatus(
    val plugin: String,
    val status: SourceStatusListener.Status,
    val sourceName: String? = null,
)

sealed class SendStatus(val topic: String)
class SendSuccess(topic: String, val numberOfRecords: Long) : SendStatus(topic)
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
