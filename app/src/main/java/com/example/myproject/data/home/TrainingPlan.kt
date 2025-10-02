package com.example.myproject.data.home

import com.example.myproject.data.training.TrainingModel

data class TrainingPlan(
    val id: String = "",
    val name: String = "",
    val weeks: Map<String, Map<String, TrainingModel>> = emptyMap()
)