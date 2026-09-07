package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.database.dao.CustomToolDao
import com.example.database.dao.RecentJobDao
import com.example.database.dao.SavedOutputDao
import com.example.database.entity.CustomToolEntity
import com.example.database.entity.RecentJobEntity
import com.example.database.entity.SavedOutputEntity
import com.example.model.CustomTool
import com.example.model.ToolInputDefinition
import com.example.model.ToolInputType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CustomToolEntity::class,
        RecentJobEntity::class,
        SavedOutputEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customToolDao(): CustomToolDao
    abstract fun recentJobDao(): RecentJobDao
    abstract fun savedOutputDao(): SavedOutputDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "termux_commanddeck.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDefaultTools(database.customToolDao())
                    }
                }
            }

            private suspend fun populateDefaultTools(dao: CustomToolDao) {
                val defaultTools = listOf(
                    CustomTool(
                        id = "tool_apktool_decompile",
                        name = "APK Decompile",
                        icon = "build",
                        accentColorHex = "#A855F7",
                        description = "Decompile an APK file to inspect smali and resources",
                        inputDefinitions = listOf(
                            ToolInputDefinition(
                                key = "input",
                                label = "APK File Path",
                                type = ToolInputType.FILE,
                                placeholder = "/path/to/target.apk"
                            ),
                            ToolInputDefinition(
                                key = "output",
                                label = "Output Directory",
                                type = ToolInputType.TEXT,
                                placeholder = "out_decompiled"
                            )
                        ),
                        executable = "apktool",
                        arguments = listOf("d", "{input}", "-o", "{output}", "-f"),
                        enabled = true,
                        sortOrder = 0
                    ),
                    CustomTool(
                        id = "tool_ffprobe_inspect",
                        name = "FFprobe Stream Info",
                        icon = "info",
                        accentColorHex = "#06B6D4",
                        description = "Extract detailed audio and video codec metadata",
                        inputDefinitions = listOf(
                            ToolInputDefinition(
                                key = "input",
                                label = "Media File",
                                type = ToolInputType.FILE,
                                placeholder = "/path/to/media.mp4"
                            )
                        ),
                        executable = "ffprobe",
                        arguments = listOf("-v", "quiet", "-print_format", "json", "-show_format", "-show_streams", "{input}"),
                        enabled = true,
                        sortOrder = 1
                    ),
                    CustomTool(
                        id = "tool_ffmpeg_extract_mp3",
                        name = "Extract MP3 Audio",
                        icon = "music_note",
                        accentColorHex = "#F59E0B",
                        description = "Convert any video into high-quality MP3 audio",
                        inputDefinitions = listOf(
                            ToolInputDefinition(
                                key = "input",
                                label = "Video File",
                                type = ToolInputType.FILE,
                                placeholder = "/path/to/video.mp4"
                            ),
                            ToolInputDefinition(
                                key = "output",
                                label = "Output MP3 Name",
                                type = ToolInputType.TEXT,
                                defaultValue = "audio.mp3"
                            )
                        ),
                        executable = "ffmpeg",
                        arguments = listOf("-i", "{input}", "-vn", "-acodec", "libmp3lame", "-q:a", "2", "{output}"),
                        enabled = true,
                        sortOrder = 2
                    ),
                    CustomTool(
                        id = "tool_aider_code_task",
                        name = "Aider CLI Agent",
                        icon = "terminal",
                        accentColorHex = "#10B981",
                        description = "Run AI-driven code refactoring or terminal instruction",
                        inputDefinitions = listOf(
                            ToolInputDefinition(
                                key = "input",
                                label = "Prompt / Instruction",
                                type = ToolInputType.TEXT,
                                placeholder = "Fix deprecation warnings in current directory"
                            )
                        ),
                        executable = "aider",
                        arguments = listOf("--message", "{input}", "--no-git"),
                        enabled = true,
                        sortOrder = 3
                    )
                )
                dao.insertTools(defaultTools.map { CustomToolEntity.fromDomain(it) })
            }
        }
    }
}
