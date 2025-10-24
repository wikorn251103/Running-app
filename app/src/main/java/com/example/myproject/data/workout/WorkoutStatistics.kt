package com.example.myproject.data.workout

data class WorkoutStatistics(
    val totalWorkouts: Int = 0,
    val totalDistance: Double = 0.0,
    val totalDuration: Long = 0,
    val totalCalories: Int = 0,
    val averagePace: String = "0:00",
    val longestRun: Double = 0.0,
    val fastestPace: String = "0:00"
)