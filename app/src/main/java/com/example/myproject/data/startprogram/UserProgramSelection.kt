package com.example.myproject.data.startprogram

data class UserProgramSelection(
    val userId: String = "",
    val programId: String = "",
    val programDisplayName: String = "",
    val subProgramName: String = "",
    val selectedAt: Long = System.currentTimeMillis(),
    val currentWeek: Int = 1,
    val isActive: Boolean = true
)