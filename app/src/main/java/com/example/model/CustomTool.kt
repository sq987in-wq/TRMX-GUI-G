package com.example.model

enum class ToolInputType {
    NONE,
    TEXT,
    URL,
    FILE,
    FILE_PATH,
    DIRECTORY,
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
    val title: String,
    val category: String = "Custom",
    val executable: String,
    val argsTemplate: String = "",
    val inputType: String = "TEXT", // NONE, TEXT, FILE_PATH, DIRECTORY
    val accentColor: String = "#06B6D4",
    val isPinned: Boolean = false,
    val description: String = "",
    val icon: String = "terminal",
    val inputDefinitions: List<ToolInputDefinition> = emptyList(),
    val arguments: List<String> = emptyList(),
    val enabled: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Backward compatibility aliases
    val name: String get() = title
    val accentColorHex: String get() = accentColor

    /**
     * Resolves command argument list for execution.
     */
    fun resolveArguments(inputs: Map<String, String>): List<String> {
        if (arguments.isNotEmpty()) {
            return arguments.map { arg ->
                var resolved = arg
                inputs.forEach { (k, v) ->
                    resolved = resolved.replace("{$k}", v)
                }
                resolved
            }
        }
        if (argsTemplate.isNotEmpty()) {
            val tokens = splitArgs(argsTemplate)
            return tokens.map { token ->
                var resolved = token
                inputs.forEach { (k, v) ->
                    resolved = resolved.replace("{$k}", v)
                }
                if (resolved.contains("{input}") && inputs.isNotEmpty()) {
                    val defaultVal = inputs["input"] ?: inputs.values.firstOrNull() ?: ""
                    resolved = resolved.replace("{input}", defaultVal)
                }
                resolved
            }
        }
        return emptyList()
    }

    companion object {
        fun splitArgs(template: String): List<String> {
            val list = mutableListOf<String>()
            val regex = Regex("\"([^\"]*)\"|'([^']*)'|(\\S+)")
            val matches = regex.findAll(template)
            for (m in matches) {
                when {
                    m.groups[1] != null -> list.add(m.groups[1]!!.value)
                    m.groups[2] != null -> list.add(m.groups[2]!!.value)
                    m.groups[3] != null -> list.add(m.groups[3]!!.value)
                }
            }
            return if (list.isNotEmpty()) list else template.split(" ").filter { it.isNotBlank() }
        }
    }
}
