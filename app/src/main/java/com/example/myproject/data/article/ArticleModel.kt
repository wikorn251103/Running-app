package com.example.myproject.data.article

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ArticleModel (
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
) : Parcelable