package com.humblecoders.humblecoders

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class ChatGPTService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val apiKey = ""

    suspend fun generateStudySchedule(request: ScheduleRequest): Result<StudySchedule> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("ChatGPTService", "Generating schedule for course: ${request.courseTitle}")
            val prompt = buildPrompt(request)
            android.util.Log.d("ChatGPTService", "Prompt created, calling API")
            val response = callChatGPTAPI(prompt)
            
            android.util.Log.d("ChatGPTService", "API response code: ${response.code}")
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                android.util.Log.d("ChatGPTService", "Response body length: ${responseBody?.length}")
                if (responseBody != null) {
                    val schedule = parseScheduleResponse(responseBody)
                    android.util.Log.d("ChatGPTService", "Schedule parsed successfully")
                    Result.success(schedule)
                } else {
                    android.util.Log.e("ChatGPTService", "Empty response body")
                    Result.failure(Exception("Empty response from ChatGPT"))
                }
            } else {
                android.util.Log.e("ChatGPTService", "API call failed: ${response.code} - ${response.message}")
                Result.failure(Exception("API call failed: ${response.code} - ${response.message}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatGPTService", "Exception in generateStudySchedule", e)
            Result.failure(e)
        }
    }

    private fun buildPrompt(request: ScheduleRequest): String {
        val sectionsInfo = request.sections.joinToString("\n") { section ->
            "- ${section.title}: ${section.description} (${section.videoCount} videos, ${section.estimatedDuration})"
        }

        return """
        You are an expert educational planner. Create a detailed study schedule for the following course:

        COURSE DETAILS:
        - Title: ${request.courseTitle}
        - Description: ${request.courseDescription}
        - Difficulty Level: ${request.difficultyLevel}
        - Total Sections: ${request.totalSections}
        - Total Videos: ${request.totalVideos}
        - User's Desired Timeframe: ${request.userTimeframe}

        COURSE SECTIONS:
        $sectionsInfo

        REQUIREMENTS:
        1. Create a realistic study schedule that fits within the user's timeframe
        2. Break down the content into weekly and daily plans
        3. Include different types of activities: video watching, reading, practice, assignments, and reviews
        4. Consider the difficulty level when planning the pace
        5. Include time for breaks and review sessions
        6. Make the schedule practical and achievable

        RESPONSE FORMAT:
        Return ONLY a valid JSON object with the following structure:
        {
            "courseTitle": "Course Title",
            "totalDuration": "X weeks/months",
            "estimatedHoursPerWeek": X,
            "difficultyLevel": "beginner/intermediate/advanced",
            "schedule": [
                {
                    "weekNumber": 1,
                    "title": "Week 1 Title",
                    "description": "What will be covered this week",
                    "estimatedHours": X,
                    "days": [
                        {
                            "dayNumber": 1,
                            "dayName": "Monday",
                            "estimatedHours": X,
                            "activities": [
                                {
                                    "time": "9:00 AM",
                                    "title": "Activity Title",
                                    "description": "Detailed description",
                                    "duration": "1 hour",
                                    "type": "video/reading/practice/assignment/review",
                                    "priority": "high/medium/low"
                                }
                            ]
                        }
                    ]
                }
            ]
        }

        IMPORTANT: Return ONLY the JSON object, no additional text or formatting.
        """.trimIndent()
    }

    private suspend fun callChatGPTAPI(prompt: String): Response = withContext(Dispatchers.IO) {
        val requestBody = JsonObject().apply {
            addProperty("model", "gpt-3.5-turbo")
            add("messages", com.google.gson.JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", prompt)
                })
            })
            addProperty("max_tokens", 4000)
            addProperty("temperature", 0.7)
        }

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute()
    }

    private fun parseScheduleResponse(responseBody: String): StudySchedule {
        try {
            val jsonObject = JsonParser.parseString(responseBody).asJsonObject
            val choices = jsonObject.getAsJsonArray("choices")
            val firstChoice = choices[0].asJsonObject
            val message = firstChoice.getAsJsonObject("message")
            val content = message.get("content").asString

            // Parse the JSON content from ChatGPT
            val scheduleJson = JsonParser.parseString(content).asJsonObject
            
            return StudySchedule(
                courseTitle = scheduleJson.get("courseTitle")?.asString ?: "",
                totalDuration = scheduleJson.get("totalDuration")?.asString ?: "",
                estimatedHoursPerWeek = scheduleJson.get("estimatedHoursPerWeek")?.asInt ?: 0,
                difficultyLevel = scheduleJson.get("difficultyLevel")?.asString ?: "",
                schedule = parseWeekSchedules(scheduleJson.getAsJsonArray("schedule"))
            )
        } catch (e: Exception) {
            throw Exception("Failed to parse ChatGPT response: ${e.message}")
        }
    }

    private fun parseWeekSchedules(weeksArray: com.google.gson.JsonArray): List<WeekSchedule> {
        return weeksArray.map { weekElement ->
            val week = weekElement.asJsonObject
            WeekSchedule(
                weekNumber = week.get("weekNumber")?.asInt ?: 0,
                title = week.get("title")?.asString ?: "",
                description = week.get("description")?.asString ?: "",
                estimatedHours = week.get("estimatedHours")?.asInt ?: 0,
                days = parseDaySchedules(week.getAsJsonArray("days"))
            )
        }
    }

    private fun parseDaySchedules(daysArray: com.google.gson.JsonArray): List<DaySchedule> {
        return daysArray.map { dayElement ->
            val day = dayElement.asJsonObject
            DaySchedule(
                dayNumber = day.get("dayNumber")?.asInt ?: 0,
                dayName = day.get("dayName")?.asString ?: "",
                estimatedHours = day.get("estimatedHours")?.asInt ?: 0,
                activities = parseActivities(day.getAsJsonArray("activities"))
            )
        }
    }

    private fun parseActivities(activitiesArray: com.google.gson.JsonArray): List<StudyActivity> {
        return activitiesArray.map { activityElement ->
            val activity = activityElement.asJsonObject
            StudyActivity(
                time = activity.get("time")?.asString ?: "",
                title = activity.get("title")?.asString ?: "",
                description = activity.get("description")?.asString ?: "",
                duration = activity.get("duration")?.asString ?: "",
                type = activity.get("type")?.asString ?: "",
                priority = activity.get("priority")?.asString ?: ""
            )
        }
    }
}
