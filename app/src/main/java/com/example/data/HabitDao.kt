package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits ORDER BY id DESC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits")
    suspend fun getAllHabitsRaw(): List<Habit>

    @Query("SELECT * FROM habits WHERE id = :id")
    fun getHabitById(id: Int): Flow<Habit?>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitByIdSuspend(id: Int): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: Int)

    @Query("SELECT * FROM habit_logs")
    fun getAllLogs(): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs")
    suspend fun getAllLogsRaw(): List<HabitLog>

    @Query("SELECT * FROM habit_logs WHERE date = :date")
    fun getLogsForDate(date: String): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE date = :date")
    suspend fun getLogsForDateRaw(date: String): List<HabitLog>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId")
    fun getLogsForHabit(habitId: Int): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun getLogsForHabitOnDate(habitId: Int, date: String): List<HabitLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HabitLog): Long

    @Delete
    suspend fun deleteLog(log: HabitLog)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun deleteLogsForHabitOnDate(habitId: Int, date: String)

    @Query("DELETE FROM habit_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM habits")
    suspend fun clearAllHabits()
}
