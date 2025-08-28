package com.example.myproject.data.article.drill

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class drillModel (
    val title: String = "",
    val subtitle: String = "",
    val description: String = "",
    //val imageUrl: String = ""
) : Parcelable