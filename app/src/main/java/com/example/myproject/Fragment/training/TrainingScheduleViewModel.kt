package com.example.myproject.Fragment.training

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myproject.data.training.TrainingModel
import com.example.myproject.data.training.TrainingRepository

class TrainingScheduleViewModel(private val repository: TrainingRepository) : ViewModel() {

    private val _trainingDays = MutableLiveData<List<TrainingModel>>()
    val trainingDays: LiveData<List<TrainingModel>> get() = _trainingDays

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    var selectedTrainingPlanId: String? = null
    var currentWeek = 1

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
}



