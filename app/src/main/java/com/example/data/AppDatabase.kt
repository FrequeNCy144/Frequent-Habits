package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private fun safeAddColumn(db: SupportSQLiteDatabase, table: String, column: String, type: String) {
    try {
        var columnExists = false
        val cursor = db.query("PRAGMA table_info(`$table`)")
        cursor.use { c ->
            val nameIndex = c.getColumnIndex("name")
            if (nameIndex != -1) {
                while (c.moveToNext()) {
                    if (c.getString(nameIndex).equals(column, ignoreCase = true)) {
                        columnExists = true
                        break
                    }
                }
            }
        }
        if (!columnExists) {
            db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $type")
        }
    } catch (_: Exception) {
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        safeAddColumn(db, "habits", "isNegative", "INTEGER NOT NULL DEFAULT 0")
        safeAddColumn(db, "habits", "type", "TEXT NOT NULL DEFAULT 'BINARY'")
        safeAddColumn(db, "habits", "unit", "TEXT NOT NULL DEFAULT ''")
        safeAddColumn(db, "habits", "targetValue", "REAL NOT NULL DEFAULT 1.0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        safeAddColumn(db, "habits", "frequency", "TEXT NOT NULL DEFAULT 'DAILY'")
        safeAddColumn(db, "habits", "specificDays", "TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        safeAddColumn(db, "habits", "reminderEnabled", "INTEGER NOT NULL DEFAULT 0")
        safeAddColumn(db, "habits", "reminderHour", "INTEGER NOT NULL DEFAULT 18")
        safeAddColumn(db, "habits", "reminderMinute", "INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        safeAddColumn(db, "habit_logs", "isPaused", "INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        safeAddColumn(db, "habits", "isArchived", "INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        safeAddColumn(db, "habits", "customReminders", "TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        safeAddColumn(db, "habits", "description", "TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `time_capsule_notes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `type` TEXT NOT NULL, 
                `targetPeriod` TEXT NOT NULL, 
                `content` TEXT NOT NULL, 
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `milestone_rewards` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `habitId` INTEGER NOT NULL, 
                `rewardText` TEXT NOT NULL, 
                `isRedeemed` INTEGER NOT NULL, 
                `unlockedAt` INTEGER NOT NULL, 
                `conditionType` TEXT NOT NULL, 
                `conditionValue` INTEGER NOT NULL, 
                `trophyId` TEXT NOT NULL, 
                FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_milestone_rewards_habitId` ON `milestone_rewards` (`habitId`)")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        safeAddColumn(db, "habits", "clickIncrement", "REAL NOT NULL DEFAULT 1.0")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        safeAddColumn(db, "milestone_rewards", "description", "TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        safeAddColumn(db, "habits", "minimalViableValue", "REAL")
        safeAddColumn(db, "habits", "minimalViableText", "TEXT NOT NULL DEFAULT ''")
        safeAddColumn(db, "habit_logs", "isMinimalViable", "INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        safeAddColumn(db, "habits", "why", "TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        safeAddColumn(db, "habits", "stackedOnHabitId", "INTEGER")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        safeAddColumn(db, "habits", "isFinishable", "INTEGER NOT NULL DEFAULT 0")
        safeAddColumn(db, "habits", "totalTargetValue", "REAL")
        safeAddColumn(db, "habits", "isCompletedGoal", "INTEGER NOT NULL DEFAULT 0")
        safeAddColumn(db, "habits", "completedAt", "INTEGER")
        safeAddColumn(db, "habits", "completionNote", "TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        safeAddColumn(db, "habits", "difficulty", "TEXT NOT NULL DEFAULT 'MEDIUM'")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        safeAddColumn(db, "habits", "challengeMode", "TEXT NOT NULL DEFAULT 'ACCUMULATE'")
        safeAddColumn(db, "habits", "challengeSuccessRate", "INTEGER NOT NULL DEFAULT 90")
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        safeAddColumn(db, "habits", "folderId", "INTEGER")
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Schema hash alignment for Habit defaultValue annotations
        safeAddColumn(db, "habits", "folderId", "INTEGER")
    }
}

@Database(entities = [Habit::class, HabitLog::class, DailyNote::class, TimeCapsuleNote::class, MilestoneReward::class], version = 20, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "habits_database"
            )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_13,
                MIGRATION_13_14,
                MIGRATION_14_15,
                MIGRATION_15_16,
                MIGRATION_16_17,
                MIGRATION_17_18,
                MIGRATION_18_19,
                MIGRATION_19_20
            )
            .fallbackToDestructiveMigration()
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                if (INSTANCE != null) return INSTANCE!!
                val db = try {
                    buildDatabase(context)
                } catch (e: Throwable) {
                    android.util.Log.e("AppDatabase", "Failed to build Room DB, trying reset", e)
                    try {
                        context.deleteDatabase("habits_database")
                        buildDatabase(context)
                    } catch (e2: Throwable) {
                        android.util.Log.e("AppDatabase", "Fatal DB build failure, using in-memory database", e2)
                        Room.inMemoryDatabaseBuilder(context.applicationContext, AppDatabase::class.java)
                            .fallbackToDestructiveMigration()
                            .fallbackToDestructiveMigrationOnDowngrade()
                            .build()
                    }
                }
                INSTANCE = db
                db
            }
        }
    }
}
