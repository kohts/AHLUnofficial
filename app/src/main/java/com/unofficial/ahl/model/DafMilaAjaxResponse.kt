package com.unofficial.ahl.model

import com.google.gson.annotations.SerializedName

/**
 * Response wrapper for the Hebrew Academy AJAX API call
 * Contains success flag and nested JSON data string
 */
data class DafMilaAjaxResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: String? // Contains JSON-encoded string with DafMilaResponse structure
) 