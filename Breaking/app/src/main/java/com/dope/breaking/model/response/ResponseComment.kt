package com.dope.breaking.model.response

import com.google.gson.annotations.SerializedName

data class ResponseComment(
    @SerializedName("commentId") val commentId: Int,
    @SerializedName("content") val content: String,
    @SerializedName("likeCount") val likeCount: Int,
    @SerializedName("replyCount") val replyCount: Int,
    @SerializedName("user") val user: ResponseCommentUser,
    @SerializedName("isLiked") val isLiked: Boolean,
    @SerializedName("createdDate") val createdDate: String
)
