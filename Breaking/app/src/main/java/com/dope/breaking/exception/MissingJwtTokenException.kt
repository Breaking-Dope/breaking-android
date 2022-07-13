package com.dope.breaking.exception

/**
 * 특정 위치에서 Jwt 토큰을 가져올 때, 토큰 값이 없을 때 발생하는 예외 클래스
 */
class MissingJwtTokenException(message: String) : Exception(message)