package com.example.data

import kotlinx.coroutines.flow.Flow

class HabitRepository(private val habitDao: HabitDao) {

    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()
    val allLogs: Flow<List<HabitLog>> = habitDao.getAllLogs()
    val allDailyNotes: Flow<List<DailyNote>> = habitDao.getAllDailyNotes()

    fun getDailyNote(date: String): Flow<DailyNote?> = habitDao.getDailyNote(date)

    suspend fun getDailyNoteRaw(date: String): DailyNote? = habitDao.getDailyNoteRaw(date)

    suspend fun saveDailyNote(date: String, content: String) {
        if (content.trim().isEmpty()) {
            val existing = habitDao.getDailyNoteRaw(date)
            if (existing != null) {
                habitDao.deleteDailyNote(existing)
            }
        } else {
            habitDao.insertDailyNote(DailyNote(date, content))
        }
    }

    fun getHabitById(id: Int): Flow<Habit?> = habitDao.getHabitById(id)

    suspend fun getHabitByIdSuspend(id: Int): Habit? = habitDao.getHabitByIdSuspend(id)

    suspend fun insertHabit(habit: Habit): Long = habitDao.insertHabit(habit)

    suspend fun updateHabit(habit: Habit) = habitDao.updateHabit(habit)
    
    suspend fun updateHabitSortOrders(orders: List<Pair<Int, Int>>) = habitDao.updateHabitSortOrders(orders)

    suspend fun deleteHabit(habit: Habit) = habitDao.deleteHabit(habit)

    suspend fun deleteHabitById(id: Int) = habitDao.deleteHabitById(id)

    fun getLogsForDate(date: String): Flow<List<HabitLog>> = habitDao.getLogsForDate(date)

    fun getLogsForHabit(habitId: Int): Flow<List<HabitLog>> = habitDao.getLogsForHabit(habitId)

    suspend fun getLogsForHabitRaw(habitId: Int): List<HabitLog> = habitDao.getLogsForHabitRaw(habitId)

    suspend fun getLogsForHabitOnDate(habitId: Int, date: String): List<HabitLog> =
        habitDao.getLogsForHabitOnDate(habitId, date)

    suspend fun logHabit(habitId: Int, date: String, value: Float) {
        habitDao.logHabitTransaction(habitId, date, value)
    }

    suspend fun togglePauseHabit(habitId: Int, date: String) {
        habitDao.togglePauseHabitTransaction(habitId, date)
    }

    suspend fun setPauseStateForHabit(habitId: Int, date: String, shouldPause: Boolean) {
        habitDao.setPauseStateForHabit(habitId, date, shouldPause)
    }

    suspend fun toggleMinimalViableHabit(habitId: Int, date: String) {
        habitDao.toggleMinimalViableHabitTransaction(habitId, date)
    }

    suspend fun setMinimalViableStateForHabit(habitId: Int, date: String, shouldMinimalViable: Boolean) {
        habitDao.setMinimalViableStateForHabit(habitId, date, shouldMinimalViable)
    }

    suspend fun unlogHabit(habitId: Int, date: String) {
        habitDao.unlogHabitTransaction(habitId, date)
    }

    fun getTimeCapsuleNote(type: String, targetPeriod: String): Flow<TimeCapsuleNote?> =
        habitDao.getTimeCapsuleNote(type, targetPeriod)

    suspend fun getTimeCapsuleNoteRaw(type: String, targetPeriod: String): TimeCapsuleNote? =
        habitDao.getTimeCapsuleNoteRaw(type, targetPeriod)

    suspend fun saveTimeCapsuleNote(type: String, targetPeriod: String, content: String) {
        if (content.trim().isEmpty()) {
            val existing = habitDao.getTimeCapsuleNoteRaw(type, targetPeriod)
            if (existing != null) {
                habitDao.deleteTimeCapsuleNote(existing)
            }
        } else {
            val existing = habitDao.getTimeCapsuleNoteRaw(type, targetPeriod)
            if (existing != null) {
                habitDao.insertTimeCapsuleNote(existing.copy(content = content, createdAt = System.currentTimeMillis()))
            } else {
                habitDao.insertTimeCapsuleNote(TimeCapsuleNote(type = type, targetPeriod = targetPeriod, content = content))
            }
        }
    }

    suspend fun clearAllData() {
        habitDao.clearAllLogs()
        habitDao.clearAllHabits()
        habitDao.clearAllTimeCapsuleNotes()
        habitDao.clearAllDailyNotes()
        habitDao.clearAllMilestoneRewards()
    }

    // Milestone Rewards
    val allMilestoneRewards: Flow<List<MilestoneReward>> = habitDao.getAllMilestoneRewards()
    suspend fun getAllMilestoneRewardsRaw(): List<MilestoneReward> = habitDao.getAllMilestoneRewardsRaw()
    fun getMilestoneRewardsForHabit(habitId: Int): Flow<List<MilestoneReward>> = habitDao.getMilestoneRewardsForHabit(habitId)
    suspend fun insertMilestoneReward(reward: MilestoneReward) = habitDao.insertMilestoneReward(reward)
    suspend fun updateMilestoneReward(reward: MilestoneReward) = habitDao.updateMilestoneReward(reward)
    suspend fun deleteMilestoneReward(reward: MilestoneReward) = habitDao.deleteMilestoneReward(reward)
    suspend fun deleteMilestoneRewardsForHabit(habitId: Int) = habitDao.deleteMilestoneRewardsForHabit(habitId)
}
