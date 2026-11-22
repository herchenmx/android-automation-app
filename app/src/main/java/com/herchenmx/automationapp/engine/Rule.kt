package com.herchenmx.automationapp.engine

data class Rule(
    val id: String,
    val name: String,
    val triggers: List<Trigger>,
    val conditions: List<Condition> = emptyList(),
    val actions: List<Action>,
    var isEnabled: Boolean = true,
)
