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
    version = 2,
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
                    .fallbackToDestructiveMigration()
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

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        val count = database.customToolDao().getToolCount()
                        if (count < 10) {
                            populateDefaultTools(database.customToolDao())
                        }
                    }
                }
            }

            private suspend fun populateDefaultTools(dao: CustomToolDao) {
                val tools = PreseededToolsCatalog.getAllPreseededTools()
                dao.insertTools(tools)
            }
        }
    }
}
