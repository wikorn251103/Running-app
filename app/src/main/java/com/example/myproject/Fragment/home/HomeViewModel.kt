package com.example.myproject.Fragment.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myproject.data.training.TrainingModel
import com.example.myproject.data.training.TrainingPlanRepository
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repository = TrainingPlanRepository()

    private val _trainingPlan = MutableLiveData<Map<String, Map<String, TrainingModel>>>()
    val trainingPlan: LiveData<Map<String, Map<String, TrainingModel>>> get() = _trainingPlan

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    companion object {
        private const val TAG = "HomeViewModel"
    }

    /**
     * ✅ Clear cache และโหลดข้อมูลใหม่ (สำหรับ refresh)
     */
    fun refreshTrainingPlan(planId: String) {
        Log.d(TAG, "🔄 Refreshing training plan - clearing cache first")
        _trainingPlan.value = emptyMap() // ✅ Clear cache
        loadTrainingPlanFromAthlete(planId)
    }

    /**
     * ดึงข้อมูลจาก Athletes/{userId}
     */
    fun loadTrainingPlanFromAthlete(planId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                Log.d(TAG, "📥 Loading training plan from Athletes for: $planId")

                val result = repository.getTrainingPlanFromAthlete()

                if (result.isNotEmpty()) {
                    _trainingPlan.postValue(result)
                    Log.d(TAG, "✅ Training plan loaded successfully from Athletes: ${result.keys}")
                } else {
                    _error.postValue("ไม่พบข้อมูลโปรแกรม")
                    Log.w(TAG, "⚠️ Training plan is empty for: $planId")
                }

            } catch (e: Exception) {
                _error.postValue("เกิดข้อผิดพลาด: ${e.message}")
                Log.e(TAG, "❌ Error loading training plan from Athletes", e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * เก่า - ดึงจาก training_plans
     */
    fun loadTrainingPlan(planId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                Log.d(TAG, "📥 Loading training plan: $planId")

                val result = repository.getTrainingPlan(planId)

                if (result.isNotEmpty()) {
                    _trainingPlan.postValue(result)
                    Log.d(TAG, "✅ Training plan loaded successfully: ${result.keys}")
                } else {
                    _error.postValue("ไม่พบข้อมูลโปรแกรม")
                    Log.w(TAG, "⚠️ Training plan is empty for: $planId")
                }

            } catch (e: Exception) {
                _error.postValue("เกิดข้อผิดพลาด: ${e.message}")
                Log.e(TAG, "❌ Error loading training plan", e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * ✅ Clear ข้อมูลทั้งหมด (เมื่อออกจากโปรแกรม)
     */
    fun clearTrainingPlan() {
        Log.d(TAG, "🗑️ Clearing training plan cache")
        _trainingPlan.value = emptyMap()
        _error.value = null
    }
}