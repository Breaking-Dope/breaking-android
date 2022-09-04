package com.dope.breaking.model.response

import com.google.gson.annotations.SerializedName

data class ResponseTransaction(
    @SerializedName("cursorId") val cursorId: Int,
    @SerializedName("transactionDate") val transactionDate: String,
    @SerializedName("transactionType") val transactionType: String,
    @SerializedName("amount") val amount: Int,
    @SerializedName("balance") val balance: Int,
    @SerializedName("postId") val postId: Int?,
    @SerializedName("postTitle") val postTitle: String?,
    @SerializedName("targetUser") val targetUser: ResponseJwtUserInfo?
)