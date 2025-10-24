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

    private var selectedFeeling: String = ""

    companion object {
        private const val TAG = "RecordWorkoutViewModel"
    }

    fun setFeeling(feeling: String) {
        selectedFeeling = feeling
    }

    fun saveWorkout(
        trainingData: TrainingModel,
        weekNumber: Int,
        dayNumber: Int,
        distance: Double,
        duration: Long,
        calories: Int,
        heartRate: Int,
        notes: String
    ) {
        viewModelScope.launch {
            try {
                _isSaving.value = true
                _error.value = null

                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _error.postValue("ไม่พบข้อมูลผู้ใช้")
                    return@launch
                }

                // สร้าง WorkoutLog
                val workoutLog = WorkoutLog(
                    userId = userId,
                    programId = "", // จะดึงจาก SharedPreferences หรือส่งมาเป็น parameter
                    weekNumber = weekNumber,
                    dayNumber = dayNumber,
                    dayName = trainingData.day ?: "",
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
                    feeling = selectedFeeling,
                    isCompleted = true
                )

                // บันทึก WorkoutLog
                val saveResult = repository.saveWorkoutLog(workoutLog)

                if (saveResult.isSuccess) {
                    Log.d(TAG, "✅ Workout log saved successfully")

                    // อัพเดทสถานะใน Athletes
                    val markResult = repository.markDayAsCompleted(weekNumber, dayNumber)

                    if (markResult.isSuccess) {
                        Log.d(TAG, "✅ Day marked as completed in Athletes")
                        _saveSuccess.postValue(true)
                    } else {
                        Log.w(TAG, "⚠️ Failed to mark day as completed, but workout saved")
                        _saveSuccess.postValue(true)
                    }
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
        val minutes = paceInSeconds / 60
        val seconds = paceInSeconds % 60

        return String.format("%d:%02d", minutes, seconds)
    }

    fun clearError() {
        _error.value = null
    }
}