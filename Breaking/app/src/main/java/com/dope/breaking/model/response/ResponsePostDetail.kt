package com.dope.breaking.model.response

import android.os.Parcel
import android.os.Parcelable
import com.dope.breaking.model.PostLocation
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ResponsePostDetail(
    @SerializedName("isLiked") val isLiked: Boolean,
    @SerializedName("isBookmarked") val isBookmarked: Boolean,
    @SerializedName("isPurchased") val isPurchased: Boolean,
    @SerializedName("isPurchasable") var isPurchasable: Boolean,
    @SerializedName("user") val user: ResponsePostWriter?,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String,
    @SerializedName("mediaList") val mediaList: ArrayList<String?>,
    @SerializedName("location") val location: PostLocation,
    @SerializedName("hashtagList") val hashtagList: ArrayList<String>,
    @SerializedName("price") val price: Long,
    @SerializedName("postType") val postType: String,
    @SerializedName("eventDate") val eventDate: String,
    @SerializedName("createdDate") val createdDate: String,
    @SerializedName("modifiedDate") val modifiedDate: String,
    @SerializedName("viewCount") val viewCount: Int,
    @SerializedName("bookmarkedCount") val bookmarkedCount: Long,
    @SerializedName("soldCount") val soldCount: Long,
    @SerializedName("isAnonymous") val isAnonymous: Boolean,
    @SerializedName("isSold") val isSold: Boolean,
    @SerializedName("isHidden") val isHidden: Boolean,
    @SerializedName("isMyPost") val isMyPost: Boolean,
    @SerializedName("likeCount") val likeCount: Long,
    @SerializedName("commentCount") val commentCount: Long,
) : Serializable