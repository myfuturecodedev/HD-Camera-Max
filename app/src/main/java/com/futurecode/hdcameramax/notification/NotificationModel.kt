package com.futurecode.hdcameramax.notification

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep // ✅ FIX: Prevents R8 from shrinking or stripping this class on Release builds
data class NotificationModel(
    @SerializedName("banner") val banner: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("link") val link: String
)