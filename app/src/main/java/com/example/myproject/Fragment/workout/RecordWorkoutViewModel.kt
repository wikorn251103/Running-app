package com.example.myproject.Fragment.workout

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myproject.data.training.TrainingModel
import com.example.myproject.data.workout.WorkoutLog
import com.example.myproject.data.workout.WorkoutRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class RecordWorkoutViewModel : ViewModel() {

    private val repository = WorkoutRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _isSaving = MutableLiveData<Boolean>()
    val isSaving: LiveData<Boolean> get() = _isSaving

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> get() = _saveSuccess

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    companion object {
        private const val TAG = "RecordWorkoutViewModel"
    }

    fun saveWorkout(
        trainingData: TrainingModel,
        weekNumber: Int,
        dayNumber: Int,
        distance: Double,
        duration: Long,
        calories: Int,
        heartRate: Int,
        notes: String,
        paceResult: String = "",       // ✅ เพิ่ม
        paceDiffSeconds: Int = 0       // ✅ เพิ่ม
    ) {
        viewModelScope.launch {
            try {
                _isSaving.value = true
                _error.value = null

                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _error.postValue("ไม่พบข้อมูลผู้ใช้")
                    _isSaving.postValue(false)
                    return@launch
                }

                val workoutLog = WorkoutLog(
                    userId = userId,
                    programId = "",
                    weekNumber = weekNumber,
                    dayNumber = dayNumber,
                    dayName = trainingData.day,
                    trainingType = trainingData.type,
                    plannedDescription = trainingData.description,
                    plannedPace = trainingData.pace,
                    actualDistance = distance,
                    actualDuration = duration,
                    actualPace = calculatePace(distance, duration),
                    calories = calories,
                    averageHeartRate = heartRate,
                    completedAt = System.currentTimeMillis(),
                    notes = notes,
                    feeling = paceResult,          // ✅ เก็บ paceResult ใน feeling ด้วย (backward compat)
                    paceResult = paceResult,        // ✅
                    paceDiffSeconds = paceDiffSeconds, // ✅
                    isCompleted = true
                )

                val saveResult = repository.saveWorkoutLog(workoutLog)

                if (saveResult.isSuccess) {
                    Log.d(TAG, "✅ Workout log saved | paceResult=$paceResult | diff=${paceDiffSeconds}s")
                    val markResult = repository.markDayAsCompleted(weekNumber, dayNumber)
                    if (!markResult.isSuccess) {
                        Log.w(TAG, "⚠️ Failed to mark day, but workout saved")
                    }
                    _saveSuccess.postValue(true)
                } else {
                    _error.postValue("ไม่สามารถบันทึกข้อมูลได้")
                    Log.e(TAG, "❌ Failed to save workout log")
                }

            } catch (e: Exception) {
                _error.postValue("เกิดข้อผิดพลาด: ${e.message}")
                Log.e(TAG, "❌ Error saving workout", e)
            } finally {
                _isSaving.postValue(false)
            }
        }
    }

    private fun calculatePace(distance: Double, duration: Long): String {
        if (distance <= 0 || duration <= 0) return "0:00"
        val paceInSeconds = (duration / distance).toInt()
        return String.format("%d:%02d", paceInSeconds / 60, paceInSeconds % 60)
    }

    fun clearError() {
        _error.value = null
    }
}