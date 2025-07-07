package com.unofficial.ahl.model

import com.google.gson.annotations.SerializedName

/**
 * Data class representing a Hebrew word entry from the Academy of Hebrew Language API.
 *
 * Sample response from the API: https://kalanit.hebrew-academy.org.il/api/Ac/?SearchString=אבא
 *
 * [
 *   {
 *     "order": 0,
 *     "score": 80,
 *     "keywordWithoutNikud": null,
 *     "menukadWithoutNikud": "אבא",
 *     "ktiv_male": "אבא",
 *     "menukad": "אַבָּא",
 *     "keyword": "אַבָּא",
 *     "title": "אַבָּא (אבא)",
 *     "itemType": 2,
 *     "hagdara": "צורה עממית של אָב (הורֶה) בעיקר בפנייה לאב ובסגנון משפחתי | אָבִי (בלשון המשנה)",
 *     "isRashi": 1,
 *     "kodBinyan": 0,
 *     "IndexName": "kalanit-ac-2025_07_02_12_07_42_742_prod_local_kalanit.hebrew-academy.org.il",
 *     "ItemTypeName": "AcItemShem"
 *   },
 *   {
 *     "order": 1,
 *     "score": 1,
 *     "keywordWithoutNikud": null,
 *     "menukadWithoutNikud": "אבא מרי, אבי מרי",
 *     "ktiv_male": "אבא מרי, אבי מרי",
 *     "menukad": "אַבָּא מָרִי, אָבִי מָרִי",
 *     "keyword": "אַבָּא מָרִי",
 *     "title": "אַבָּא מָרִי, אָבִי מָרִי (אבא מרי, אבי מרי)",
 *     "itemType": 2,
 *     "hagdara": "כינוי כבוד לאדם בפי בנו",
 *     "isRashi": 1,
 *     "kodBinyan": 0,
 *     "IndexName": "kalanit-ac-2025_07_02_12_07_42_742_prod_local_kalanit.hebrew-academy.org.il",
 *     "ItemTypeName": "AcItemShem"
 *   }
 * ]
 *
 *
 */
data class HebrewWord(
    @SerializedName("keyword")
    val keyword: String?,
    
    @SerializedName("menukad")
    val menukad: String?,
    
    @SerializedName("keywordWithoutNikud")
    val keywordWithoutNikud: String?,
    
    @SerializedName("menukadWithoutNikud")
    val menukadWithoutNikud: String?,
    
    @SerializedName("ktiv_male")
    val ktivMale: String?,
    
    @SerializedName("title")
    val title: String?,
    
    @SerializedName("hagdara")
    val definition: String?,
    
    @SerializedName("order")
    val order: Int? = null,
    
    @SerializedName("score")
    val score: Int,
    
    @SerializedName("itemType")
    val itemType: Int,
    
    @SerializedName("ItemTypeName")
    val itemTypeName: String?,
    
    @SerializedName("isRashi")
    val isRashi: Int,
    
    @SerializedName("kodBinyan")
    val kodBinyan: Int,
    
    @SerializedName("IndexName")
    val indexName: String? = null
) {
    /**
     * Check if this HebrewWord has valid content for display
     * @return true if the word has at least title or menukad values
     */
    fun hasValidContent(): Boolean {
        return !title.isNullOrBlank() || 
               !menukad.isNullOrBlank() || 
               !menukadWithoutNikud.isNullOrBlank() || 
               !ktivMale.isNullOrBlank()
    }
} 