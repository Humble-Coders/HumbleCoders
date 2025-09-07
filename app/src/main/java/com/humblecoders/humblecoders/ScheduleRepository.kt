package com.humblecoders.humblecoders

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ScheduleRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val chatGPTService = ChatGPTService()

    suspend fun generateStudySchedule(
        courseDetails: CourseDetails,
        userTimeframe: String,
        userId: String
    ): Result<StudySchedule> {
        return try {
            // Prepare schedule request
            val request = ScheduleRequest(
                courseTitle = courseDetails.title,
                courseDescription = courseDetails.description,
                difficultyLevel = courseDetails.difficultyLevel,
                totalSections = courseDetails.sections.size,
                totalVideos = courseDetails.sections.sumOf { it.videos.size },
                sections = courseDetails.sections.map { section ->
                    SectionInfo(
                        title = section.title,
                        description = section.description,
                        videoCount = section.videos.size,
                        estimatedDuration = "30 minutes" // Default estimate
                    )
                },
                userTimeframe = userTimeframe
            )

            // Generate schedule using ChatGPT
            val result = chatGPTService.generateStudySchedule(request)
            
            if (result.isSuccess) {
                val schedule = result.getOrThrow().copy(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    createdAt = Timestamp.now()
                )
                
                // Save to Firestore
                saveSchedule(schedule)
                Result.success(schedule)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to generate schedule"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveSchedule(schedule: StudySchedule): Result<Boolean> {
        return try {
            firestore.collection("study_schedules")
                .document(schedule.id)
                .set(schedule.toMap())
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserSchedules(userId: String): Result<List<StudySchedule>> {
        return try {
            val documents = firestore.collection("study_schedules")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val schedules = documents.mapNotNull { doc ->
                StudySchedule.fromMap(doc.data ?: emptyMap())
            }
            Result.success(schedules)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getScheduleById(scheduleId: String): Result<StudySchedule?> {
        return try {
            val document = firestore.collection("study_schedules")
                .document(scheduleId)
                .get()
                .await()

            if (document.exists()) {
                val schedule = StudySchedule.fromMap(document.data ?: emptyMap())
                Result.success(schedule)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSchedule(scheduleId: String): Result<Boolean> {
        return try {
            firestore.collection("study_schedules")
                .document(scheduleId)
                .delete()
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
