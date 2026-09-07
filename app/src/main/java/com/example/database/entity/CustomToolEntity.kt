package com.example.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.CustomTool
import com.example.model.ToolInputDefinition
import com.example.model.ToolInputType
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "custom_tools")
data class CustomToolEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val accentColorHex: String,
    val description: String,
    val inputDefinitionsJson: String,
    val executable: String,
    val argumentsJson: String,
    val enabled: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): CustomTool {
        val inputDefs = parseInputsJson(inputDefinitionsJson)
        val args = parseArgsJson(argumentsJson)
        return CustomTool(
            id = id,
            name = name,
            icon = icon,
            accentColorHex = accentColorHex,
            description = description,
            inputDefinitions = inputDefs,
            executable = executable,
            arguments = args,
            enabled = enabled,
            sortOrder = sortOrder,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(tool: CustomTool): CustomToolEntity {
            return CustomToolEntity(
                id = tool.id,
                name = tool.name,
                icon = tool.icon,
                accentColorHex = tool.accentColorHex,
                description = tool.description,
                inputDefinitionsJson = serializeInputs(tool.inputDefinitions),
                executable = tool.executable,
                argumentsJson = serializeArgs(tool.arguments),
                enabled = tool.enabled,
                sortOrder = tool.sortOrder,
                createdAt = tool.createdAt,
                updatedAt = tool.updatedAt
            )
        }

        fun parseInputsJson(json: String): List<ToolInputDefinition> {
            if (json.isBlank()) return emptyList()
            return try {
                val array = JSONArray(json)
                val list = mutableListOf<ToolInputDefinition>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ToolInputDefinition(
                            key = obj.optString("key", ""),
                            label = obj.optString("label", ""),
                            type = try {
                                ToolInputType.valueOf(obj.optString("type", "TEXT"))
                            } catch (_: Exception) {
                                ToolInputType.TEXT
                            },
                            defaultValue = obj.optString("defaultValue", ""),
                            placeholder = obj.optString("placeholder", ""),
                            required = obj.optBoolean("required", true)
                        )
                    )
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun serializeInputs(inputs: List<ToolInputDefinition>): String {
            val array = JSONArray()
            inputs.forEach { def ->
                val obj = JSONObject()
                obj.put("key", def.key)
                obj.put("label", def.label)
                obj.put("type", def.type.name)
                obj.put("defaultValue", def.defaultValue)
                obj.put("placeholder", def.placeholder)
                obj.put("required", def.required)
                array.put(obj)
            }
            return array.toString()
        }

        fun parseArgsJson(json: String): List<String> {
            if (json.isBlank()) return emptyList()
            return try {
                val array = JSONArray(json)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun serializeArgs(args: List<String>): String {
            val array = JSONArray()
            args.forEach { array.put(it) }
            return array.toString()
        }
    }
}
