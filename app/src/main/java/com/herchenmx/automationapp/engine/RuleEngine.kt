package com.herchenmx.automationapp.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Evaluates active rules and coordinates triggers, conditions, and actions.
 *
 * Multi-trigger rules require ALL triggers to be active simultaneously (AND logic).
 * Actions fire on activation and revert on deactivation.
 */
class RuleEngine(
    private val stateManager: StateManager,
    private val scope: CoroutineScope,
) {
    private val rules: MutableList<Rule> = mutableListOf()
    private val triggerActiveCount: ConcurrentHashMap<String, Int> = ConcurrentHashMap()
    private val ruleActive: ConcurrentHashMap<String, Boolean> = ConcurrentHashMap()

    fun addRule(rule: Rule) {
        rules.add(rule)
        triggerActiveCount[rule.id] = 0
        ruleActive[rule.id] = false
    }

    fun startAll() {
        rules.filter { it.isEnabled }.forEach { rule ->
            Timber.d("RuleEngine: starting rule '${rule.name}'")
            rule.triggers.forEach { trigger ->
                try {
                    trigger.start { isActive -> onTriggerChanged(rule, trigger, isActive) }
                } catch (e: Exception) {
                    Timber.e(e, "RuleEngine: failed to start trigger '${trigger.id}' for rule '${rule.name}'")
                }
            }
        }
    }

    fun stopAll() {
        rules.forEach { rule -> rule.triggers.forEach { it.stop() } }
        Timber.d("RuleEngine: all rules stopped")
    }

    private fun onTriggerChanged(rule: Rule, trigger: Trigger, isActive: Boolean) {
        if (!rule.isEnabled) return
        Timber.d("RuleEngine: trigger '${trigger.id}' -> $isActive for rule '${rule.name}'")

        val current = triggerActiveCount.getOrDefault(rule.id, 0)
        val updated = if (isActive) current + 1 else maxOf(0, current - 1)
        triggerActiveCount[rule.id] = updated

        val shouldBeActive = updated == rule.triggers.size
        val wasActive = ruleActive[rule.id] == true

        when {
            shouldBeActive && !wasActive -> {
                val conditionsMet = rule.conditions.all { it.isMet() }
                if (conditionsMet) {
                    ruleActive[rule.id] = true
                    scope.launch(Dispatchers.IO) {
                        Timber.d("RuleEngine: activating rule '${rule.name}'")
                        rule.actions.forEach { it.execute() }
                    }
                }
            }
            !shouldBeActive && wasActive -> {
                ruleActive[rule.id] = false
                scope.launch(Dispatchers.IO) {
                    Timber.d("RuleEngine: deactivating rule '${rule.name}'")
                    rule.actions.forEach { it.revert() }
                }
            }
        }
    }
}
