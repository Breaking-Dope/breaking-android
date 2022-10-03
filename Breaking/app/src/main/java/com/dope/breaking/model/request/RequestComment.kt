package com.dope.breaking.model.request

data class RequestComment(
    val content: String,
    val hashTagList: ArrayList<String>?
)
