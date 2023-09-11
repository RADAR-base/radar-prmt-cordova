package org.radarbase.android.plugin

import android.util.SparseArray
import androidx.core.util.remove
import org.radarbase.android.data.DataHandler

class ResultListenerList<T> {
    private val sparseArray = SparseArray<ResultListener<T>>()

    @Synchronized
    fun next(value: T) {
        repeat(sparseArray.size()) { idx ->
            sparseArray.valueAt(idx).next(value)
        }
    }

    @Synchronized
    fun success(value: T) {
        repeat(sparseArray.size()) { idx ->
            sparseArray.valueAt(idx).success(value)
        }
        clear()
    }

    @Synchronized
    operator fun plusAssign(listener: ResultListener<T>) {
        sparseArray[listener.id ?: sparseArray.size()] = listener
    }

    @Synchronized
    operator fun minusAssign(id: Int) {
        sparseArray.remove(id)
    }

    @Synchronized
    operator fun minusAssign(listener: ResultListener<T>) {
        val idx = sparseArray.indexOfValue(listener)
        if (idx != -1) {
            sparseArray.remove(idx, listener)
        }
    }

    @Synchronized
    fun clear() {
        sparseArray.clear()
    }
}

internal class ContextFlushCallback(
    private val listener: ResultListener<FlushResult>
) : DataHandler.FlushCallback {
    override fun success() {
        listener.success(FlushSuccess)
    }
    override fun error(ex: Throwable) {
        listener.error(ex.toString())
    }
    override fun progress(current: Long, total: Long) {
        listener.next(FlushProgress(current, total))
    }
}
