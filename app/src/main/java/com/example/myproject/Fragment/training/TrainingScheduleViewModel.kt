package com.example.myproject.Fragment.training

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myproject.data.training.TrainingModel
import com.example.myproject.data.training.TrainingRepository
import com.google.firebase.firestore.ListenerRegistration

class TrainingScheduleViewModel(private val repository: TrainingRepository) : ViewModel() {

    private val _trainingDays = MutableLiveData<List<TrainingModel>>()
    val trainingDays: LiveData<List<TrainingModel>> get() = _trainingDays

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    var selectedTrainingPlanId: String? = null
    var currentWeek = 1

    private var weekListener: ListenerRegistration? = null

    companion object {
        private const val TAG = "TrainingScheduleVM"
    }

    /**
     * ⭐ โหลดข้อมูลแบบ Real-time
     */
    fun loadTrainingWeekRealtime(week: Int) {
        _loading.value = true
        _error.value = null

        Log.d(TAG, "📡 Loading week $week with real-time updates")

        // ยกเลิก listener เก่า
        weekListener?.remove()

        // สร้าง listener ใหม่
        weekListener = repository.getTrainingWeekDataRealtime(week,
            onSuccess = { days ->
                _loading.value = false
                _trainingDays.value = days
                Log.d(TAG, "✅ Received ${days.size} days")
            },
            onFailure = { exception ->
                _loading.value = false
                _error.value = exception.message
                Log.e(TAG, "❌ Error: ${exception.message}")
            }
        )
    }

    /**
     * เก่า - โหลดแบบปกติ (ไม่ real-time)
     */
    fun loadTrainingWeek(planId: String, week: Int) {
        _loading.value = true
        _error.value = null

        repository.getTrainingWeekData(planId, week,
            onSuccess = { days ->
                _loading.value = false
                _trainingDays.value = days
            },
            onFailure = { exception ->
                _loading.value = false
                _error.value = exception.message
            }
        )
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // ยกเลิก listener เมื่อ ViewModel ถูกทำลาย
        weekListener?.remove()
        repository.removeListener()
        Log.d(TAG, "🔕 Listeners cleaned up")
    }
}