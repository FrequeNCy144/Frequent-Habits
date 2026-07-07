package com.example.data

import kotlinx.coroutines.flow.Flow

class HabitRepository(private val habitDao: HabitDao) {

    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()
    val allLogs: Flow<List<HabitLog>> = habitDao.getAllLogs()

    fun getHabitById(id: Int): Flow<Habit?> = habitDao.getHabitById(id)

    suspend fun getHabitByIdSuspend(id: Int): Habit? = habitDao.getHabitByIdSuspend(id)

    suspend fun insertHabit(habit: Habit): Long = habitDao.insertHabit(habit)

    suspend fun updateHabit(habit: Habit) = habitDao.updateHabit(habit)

    suspend fun deleteHabit(habit: Habit) = habitDao.deleteHabit(habit)

    suspend fun deleteHabitById(id: Int) = habitDao.deleteHabitById(id)

    fun getLogsForDate(date: String): Flow<List<HabitLog>> = habitDao.getLogsForDate(date)

    fun getLogsForHabit(habitId: Int): Flow<List<HabitLog>> = habitDao.getLogsForHabit(habitId)

    suspend fun getLogsForHabitOnDate(habitId: Int, date: String): List<HabitLog> =
        habitDao.getLogsForHabitOnDate(habitId, date)

    suspend fun logHabit(habitId: Int, date: String, value: Float) {
        val existing = habitDao.getLogsForHabitOnDate(habitId, date)
        if (existing.isNotEmpty()) {
            val first = existing.first()
            val updated = first.copy(value = value, timestamp = System.currentTimeMillis())
            habitDao.insertLog(updated)
            if (existing.size > 1) {
                for (i in 1 until existing.size) {
                    habitDao.deleteLog(existing[i])
                }
            }
        } else {
            val log = HabitLog(habitId = habitId, date = date, value = value)
            habitDao.insertLog(log)
        }
    }

    suspend fun unlogHabit(habitId: Int, date: String) {
        habitDao.deleteLogsForHabitOnDate(habitId, date)
    }

    suspend fun clearAllData() {
        habitDao.clearAllLogs()
        habitDao.clearAllHabits()
    }
}
