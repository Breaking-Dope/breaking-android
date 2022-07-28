package com.dope.breaking.model

data class FollowData(
    val userId: Long,
    val nickname: String,
    val statusMsg: String,
    val profileImgURL: String
)