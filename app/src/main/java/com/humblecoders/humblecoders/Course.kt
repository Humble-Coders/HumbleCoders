package com.humblecoders.humblecoders

import com.google.firebase.Timestamp

data class Course(
    val id: String = "",
    val title: String = "",
    val price: String = "",
    val thumbnailUrl: String = "",
    val description: String = "",
    val instructor: String = "",
    val duration: String = "",
    val rating: Double = 0.0,
    val studentsCount: Int = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "title" to title,
            "price" to price,
            "thumbnailUrl" to thumbnailUrl,
            "description" to description,
            "instructor" to instructor,
            "duration" to duration,
            "rating" to rating,
            "studentsCount" to studentsCount,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): Course {
            val thumbnailUrl = map["thumbnail"] as? String ?: map["thumbnailUrl"] as? String ?: ""
            android.util.Log.d("Course", "Parsing course from map: $map")
            android.util.Log.d("Course", "Found thumbnail field: ${map["thumbnail"]}")
            android.util.Log.d("Course", "Found thumbnailUrl field: ${map["thumbnailUrl"]}")
            android.util.Log.d("Course", "Final thumbnailUrl: $thumbnailUrl")
            
            return Course(
                id = map["id"] as? String ?: "",
                title = map["title"] as? String ?: "",
                price = map["price"] as? String ?: "",
                thumbnailUrl = thumbnailUrl,
                description = map["description"] as? String ?: "",
                instructor = map["instructor"] as? String ?: "",
                duration = map["duration"] as? String ?: "",
                rating = (map["rating"] as? Number)?.toDouble() ?: 0.0,
                studentsCount = (map["studentsCount"] as? Number)?.toInt() ?: 0,
                createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
                updatedAt = map["updatedAt"] as? Timestamp ?: Timestamp.now()
            )
        }
    }
}

data class CourseCategory(
    val categoryName: String,
    val courses: List<Course>
)

data class CourseVideo(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val duration: String = "", // e.g., "15:30"
    val thumbnailUrl: String = "",
    val videoUrl: String = "",
    val isFree: Boolean = false,
    val order: Int = 0
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "title" to title,
            "description" to description,
            "duration" to duration,
            "thumbnailUrl" to thumbnailUrl,
            "videoUrl" to videoUrl,
            "isFree" to isFree,
            "order" to order
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): CourseVideo {
            return CourseVideo(
                id = map["id"] as? String ?: "",
                title = map["title"] as? String ?: "",
                description = map["description"] as? String ?: "",
                duration = map["duration"] as? String ?: "",
                thumbnailUrl = map["thumbnailUrl"] as? String ?: "",
                videoUrl = map["videoUrl"] as? String ?: "",
                isFree = map["isFree"] as? Boolean ?: false,
                order = (map["order"] as? Number)?.toInt() ?: 0
            )
        }
    }
}

// New data classes for detailed course structure
data class CourseDetails(
    val title: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val difficultyLevel: String = "",
    val price: Int = 0,
    val rating: Double = 0.0,
    val sections: List<CourseSection> = emptyList()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "title" to title,
            "description" to description,
            "thumbnailUrl" to thumbnailUrl,
            "difficultyLevel" to difficultyLevel,
            "price" to price,
            "rating" to rating,
            "sections" to sections.map { it.toMap() }
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): CourseDetails {
            android.util.Log.d("CourseDetails", "Parsing course details from map: $map")
            
            val sectionsData = map["sections"] as? List<Map<String, Any>> ?: emptyList()
            android.util.Log.d("CourseDetails", "Sections data: $sectionsData")
            
            val sections = sectionsData.map { CourseSection.fromMap(it) }
            android.util.Log.d("CourseDetails", "Parsed ${sections.size} sections")
            
            val courseDetails = CourseDetails(
                title = map["title"] as? String ?: "",
                description = map["description"] as? String ?: "",
                thumbnailUrl = map["thumbnailUrl"] as? String ?: "",
                difficultyLevel = map["difficultyLevel"] as? String ?: "",
                price = (map["price"] as? Number)?.toInt() ?: 0,
                rating = (map["rating"] as? Number)?.toDouble() ?: 0.0,
                sections = sections
            )
            
            android.util.Log.d("CourseDetails", "Created course details: title=${courseDetails.title}, sections count=${courseDetails.sections.size}")
            return courseDetails
        }
    }
}

data class CourseSection(
    val title: String = "",
    val description: String = "",
    val order: Int = 0,
    val totalVideos: Int = 0,
    val videos: List<SectionVideo> = emptyList()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "title" to title,
            "description" to description,
            "order" to order,
            "totalVideos" to totalVideos,
            "videos" to videos.map { it.toMap() }
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): CourseSection {
            android.util.Log.d("CourseSection", "Parsing section from map: $map")
            
            // Videos are stored as an array of objects
            val videosArray = map["videos"] as? List<Map<String, Any>> ?: emptyList()
            android.util.Log.d("CourseSection", "Videos array: $videosArray")
            
            val videos = videosArray.mapIndexed { index, videoMap ->
                android.util.Log.d("CourseSection", "Processing video $index: $videoMap")
                SectionVideo.fromMap(videoMap)
            }
            
            android.util.Log.d("CourseSection", "Parsed ${videos.size} videos")
            
            return CourseSection(
                title = map["title"] as? String ?: "",
                description = map["description"] as? String ?: "",
                order = (map["order"] as? Number)?.toInt() ?: 0,
                totalVideos = (map["totalVideos"] as? Number)?.toInt() ?: 0,
                videos = videos
            )
        }
    }
}

data class SectionVideo(
    val title: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val videoUrls: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "title" to title,
            "description" to description,
            "thumbnailUrl" to thumbnailUrl,
            "videoUrls" to videoUrls
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): SectionVideo {
            android.util.Log.d("SectionVideo", "Parsing video from map: $map")
            
            val videoUrls = (map["videoUrls"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            android.util.Log.d("SectionVideo", "Parsed videoUrls: $videoUrls")
            
            val video = SectionVideo(
                title = map["title"] as? String ?: "",
                description = map["description"] as? String ?: "",
                thumbnailUrl = map["thumbnailUrl"] as? String ?: "",
                videoUrls = videoUrls
            )
            
            android.util.Log.d("SectionVideo", "Created video: title=${video.title}, videoUrls count=${video.videoUrls.size}")
            return video
        }
    }
}
