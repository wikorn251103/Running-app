package com.example.myproject.data.startprogram

data class TrainingLog(
    val id: String,
    val programId: String,
    val week: Int,
    val day: Int,
    val sessionName: String,
    val distance: Double,
    val duration: Long,
    val pace: String,
    val completedAt: Long,
    val status: String,
    val notes: String
)