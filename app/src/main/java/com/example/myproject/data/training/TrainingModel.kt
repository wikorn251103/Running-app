package com.example.myproject.data.training

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TrainingModel(
    val day: String = "",
    val description: String = "",
    val pace: String = "",
    val type: String = "",
    val isCompleted: Boolean = false,
    val isMissed: Boolean = false
) : Parcelable
