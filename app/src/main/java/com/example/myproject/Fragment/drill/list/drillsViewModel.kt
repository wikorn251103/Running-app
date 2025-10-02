package com.example.myproject.Fragment.drill.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myproject.data.drill.drillModel
import com.example.myproject.data.drill.drillRepository
import kotlinx.coroutines.launch

class drillsViewModel(private val repository: drillRepository) : ViewModel() {

    private val _drills = MutableLiveData<List<drillModel>>()
    val drills: LiveData<List<drillModel>> = _drills

    fun getDrills() {
        viewModelScope.launch {
            repository.getdrillsFlow().collect { drills ->
                _drills.value = drills
            }
        }
    }
}
