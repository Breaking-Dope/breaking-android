package com.dope.breaking.model.request

class RequestGoogleAccessToken(
    val client_id: String,
    val client_secret: String,
    val code: String,
    val grant_type: String = "authorization_code",
    val redirect_uri: String = ""
)