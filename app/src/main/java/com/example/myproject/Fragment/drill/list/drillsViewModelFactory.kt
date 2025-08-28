package com.example.myproject.Fragment.drill.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myproject.data.article.drill.drillRepository

class drillsViewModelFactory(
    private val repository: drillRepository
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(drillsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return drillsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}