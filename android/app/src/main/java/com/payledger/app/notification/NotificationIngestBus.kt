package com.payledger.app.notification

import com.payledger.app.data.model.CreateTransactionRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NotificationIngestBus {
    private val _events = MutableSharedFlow<CreateTransactionRequest>(extraBufferCapacity = 32)
    val events: SharedFlow<CreateTransactionRequest> = _events.asSharedFlow()

    private val _status = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val status: SharedFlow<String> = _status.asSharedFlow()

    fun publish(request: CreateTransactionRequest) {
        _events.tryEmit(request)
    }

    fun publishStatus(message: String) {
        _status.tryEmit(message)
    }
}
