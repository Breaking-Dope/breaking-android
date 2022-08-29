package com.dope.breaking.model

data class FollowData(
    val cursorId: Int, // DB에 저장된 PK
    val userId: Long, // 유저 고유 아이디
    val nickname: String, // 닉네임
    val statusMsg: String, // 상태 메세지
    val profileImgURL: String?, // 프로필 이미지 URL
    var isFollowing: Boolean // 내가 userId 에 해당하는 사람을 팔로우하고 있는지
)