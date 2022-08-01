package com.dope.breaking.exception

/**
 * 이미지나 영상 파일을 선택했는데 URI 등의 정보를 올바르게 가져오지 못한 경우
 */
class ErrorFileSelectedException(message: String) : Exception(message)