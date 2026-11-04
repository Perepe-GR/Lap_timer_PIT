package com.ejemplo.laptimer.logic

/**
 * Event bus and listener coordination for MainActivity components.
 */
class MainEventBus {
    private val listeners = mutableMapOf<String, (Any?) -> Unit>()

    fun register(event: String, callback: (Any?) -> Unit) {
        listeners[event] = callback
    }

    fun unregister(event: String) {
        listeners.remove(event)
    }

    fun dispatch(event: String, payload: Any? = null) {
        listeners[event]?.invoke(payload)
    }

    fun clear() {
        listeners.clear()
    }
}
