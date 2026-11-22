package com.herchenmx.automationapp.engine

/**
 * Optional constraint evaluated at rule activation time.
 * All conditions in a Rule must be met before actions fire.
 */
interface Condition {
    val id: String
    fun isMet(): Boolean
}
