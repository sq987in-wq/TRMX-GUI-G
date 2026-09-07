package com.example.model

enum class ToolInputType {
    NONE,
    TEXT,
    URL,
    FILE,
    MULTI_FILE
}

data class ToolInputDefinition(
    val key: String,
    val label: String,
    val type: ToolInputType,
    val defaultValue: String = "",
    val placeholder: String = "",
    val required: Boolean = true
)

typealias InputType = ToolInputType

data class CustomTool(
    val id: String,
    val name: String,
    val icon: String,
    val accentColorHex: String = "#06B6D4",
    val description: String,
    val inputDefinitions: List<ToolInputDefinition> = emptyList(),
    val executable: String,
    val arguments: List<String> = emptyList(),
    val enabled: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val accentColor: String get() = accentColorHex
}
