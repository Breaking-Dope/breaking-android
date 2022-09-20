package com.dope.breaking.model.request

import com.dope.breaking.model.PostLocation
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class RequestPostDataModify(
    val title: String,
    val content: String,
    val location: PostLocation,
    val price: Int,
    val hashtagList: ArrayList<String>,
    val postType: String,
    @get:JsonProperty("isAnonymous")
    val isAnonymous: Boolean,
    val eventDate: String,
) : Serializable