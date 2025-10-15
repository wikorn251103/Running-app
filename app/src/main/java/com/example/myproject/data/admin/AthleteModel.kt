package com.example.myproject.data.admin

data class AthleteModel(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val trainingPlan: String = "",
    val age: Int? = null,
    val gender: String? = null,
    val height: Int? = null,
    val weight: Int? = null
)