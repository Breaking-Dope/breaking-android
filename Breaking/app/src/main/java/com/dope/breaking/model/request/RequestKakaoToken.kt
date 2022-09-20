package com.dope.breaking.model.request

class RequestKakaoToken {
    // 카카오 로그인 성공 시 받는 액세스 토큰
    var accessToken: String

    constructor(accessToken: String) {
        this.accessToken = accessToken
    }
}