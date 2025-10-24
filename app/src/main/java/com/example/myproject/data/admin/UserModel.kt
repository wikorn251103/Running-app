package com.example.myproject.data.admin

data class UserModel(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val role: String = "user",
    val createdAt: Long = 0,
    val hasActiveProgram: Boolean = false,
    val currentProgramId: String = "",
    val programDisplayName: String,
    val dayKey: String = "",
    var day: String = "",
    var description: String = ""
)