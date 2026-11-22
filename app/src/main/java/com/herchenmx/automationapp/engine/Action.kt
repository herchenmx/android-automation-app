package com.herchenmx.automationapp.engine

/**
 * Performs a system change and can undo it.
 * execute() is called when the rule activates; revert() when it deactivates.
 */
interface Action {
    val id: String
    suspend fun execute()
    suspend fun revert()
}
