package com.humblecoders.humblecoders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CourseViewModel : ViewModel() {
    private val repository = CourseRepository()

    private val _courses = MutableStateFlow<List<CourseCategory>>(emptyList())
    val courses: StateFlow<List<CourseCategory>> = _courses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredCourses = MutableStateFlow<List<CourseCategory>>(emptyList())
    val filteredCourses: StateFlow<List<CourseCategory>> = _filteredCourses.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val _courseDetails = MutableStateFlow<CourseDetails?>(null)
    val courseDetails: StateFlow<CourseDetails?> = _courseDetails.asStateFlow()

    fun loadCourses() {
        viewModelScope.launch {
            android.util.Log.d("CourseViewModel", "Starting to load courses")
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = repository.getAllCourses()
                if (result.isSuccess) {
                    val courses = result.getOrThrow()
                    android.util.Log.d("CourseViewModel", "Successfully loaded ${courses.size} course categories")
                    courses.forEach { category ->
                        android.util.Log.d("CourseViewModel", "Category: ${category.categoryName}, Courses: ${category.courses.size}")
                        category.courses.forEach { course ->
                            android.util.Log.d("CourseViewModel", "Course: ${course.title}, Thumbnail: ${course.thumbnailUrl}")
                        }
                    }
                    _courses.value = courses
                    _filteredCourses.value = courses // Initialize filtered courses
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to load courses"
                    android.util.Log.e("CourseViewModel", "Failed to load courses: $error")
                    _errorMessage.value = error
                }
            } catch (e: Exception) {
                android.util.Log.e("CourseViewModel", "Exception while loading courses", e)
                _errorMessage.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
                android.util.Log.d("CourseViewModel", "Finished loading courses")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterCourses()
        
        // Add to search history if query is not empty and not already in history
        if (query.isNotEmpty() && !_searchHistory.value.contains(query)) {
            val newHistory = listOf(query) + _searchHistory.value.take(4) // Keep last 5 searches
            _searchHistory.value = newHistory
        }
    }

    private fun filterCourses() {
        val query = _searchQuery.value.lowercase().trim()
        val allCourses = _courses.value

        if (query.isEmpty()) {
            _filteredCourses.value = allCourses
        } else {
            val filtered = allCourses.map { category ->
                val filteredCoursesInCategory = category.courses.filter { course ->
                    course.title.lowercase().contains(query) ||
                    course.description.lowercase().contains(query) ||
                    course.instructor.lowercase().contains(query)
                }
                if (filteredCoursesInCategory.isNotEmpty()) {
                    category.copy(courses = filteredCoursesInCategory)
                } else null
            }.filterNotNull()

            _filteredCourses.value = filtered
        }
    }

    fun getCoursesByCategory(categoryName: String): List<Course> {
        return _courses.value.find { it.categoryName == categoryName }?.courses ?: emptyList()
    }

    fun getOrderedCourses(preferredCategory: String?): List<CourseCategory> {
        val allCourses = _filteredCourses.value
        if (allCourses.isEmpty()) return allCourses

        return when (preferredCategory) {
            "App Dev" -> {
                val appDev = allCourses.find { it.categoryName == "App Dev" }
                val webDev = allCourses.find { it.categoryName == "Web Dev" }
                val python = allCourses.find { it.categoryName == "Python" }
                listOfNotNull(appDev, webDev, python)
            }
            "Web Dev" -> {
                val webDev = allCourses.find { it.categoryName == "Web Dev" }
                val appDev = allCourses.find { it.categoryName == "App Dev" }
                val python = allCourses.find { it.categoryName == "Python" }
                listOfNotNull(webDev, appDev, python)
            }
            "Python" -> {
                val python = allCourses.find { it.categoryName == "Python" }
                val appDev = allCourses.find { it.categoryName == "App Dev" }
                val webDev = allCourses.find { it.categoryName == "Web Dev" }
                listOfNotNull(python, appDev, webDev)
            }
            else -> allCourses
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _filteredCourses.value = _courses.value
    }

    fun loadCourseDetails(courseTitle: String) {
        viewModelScope.launch {
            android.util.Log.d("CourseViewModel", "Loading course details for: $courseTitle")
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = repository.getCourseDetailsByTitle(courseTitle)
                if (result.isSuccess) {
                    val courseDetails = result.getOrThrow()
                    android.util.Log.d("CourseViewModel", "Successfully loaded course details: ${courseDetails?.title}")
                    _courseDetails.value = courseDetails
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to load course details"
                    android.util.Log.e("CourseViewModel", "Failed to load course details: $error")
                    _errorMessage.value = error
                }
            } catch (e: Exception) {
                android.util.Log.e("CourseViewModel", "Exception while loading course details", e)
                _errorMessage.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
                android.util.Log.d("CourseViewModel", "Finished loading course details")
            }
        }
    }

    fun updateCourseRating(courseTitle: String, newRating: Double) {
        viewModelScope.launch {
            android.util.Log.d("CourseViewModel", "Starting rating update for course: $courseTitle with rating: $newRating")
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = repository.updateCourseRating(courseTitle, newRating)
                if (result.isSuccess) {
                    android.util.Log.d("CourseViewModel", "Rating updated successfully, reloading courses and course details")
                    // Reload courses to get updated ratings
                    loadCourses()
                    // Also reload the specific course details to update the UI immediately
                    loadCourseDetails(courseTitle)
                } else {
                    android.util.Log.e("CourseViewModel", "Failed to update rating: ${result.exceptionOrNull()?.message}")
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to update rating"
                }
            } catch (e: Exception) {
                android.util.Log.e("CourseViewModel", "Exception while updating rating", e)
                _errorMessage.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
                android.util.Log.d("CourseViewModel", "Finished updating rating")
            }
        }
    }
}
