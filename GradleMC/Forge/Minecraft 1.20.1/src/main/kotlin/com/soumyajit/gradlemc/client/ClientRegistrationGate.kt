package com.soumyajit.gradlemc.client

/** Sets its registered state only after the registration action completes successfully. */
internal class ClientRegistrationGate {
    private var registered = false

    @Synchronized
    fun registerOnce(action: () -> Unit): Boolean {
        if (registered) return false
        action()
        registered = true
        return true
    }
}
