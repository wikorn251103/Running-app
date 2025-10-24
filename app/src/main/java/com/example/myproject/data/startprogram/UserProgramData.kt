package com.example.myproject.data.startprogram

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UserProgramData(
    val userId: String,
    val userName: String,
    val email: String,
    val programId: String,
    val programDisplayName: String,
    val subProgramName: String,
    var currentWeek: Int,
    val totalWeeks: Int = 4,
    var progress: Int,
    var completedSessions: Int,
    val totalSessions: Int = 12,
    var isActive: Boolean,
    val selectedAt: Long
) : Parcelable