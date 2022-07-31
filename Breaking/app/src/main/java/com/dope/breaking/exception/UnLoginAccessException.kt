package com.dope.breaking.exception

/**
 * 로그인한 상태에서만 접근가능한 활동에서 비로그인 상태로 접근했을 때 발생하는 예외
 */
class UnLoginAccessException(message: String) : Exception(message)