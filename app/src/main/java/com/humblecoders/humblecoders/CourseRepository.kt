package com.humblecoders.humblecoders

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CourseRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getAllCourses(): Result<List<CourseCategory>> {
        return try {
            android.util.Log.d("CourseRepository", "Starting to fetch all courses")
            val categories = listOf("App Dev", "Python", "Web Dev")
            val courseCategories = mutableListOf<CourseCategory>()

            for (category in categories) {
                android.util.Log.d("CourseRepository", "Fetching courses for category: $category")
                val courses = getCoursesByCategory(category)
                if (courses.isSuccess) {
                    val courseList = courses.getOrThrow()
                    android.util.Log.d("CourseRepository", "Successfully fetched ${courseList.size} courses for $category")
                    courseList.forEach { course ->
                        android.util.Log.d("CourseRepository", "Course: ${course.title}, Thumbnail: ${course.thumbnailUrl}")
                    }
                    courseCategories.add(
                        CourseCategory(
                            categoryName = category,
                            courses = courseList
                        )
                    )
                } else {
                    android.util.Log.e("CourseRepository", "Failed to fetch courses for $category: ${courses.exceptionOrNull()?.message}")
                }
            }

            android.util.Log.d("CourseRepository", "Total categories fetched: ${courseCategories.size}")
            Result.success(courseCategories)
        } catch (e: Exception) {
            android.util.Log.e("CourseRepository", "Error fetching all courses", e)
            Result.failure(e)
        }
    }

    suspend fun getCoursesByCategory(categoryName: String): Result<List<Course>> {
        return try {
            android.util.Log.d("CourseRepository", "Fetching courses for category: $categoryName")
            val document = firestore.collection("Courses")
                .document(categoryName)
                .get()
                .await()

            android.util.Log.d("CourseRepository", "Document exists: ${document.exists()}")
            
            if (document.exists()) {
                @Suppress("UNCHECKED_CAST")
                val coursesData = document.get("courses") as? List<Map<String, Any>> ?: emptyList()
                android.util.Log.d("CourseRepository", "Raw courses data size: ${coursesData.size}")
                android.util.Log.d("CourseRepository", "Raw courses data: $coursesData")
                
                val courses = coursesData.map { courseMap ->
                    android.util.Log.d("CourseRepository", "Raw course map: $courseMap")
                    val course = Course.fromMap(courseMap)
                    android.util.Log.d("CourseRepository", "Parsed course: ${course.title}, Thumbnail: ${course.thumbnailUrl}")
                    course
                }
                android.util.Log.d("CourseRepository", "Successfully parsed ${courses.size} courses")
                Result.success(courses)
            } else {
                android.util.Log.w("CourseRepository", "Document does not exist for category: $categoryName")
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            android.util.Log.e("CourseRepository", "Error fetching courses for $categoryName", e)
            Result.failure(e)
        }
    }

    suspend fun getCourseById(categoryName: String, courseId: String): Result<Course?> {
        return try {
            val document = firestore.collection("Courses")
                .document(categoryName)
                .get()
                .await()

            if (document.exists()) {
                @Suppress("UNCHECKED_CAST")
                val coursesData = document.get("courses") as? List<Map<String, Any>> ?: emptyList()
                val course = coursesData.find { 
                    (it["id"] as? String) == courseId 
                }?.let { Course.fromMap(it) }
                Result.success(course)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCourseDetailsByTitle(courseTitle: String): Result<CourseDetails?> {
        return try {
            android.util.Log.d("CourseRepository", "Fetching course details for title: $courseTitle")
            
            // First get the courseDetails document
            val courseDetailsDoc = firestore.collection(courseTitle)
                .document("courseDetails")
                .get()
                .await()

            if (!courseDetailsDoc.exists()) {
                android.util.Log.w("CourseRepository", "Course details document not found for: $courseTitle")
                return Result.success(null)
            }

            val courseDetailsData = courseDetailsDoc.data ?: emptyMap()
            android.util.Log.d("CourseRepository", "Course details data: $courseDetailsData")

            // Get all section documents
            val sectionsSnapshot = firestore.collection(courseTitle)
                .get()
                .await()

            val sections = mutableListOf<CourseSection>()
            
            sectionsSnapshot.documents.forEach { doc ->
                // Skip the courseDetails document
                if (doc.id != "courseDetails") {
                    val sectionData = doc.data ?: emptyMap()
                    android.util.Log.d("CourseRepository", "Section data for ${doc.id}: $sectionData")
                    
                    val section = CourseSection.fromMap(sectionData)
                    sections.add(section)
                }
            }

            // Sort sections by order
            val sortedSections = sections.sortedBy { it.order }
            android.util.Log.d("CourseRepository", "Found ${sortedSections.size} sections")

            // Create a map with sections data for CourseDetails.fromMap
            val courseDetailsMap = courseDetailsData.toMutableMap()
            courseDetailsMap["sections"] = sortedSections.map { it.toMap() }
            
            android.util.Log.d("CourseRepository", "Course details map with sections: $courseDetailsMap")
            
            val courseDetails = CourseDetails.fromMap(courseDetailsMap)

            android.util.Log.d("CourseRepository", "Successfully created course details: ${courseDetails.title}")
            Result.success(courseDetails)

        } catch (e: Exception) {
            android.util.Log.e("CourseRepository", "Error fetching course details for $courseTitle", e)
            Result.failure(e)
        }
    }

    suspend fun updateCourseRating(courseTitle: String, newRating: Double): Result<Boolean> {
        return try {
            android.util.Log.d("CourseRepository", "Updating rating for course: $courseTitle with rating: $newRating")
            
            // Get current rating from the courseDetails document
            val courseDetailsDoc = firestore.collection(courseTitle)
                .document("courseDetails")
                .get()
                .await()
            
            if (courseDetailsDoc.exists()) {
                val currentRating = (courseDetailsDoc.get("rating") as? Number)?.toDouble() ?: 0.0
                
                // Calculate new average rating (average of current and new rating)
                val updatedRating = (currentRating + newRating) / 2.0
                
                android.util.Log.d("CourseRepository", "Current rating: $currentRating, New rating: $newRating, Updated rating: $updatedRating")
                
                // Update the rating in Firestore
                firestore.collection(courseTitle)
                    .document("courseDetails")
                    .update("rating", updatedRating)
                    .await()
                
                android.util.Log.d("CourseRepository", "Successfully updated course rating to: $updatedRating")
                Result.success(true)
            } else {
                android.util.Log.e("CourseRepository", "Course details document not found for: $courseTitle")
                Result.failure(Exception("Course details document not found"))
            }
            
        } catch (e: Exception) {
            android.util.Log.e("CourseRepository", "Error updating course rating", e)
            Result.failure(e)
        }
    }
}
