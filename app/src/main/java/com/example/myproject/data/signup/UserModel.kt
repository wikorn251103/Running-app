package com.example.myproject.data.signup

data class UserModel(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val height: Int = 0,
    val weight: Double = 0.0,
    val age: Int = 0,
    val gender: String = "",
    val trainingPlan: String? = null,
    val role: String = "user"
)