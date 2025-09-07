package com.humblecoders.humblecoders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScheduleUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val generatedSchedule: StudySchedule? = null,
    val userSchedules: List<StudySchedule> = emptyList(),
    val isGenerating: Boolean = false
)

class ScheduleViewModel : ViewModel() {
    private val repository = ScheduleRepository()
    
    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    fun generateSchedule(
        courseDetails: CourseDetails,
        userTimeframe: String,
        userId: String
    ) {
        android.util.Log.d("ScheduleViewModel", "Starting schedule generation")
        _uiState.value = _uiState.value.copy(
            isGenerating = true,
            error = null,
            generatedSchedule = null
        )

        viewModelScope.launch {
            try {
                android.util.Log.d("ScheduleViewModel", "Calling repository to generate schedule")
                val result = repository.generateStudySchedule(courseDetails, userTimeframe, userId)
                if (result.isSuccess) {
                    val schedule = result.getOrThrow()
                    android.util.Log.d("ScheduleViewModel", "Schedule generated successfully: ${schedule.courseTitle}")
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        generatedSchedule = schedule,
                        userSchedules = _uiState.value.userSchedules + schedule
                    )
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to generate schedule"
                    android.util.Log.e("ScheduleViewModel", "Schedule generation failed: $error")
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        error = error
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ScheduleViewModel", "Exception in schedule generation", e)
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun loadUserSchedules(userId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val result = repository.getUserSchedules(userId)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userSchedules = result.getOrThrow()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to load schedules"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearGeneratedSchedule() {
        _uiState.value = _uiState.value.copy(generatedSchedule = null)
    }
}
