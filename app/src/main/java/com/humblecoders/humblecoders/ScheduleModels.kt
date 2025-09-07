package com.humblecoders.humblecoders

import com.google.firebase.Timestamp

data class StudySchedule(
    val id: String = "",
    val courseTitle: String = "",
    val totalDuration: String = "", // e.g., "4 weeks", "2 months"
    val estimatedHoursPerWeek: Int = 0,
    val difficultyLevel: String = "",
    val schedule: List<WeekSchedule> = emptyList(),
    val createdAt: Timestamp = Timestamp.now(),
    val userId: String = ""
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "courseTitle" to courseTitle,
            "totalDuration" to totalDuration,
            "estimatedHoursPerWeek" to estimatedHoursPerWeek,
            "difficultyLevel" to difficultyLevel,
            "schedule" to schedule.map { it.toMap() },
            "createdAt" to createdAt,
            "userId" to userId
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): StudySchedule {
            return StudySchedule(
                id = map["id"] as? String ?: "",
                courseTitle = map["courseTitle"] as? String ?: "",
                totalDuration = map["totalDuration"] as? String ?: "",
                estimatedHoursPerWeek = (map["estimatedHoursPerWeek"] as? Number)?.toInt() ?: 0,
                difficultyLevel = map["difficultyLevel"] as? String ?: "",
                schedule = (map["schedule"] as? List<Map<String, Any>>)?.map { WeekSchedule.fromMap(it) } ?: emptyList(),
                createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
                userId = map["userId"] as? String ?: ""
            )
        }
    }
}

data class WeekSchedule(
    val weekNumber: Int = 0,
    val title: String = "",
    val description: String = "",
    val estimatedHours: Int = 0,
    val days: List<DaySchedule> = emptyList()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "weekNumber" to weekNumber,
            "title" to title,
            "description" to description,
            "estimatedHours" to estimatedHours,
            "days" to days.map { it.toMap() }
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): WeekSchedule {
            return WeekSchedule(
                weekNumber = (map["weekNumber"] as? Number)?.toInt() ?: 0,
                title = map["title"] as? String ?: "",
                description = map["description"] as? String ?: "",
                estimatedHours = (map["estimatedHours"] as? Number)?.toInt() ?: 0,
                days = (map["days"] as? List<Map<String, Any>>)?.map { DaySchedule.fromMap(it) } ?: emptyList()
            )
        }
    }
}

data class DaySchedule(
    val dayNumber: Int = 0,
    val dayName: String = "",
    val activities: List<StudyActivity> = emptyList(),
    val estimatedHours: Int = 0
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "dayNumber" to dayNumber,
            "dayName" to dayName,
            "activities" to activities.map { it.toMap() },
            "estimatedHours" to estimatedHours
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): DaySchedule {
            return DaySchedule(
                dayNumber = (map["dayNumber"] as? Number)?.toInt() ?: 0,
                dayName = map["dayName"] as? String ?: "",
                activities = (map["activities"] as? List<Map<String, Any>>)?.map { StudyActivity.fromMap(it) } ?: emptyList(),
                estimatedHours = (map["estimatedHours"] as? Number)?.toInt() ?: 0
            )
        }
    }
}

data class StudyActivity(
    val time: String = "",
    val title: String = "",
    val description: String = "",
    val duration: String = "",
    val type: String = "", // "video", "reading", "practice", "assignment", "review"
    val priority: String = "" // "high", "medium", "low"
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "time" to time,
            "title" to title,
            "description" to description,
            "duration" to duration,
            "type" to type,
            "priority" to priority
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): StudyActivity {
            return StudyActivity(
                time = map["time"] as? String ?: "",
                title = map["title"] as? String ?: "",
                description = map["description"] as? String ?: "",
                duration = map["duration"] as? String ?: "",
                type = map["type"] as? String ?: "",
                priority = map["priority"] as? String ?: ""
            )
        }
    }
}

data class ScheduleRequest(
    val courseTitle: String,
    val courseDescription: String,
    val difficultyLevel: String,
    val totalSections: Int,
    val totalVideos: Int,
    val sections: List<SectionInfo>,
    val userTimeframe: String // e.g., "2 weeks", "1 month", "3 months"
)

data class SectionInfo(
    val title: String,
    val description: String,
    val videoCount: Int,
    val estimatedDuration: String
)
