package com.unofficial.ahl.model

import com.google.gson.annotations.SerializedName

/**
 * Data class representing a Hebrew word entry from the Academy of Hebrew Language API
 */
data class HebrewWord(
    @SerializedName("keyword")
    val keyword: String,
    
    @SerializedName("menukad")
    val menukad: String?,
    
    @SerializedName("keywordWithoutNikud")
    val keywordWithoutNikud: String,
    
    @SerializedName("ktiv_male")
    val ktivMale: String?,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("hagdara")
    val definition: String?,
    
    @SerializedName("score")
    val score: Int,
    
    @SerializedName("itemType")
    val itemType: Int,
    
    @SerializedName("ItemTypeName")
    val itemTypeName: String,
    
    @SerializedName("isRashi")
    val isRashi: Int,
    
    @SerializedName("kodBinyan")
    val kodBinyan: Int
) 