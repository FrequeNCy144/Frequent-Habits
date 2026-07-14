package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private fun safeAddColumn(db: SupportSQLiteDatabase, table: String, column: String, type: String) {
    try {
        db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $type")
    } catch (e: Exception) {
        // Ignored if column already exists
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

@Database(entities = [Habit::class, HabitLog::class, DailyNote::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
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
                    MIGRATION_6_7
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
