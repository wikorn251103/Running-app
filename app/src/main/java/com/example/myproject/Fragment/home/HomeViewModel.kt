package com.example.myproject.Fragment.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myproject.data.home.TrainingPlanRepository
import com.example.myproject.data.training.TrainingModel
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repository = TrainingPlanRepository()

    private val _trainingPlan = MutableLiveData<Map<String, Map<String, TrainingModel>>>()
    val trainingPlan: LiveData<Map<String, Map<String, TrainingModel>>> get() = _trainingPlan

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun loadTrainingPlan(planId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                Log.d("HomeViewModel", "Loading training plan: $planId")

                val result = repository.getTrainingPlan(planId)

                if (result.isNotEmpty()) {
                    _trainingPlan.postValue(result)
                    Log.d("HomeViewModel", "Training plan loaded successfully: ${result.keys}")
                } else {
                    _error.postValue("ไม่พบข้อมูลโปรแกรม")
                    Log.w("HomeViewModel", "Training plan is empty for: $planId")
                }

            } catch (e: Exception) {
                _error.postValue("เกิดข้อผิดพลาด: ${e.message}")
                Log.e("HomeViewModel", "Error loading training plan", e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}