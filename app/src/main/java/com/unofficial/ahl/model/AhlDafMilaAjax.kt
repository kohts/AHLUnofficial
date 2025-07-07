package com.unofficial.ahl.model

import com.google.gson.annotations.SerializedName

/**
 * Data class representing the ahl_daf_mila_ajax JavaScript object
 * Contains configuration data needed for AJAX requests to get detailed word information
 */
data class AhlDafMilaAjax(
    @SerializedName("ajax_url")
    val ajaxUrl: String,
    
    @SerializedName("nonce")
    val nonce: String,
    
    @SerializedName("keyword")
    val keyword: String,
    
    @SerializedName("plugin_url")
    val pluginUrl: String
) 