package com.example.myproject.Fragment.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myproject.data.admin.AdminRepository
import com.example.myproject.data.admin.AthleteModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminProgramViewModel : ViewModel() {

    private val repository = AdminRepository()

    private val _athletes = MutableStateFlow<List<AthleteModel>>(emptyList())
    val athletes: StateFlow<List<AthleteModel>> = _athletes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadAthletes() {
        viewModelScope.launch {
            _isLoading.value = true
            _athletes.value = repository.getAllAthletes()
            _isLoading.value = false
        }
    }

    fun cancelPlan(uid: String) {
        viewModelScope.launch {
            repository.cancelAthletePlan(uid)
            loadAthletes()
        }
    }
}