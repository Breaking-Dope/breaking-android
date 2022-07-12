package com.dope.breaking.exception

/**
 * Http 통신에서 status code 가 2xx이 아닌 경우에 발생하는 예외 클래스
 */
class ResponseErrorException(message: String) : Exception(message)