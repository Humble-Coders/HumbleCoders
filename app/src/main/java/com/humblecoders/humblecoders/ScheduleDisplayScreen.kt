package com.humblecoders.humblecoders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDisplayScreen(
    schedule: StudySchedule,
    onBackPressed: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Study Schedule",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = schedule.courseTitle,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2C3E50)
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Schedule Overview
            item {
                ScheduleOverviewCard(schedule = schedule)
            }
            
            // Weekly Schedule
            items(schedule.schedule) { weekSchedule ->
                WeekScheduleCard(weekSchedule = weekSchedule)
            }
        }
    }
}

@Composable
fun ScheduleOverviewCard(schedule: StudySchedule) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Schedule Overview",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ScheduleInfoItem(
                    icon = Icons.Default.Schedule,
                    title = "Duration",
                    value = schedule.totalDuration,
                    color = Color(0xFF4A90E2)
                )
                
                ScheduleInfoItem(
                    icon = Icons.Default.Timer,
                    title = "Hours/Week",
                    value = "${schedule.estimatedHoursPerWeek} hrs",
                    color = Color(0xFF27AE60)
                )
                
                ScheduleInfoItem(
                    icon = Icons.Default.School,
                    title = "Level",
                    value = schedule.difficultyLevel,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
fun ScheduleInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
        
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
    }
}

@Composable
fun WeekScheduleCard(weekSchedule: WeekSchedule) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Week ${weekSchedule.weekNumber}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                
                Text(
                    text = "${weekSchedule.estimatedHours} hrs",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = weekSchedule.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2C3E50)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = weekSchedule.description,
                fontSize = 14.sp,
                color = Color(0xFF666666),
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Days
            weekSchedule.days.forEach { daySchedule ->
                DayScheduleItem(daySchedule = daySchedule)
            }
        }
    }
}

@Composable
fun DayScheduleItem(daySchedule: DaySchedule) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = daySchedule.dayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                
                Text(
                    text = "${daySchedule.estimatedHours} hrs",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            daySchedule.activities.forEach { activity ->
                ActivityItem(activity = activity)
            }
        }
    }
}

@Composable
fun ActivityItem(activity: StudyActivity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time
        Text(
            text = activity.time,
            fontSize = 12.sp,
            color = Color(0xFF666666),
            modifier = Modifier.width(60.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Activity type icon
        val icon = when (activity.type) {
            "video" -> Icons.Default.PlayArrow
            "reading" -> Icons.Default.Book
            "practice" -> Icons.Default.Code
            "assignment" -> Icons.Default.Assignment
            "review" -> Icons.Default.Refresh
            else -> Icons.Default.Circle
        }
        
        val color = when (activity.priority) {
            "high" -> Color(0xFFE74C3C)
            "medium" -> Color(0xFFF39C12)
            "low" -> Color(0xFF27AE60)
            else -> Color(0xFF666666)
        }
        
        Icon(
            imageVector = icon,
            contentDescription = activity.type,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Activity details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = activity.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2C3E50)
            )
            
            if (activity.description.isNotEmpty()) {
                Text(
                    text = activity.description,
                    fontSize = 11.sp,
                    color = Color(0xFF666666),
                    lineHeight = 14.sp
                )
            }
        }
        
        // Duration
        Text(
            text = activity.duration,
            fontSize = 11.sp,
            color = Color(0xFF666666)
        )
    }
}
