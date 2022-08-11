package com.dope.breaking.exception

/**
 * Geocoder 로부터 현재 위치를 올바르게 가져오지 못한 경우
 */
class FailedGetLocationException(message: String) : Exception(message)