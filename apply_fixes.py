import os

# 1. HabitsViewModel.kt
with open('app/src/main/java/com/example/ui/HabitsViewModel.kt', 'r') as f:
    vm_content = f.read()

# Add fields if not present
if 'val newlyUnlockedAchievement:' not in vm_content:
    insert_marker = 'class HabitsViewModel(application: Application) : AndroidViewModel(application) {'
    replacement = """class HabitsViewModel(application: Application) : AndroidViewModel(application) {
    private val achievementQueue = MutableStateFlow<List<AchievementUnlockedData>>(emptyList())
    private val _newlyUnlockedAchievement = MutableStateFlow<AchievementUnlockedData?>(null)
    val newlyUnlockedAchievement: StateFlow<AchievementUnlockedData?> = _newlyUnlockedAchievement.asStateFlow()
    private var knownUnlockedAchievementIds: MutableSet<String>? = null
"""
    vm_content = vm_content.replace(insert_marker, replacement)

# Add init block if not present
if 'initAchievementObserver()' not in vm_content:
    init_marker = 'private val _smartInsightDismissedDate ='
    init_code = """    init {
        com.example.ui.theme.updateAccentColors(_accentColorName.value)
        initAchievementObserver()
        viewModelScope.launch {
            perfectDaysStats.collect { stats ->
                sharedPrefs.edit().putInt("current_perfect_streak", stats.currentStreak).apply()
            }
        }
        viewModelScope.launch {
            selectedDate.collect {
                _heatmapMonthOffset.value = 0
            }
        }
        viewModelScope.launch {
            try {
                com.example.NotificationHelper.scheduleSmartInsightNotifications(getApplication())
                com.example.NotificationHelper.scheduleReviewNotifications(getApplication())
                
                val list = database.habitDao().getAllHabitsRaw()
                list.forEach { habit ->
                    com.example.NotificationHelper.scheduleAllHabitReminders(
                        getApplication(),
                        habit
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val _smartInsightDismissedDate ="""
    vm_content = vm_content.replace(init_marker, init_code)

# Fix compareTo or getTodayDateString in HabitsViewModel
vm_content = vm_content.replace("getTodayDateString()", "com.example.ui.components.getTodayDateString()")

with open('app/src/main/java/com/example/ui/HabitsViewModel.kt', 'w') as f:
    f.write(vm_content)

# 2. StatsCalculationUtils.kt
with open('app/src/main/java/com/example/ui/components/StatsCalculationUtils.kt', 'r') as f:
    sc_content = f.read()
sc_content = "package com.example.ui.components\n\nimport com.example.data.*\nimport com.example.data.HabitCalculationEngine.calculateHabitStrengthOnDate\nimport com.example.data.HabitCalculationEngine.isLogCompleted\n" + sc_content.split("\n", 1)[1]
with open('app/src/main/java/com/example/ui/components/StatsCalculationUtils.kt', 'w') as f:
    f.write(sc_content)

# 3. SmartInsightCard.kt
with open('app/src/main/java/com/example/ui/components/SmartInsightCard.kt', 'r') as f:
    si_content = f.read()
si_content = "package com.example.ui.components\n\nimport com.example.*\n" + si_content.split("\n", 1)[1]
with open('app/src/main/java/com/example/ui/components/SmartInsightCard.kt', 'w') as f:
    f.write(si_content)

# 4. StatsTrendLineCharts.kt
with open('app/src/main/java/com/example/ui/components/StatsTrendLineCharts.kt', 'r') as f:
    st_content = f.read()
st_imports = """package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
"""
st_content = st_imports + '\n' + '\n'.join(st_content.splitlines()[15:])
with open('app/src/main/java/com/example/ui/components/StatsTrendLineCharts.kt', 'w') as f:
    f.write(st_content)

# 5. StatsVolumeBarCharts.kt
with open('app/src/main/java/com/example/ui/components/StatsVolumeBarCharts.kt', 'r') as f:
    sv_content = f.read()
sv_imports = """package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
"""
sv_content = sv_imports + '\n' + '\n'.join(sv_content.splitlines()[15:])
with open('app/src/main/java/com/example/ui/components/StatsVolumeBarCharts.kt', 'w') as f:
    f.write(sv_content)

# 6. UnifiedUI.kt
with open('app/src/main/java/com/example/ui/components/UnifiedUI.kt', 'r') as f:
    u_content = f.read()
u_imports = """package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
"""
u_content = u_imports + '\n' + '\n'.join(u_content.splitlines()[15:])
with open('app/src/main/java/com/example/ui/components/UnifiedUI.kt', 'w') as f:
    f.write(u_content)

# 7. StatsSelectorComponents.kt
with open('app/src/main/java/com/example/ui/components/StatsSelectorComponents.kt', 'r') as f:
    sel_content = f.read()
sel_imports = """package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
"""
sel_content = sel_imports + '\n' + '\n'.join(sel_content.splitlines()[5:])
with open('app/src/main/java/com/example/ui/components/StatsSelectorComponents.kt', 'w') as f:
    f.write(sel_content)

print("Applied fixes to components!")
