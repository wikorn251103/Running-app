package com.example.myproject.data.admin

data class TrainingProgramModel(
    val programId: String = "",
    val programName: String = "",
    val category: String = "",
    val weeks: Int = 0,
    val daysPerWeek: Int = 7,
    val activeUsers: Int = 0,
    val completedUsers: Int = 0,
    val createdAt: Long = 0
)