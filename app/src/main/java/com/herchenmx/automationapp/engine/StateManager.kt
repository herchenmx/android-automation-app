package com.herchenmx.automationapp.engine

import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Saves and restores named state values using a per-key stack.
 *
 * When multiple rules modify the same state key, each rule pushes its own saved value.
 * Reverting pops the most recent save, so overlapping rules always restore to the
 * value that existed at the moment each individual rule activated — no conflicts.
 */
class StateManager {
    private val stacks: ConcurrentHashMap<String, ArrayDeque<Any>> = ConcurrentHashMap()

    fun <T : Any> save(key: String, value: T) {
        stacks.getOrPut(key) { ArrayDeque() }.addLast(value)
        Timber.d("StateManager: saved key=$key value=$value depth=${stacks[key]?.size}")
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> restore(key: String): T? {
        val stack = stacks[key] ?: return null
        val value = if (stack.isNotEmpty()) stack.removeLast() as? T else null
        Timber.d("StateManager: restored key=$key value=$value depth=${stack.size}")
        return value
    }

    fun hasState(key: String): Boolean = (stacks[key]?.size ?: 0) > 0
}
