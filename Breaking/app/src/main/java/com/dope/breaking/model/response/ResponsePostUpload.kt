package com.dope.breaking.model.response

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ResponsePostUpload (
    @SerializedName("postId") val postId : Long
) : Serializable