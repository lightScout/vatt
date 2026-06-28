package org.lightscout.vatt.core.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Decouples the HTTP layer (which detects a failed token refresh) from the auth repository / UI (which
 * react by routing to login). The client factory calls [notifyExpired]; the app observes [expired].
 */
class SessionEvents {
    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val expired: SharedFlow<Unit> = _expired

    fun notifyExpired() {
        _expired.tryEmit(Unit)
    }
}
