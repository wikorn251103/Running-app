package com.example.myproject.Fragment.workout

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myproject.data.workout.WorkoutLog
import com.example.myproject.data.workout.WorkoutRepository
import com.example.myproject.data.workout.WorkoutStatistics
import kotlinx.coroutines.launch
import java.util.Calendar

class WorkoutHistoryViewModel : ViewModel() {

    private val repository = WorkoutRepository()

    private val _workoutHistory = MutableLiveData<List<WorkoutLog>>()
    val workoutHistory: LiveData<List<WorkoutLog>> get() = _workoutHistory

    private val _statistics = MutableLiveData<WorkoutStatistics>()
    val statistics: LiveData<WorkoutStatistics> get() = _statistics

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    companion object {
        private const val TAG = "WorkoutHistoryViewModel"
    }

    fun loadWorkoutHistory() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val result = repository.getAllWorkoutLogs()

                if (result.isSuccess) {
                    val logs = result.getOrNull() ?: emptyList()
                    _workoutHistory.postValue(logs)
                    Log.d(TAG, "✅ Loaded ${logs.size} workout logs")
                } else {
                    _error.postValue("ไม่สามารถโหลดประวัติได้")
                    Log.e(TAG, "❌ Failed to load workout history")
                }

            } catch (e: Exception) {
                _error.postValue("เกิดข้อผิดพลาด: ${e.message}")
                Log.e(TAG, "❌ Error loading workout history", e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun loadWorkoutHistoryLastWeek() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val calendar = Calendar.getInstance()
                val endTime = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val startTime = calendar.timeInMillis

                val result = repository.getWorkoutLogsByDateRange(startTime, endTime)

                if (result.isSuccess) {
                    val logs = result.getOrNull() ?: emptyList()
                    _workoutHistory.postValue(logs)
                    Log.d(TAG, "✅ Loaded ${logs.size} workout logs (last week)")
                } else {
                    _error.postValue("ไม่สามารถโหลดประวัติได้")
                }

            } catch (e: Exception) {
                _error.postValue("เกิดข้อผิดพลาด: ${e.message}")
                Log.e(TAG, "❌ Error loading workout history (last week)", e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun loadWorkoutHistoryLastMonth() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val calendar = Calendar.getInstance()
                val endTime = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                val startTime = calendar.timeInMillis

                val result = repository.getWorkoutLogsByDateRange(startTime, endTime)

                if (result.isSuccess) {
                    val logs = result.getOrNull() ?: emptyList()
                    _workoutHistory.postValue(logs)
                    Log.d(TAG, "✅ Loaded ${logs.size} workout logs (last month)")
                } else {
                    _error.postValue("ไม่สามารถโหลดประวัติได้")
                }

            } catch (e: Exception) {
                _error.postValue("เกิดข้อผิดพลาด: ${e.message}")
                Log.e(TAG, "❌ Error loading workout history (last month)", e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun loadStatistics() {
        viewModelScope.launch {
            try {
                val result = repository.getWorkoutStatistics()

                if (result.isSuccess) {
                    val stats = result.getOrNull() ?: WorkoutStatistics()
                    _statistics.postValue(stats)
                    Log.d(TAG, "✅ Statistics loaded")
                } else {
                    Log.e(TAG, "❌ Failed to load statistics")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading statistics", e)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}