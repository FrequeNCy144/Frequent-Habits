package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.example.tr

@Composable
fun HabitBottomNavigation(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onAddClick: () -> Unit,
    language: String
) {
    val items = listOf(
        Triple("TODAY", tr(language, "Heute", "Today"), Icons.Default.CalendarToday),
        Triple("STATS", tr(language, "Statistik", "Stats"), Icons.Default.BarChart),
        Triple("PROFILE", tr(language, "Profil", "Profile"), Icons.Default.Person)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 8.dp, top = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = AppCard,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, AppBorder),
            shadowElevation = 8.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { (tabKey, label, icon) ->
                    val isSelected = selectedTab == tabKey
                    val navBgColor by animateColorAsState(
                        targetValue = if (isSelected) PrimaryViolet else Color.Transparent,
                        animationSpec = tween(220),
                        label = "navBg"
                    )
                    val navContentColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else TextSecondary,
                        animationSpec = tween(220),
                        label = "navContent"
                    )

                    Row(
                        modifier = Modifier
                            .testTag("nav_${tabKey.lowercase()}")
                            .clip(RoundedCornerShape(14.dp))
                            .background(navBgColor)
                            .clickable { onTabSelected(tabKey) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = navContentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = label,
                            color = navContentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
