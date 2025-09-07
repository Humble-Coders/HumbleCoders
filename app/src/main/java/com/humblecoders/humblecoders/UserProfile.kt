package com.humblecoders.humblecoders

import com.google.firebase.Timestamp
import java.util.Date

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val skills: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    val preferredCourseCategory: String = "",
    val experienceLevel: String = "",
    val learningGoals: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val luxandPersonId: String = "",
    val faceUuid: String = "",
    val isOnboardingComplete: Boolean = false,
    val enrolledCourses: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "email" to email,
            "displayName" to displayName,
            "photoUrl" to photoUrl,
            "skills" to skills,
            "interests" to interests,
            "preferredCourseCategory" to preferredCourseCategory,
            "experienceLevel" to experienceLevel,
            "learningGoals" to learningGoals,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "luxandPersonId" to luxandPersonId,
            "faceUuid" to faceUuid,
            "isOnboardingComplete" to isOnboardingComplete,
            "enrolledCourses" to enrolledCourses
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): UserProfile {
            return UserProfile(
                uid = map["uid"] as? String ?: "",
                email = map["email"] as? String ?: "",
                displayName = map["displayName"] as? String ?: "",
                photoUrl = map["photoUrl"] as? String ?: "",
                skills = (map["skills"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                interests = (map["interests"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                preferredCourseCategory = map["preferredCourseCategory"] as? String ?: "",
                experienceLevel = map["experienceLevel"] as? String ?: "",
                learningGoals = map["learningGoals"] as? String ?: "",
                createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
                updatedAt = map["updatedAt"] as? Timestamp ?: Timestamp.now(),
                luxandPersonId = map["luxandPersonId"] as? String ?: "",
                faceUuid = map["faceUuid"] as? String ?: "",
                isOnboardingComplete = map["isOnboardingComplete"] as? Boolean ?: false,
                enrolledCourses = (map["enrolledCourses"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            )
        }
    }
}
