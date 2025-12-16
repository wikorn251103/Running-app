package com.example.myproject.data.admin

data class UserStat (
    val name: String,
    val program: String,
    val totalDistance: Double,
    val totalWorkouts: Int,
    val completionRate: Double
)