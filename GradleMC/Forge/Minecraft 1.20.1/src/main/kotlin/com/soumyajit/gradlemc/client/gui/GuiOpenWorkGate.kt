package com.soumyajit.gradlemc.client.gui

import java.util.concurrent.atomic.AtomicBoolean

/** Keeps at most one off-thread GUI-open task queued on the client executor. */
internal class GuiOpenWorkGate {
    private val pending = AtomicBoolean(false)

    fun submit(schedule: (Runnable) -> Unit, work: Runnable): Boolean {
        if (!pending.compareAndSet(false, true)) return false
        try {
            schedule(Runnable {
                try {
                    work.run()
                } finally {
                    pending.set(false)
                }
            })
        } catch (failure: RuntimeException) {
            pending.set(false)
            throw failure
        }
        return true
    }
}
