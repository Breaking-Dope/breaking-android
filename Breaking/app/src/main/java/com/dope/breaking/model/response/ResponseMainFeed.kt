package com.dope.breaking.model.response

import com.dope.breaking.model.PostLocation
import com.google.gson.annotations.SerializedName

data class ResponseMainFeed(
    @SerializedName("postId") val postId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("location") val location: PostLocation,
    @SerializedName("thumbnailImgURL") val thumbnailImgURL: String?,
    @SerializedName("likeCount") val likeCount: Int,
    @SerializedName("commentCount") val commentCount: Int,
    @SerializedName("postType") val postType: String,
    @SerializedName("viewCount") val viewCount: Int,
    @SerializedName("user") val user: ResponseJwtUserInfo?,
    @SerializedName("price") val price: Int,
    @SerializedName("createdDate") val createdDate: String,
    @SerializedName("isPurchasable") val isPurchasable: Boolean,
    @SerializedName("isSold") val isSold: Boolean,
    @SerializedName("isAnonymous") val isAnonymous: Boolean,
    @SerializedName("isMyPost") val isMyPost: Boolean,
    @SerializedName("isLiked") val isLiked: Boolean,
    @SerializedName("isBookmarked") var isBookmarked: Boolean,
)