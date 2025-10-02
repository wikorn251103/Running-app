package com.example.myproject.data.drill

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class drillModel(
    val title: String = "",
    val timeRange: String = "",
    val setRange: String = "",
    val description: String = "",
    val subtitle: String = "",
    val step: String = "",
    val videoUrl: String = "",
    val focus: String = ""
) : Parcelable