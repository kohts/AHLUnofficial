package com.unofficial.ahl.util

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.unofficial.ahl.model.AhlDafMilaAjax
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Utility class for parsing HTML content
 */
object HtmlParser {
    
    private val gson = Gson()
    
    /**
     * Extract content from HTML between specific markers and clean up script tags
     * 
     * @param htmlContent The raw HTML content
     * @param startMarker The beginning marker to extract from
     * @param endMarker The ending marker to extract to
     * @return The extracted and cleaned content
     */
    fun extractContent(
        htmlContent: String,
        startMarker: String = "<div id='post-millon' class='post millon'>",
        endMarker: String = "</div><!-- #content-container -->"
    ): String {

        // Get content between markers
        val startIndex = htmlContent.indexOf(startMarker)
        if (startIndex == -1) return ""
        
        val contentStartIndex = startIndex + startMarker.length
        val endIndex = htmlContent.indexOf(endMarker, contentStartIndex)
        if (endIndex == -1) return htmlContent.substring(contentStartIndex)
        
        val extractedContent = htmlContent.substring(contentStartIndex, endIndex)
        
        // Remove script tags and their content
        return removeScriptTags(extractedContent)
    }
    
    /**
     * Extract the ahl_daf_mila_ajax JavaScript variable from HTML content
     * 
     * @param htmlContent The HTML content containing the JavaScript variable
     * @return The parsed AhlDafMilaAjax object, or null if not found or parsing failed
     */
    fun extractAhlDafMilaAjax(htmlContent: String): AhlDafMilaAjax? {
        try {
            // Look for the JavaScript variable declaration
            val variablePattern = Regex(
                """var\s+ahl_daf_mila_ajax\s*=\s*(\{[^}]+\});?""",
                RegexOption.DOT_MATCHES_ALL
            )
            
            val matchResult = variablePattern.find(htmlContent)
                ?: return null
            
            // Extract the JavaScript object part
            val jsObject = matchResult.groupValues[1]
            
            // Convert JavaScript object syntax to valid JSON
            val jsonString = convertJsObjectToJson(jsObject)
            
            // Parse the JSON into our data class
            return gson.fromJson(jsonString, AhlDafMilaAjax::class.java)
            
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Build the keyword detail page URL with proper encoding
     * 
     * @param keyword The Hebrew keyword to encode
     * @return The complete URL for the keyword detail page
     */
    fun buildKeywordDetailUrl(keyword: String): String {
        val encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString())
        val dafMila = URLEncoder.encode("דף-מילה", StandardCharsets.UTF_8.toString())
        return "https://hebrew-academy.org.il/$encodedKeyword/$dafMila/"
    }
    
    /**
     * Build the AJAX URL for fetching detailed word data
     * 
     * @param ajaxConfig The AJAX configuration extracted from the HTML
     * @param searchString The keyword to search for
     * @return The complete AJAX URL with query parameters
     */
    fun buildAjaxUrl(ajaxConfig: AhlDafMilaAjax, searchString: String): String {
        val encodedSearchString = URLEncoder.encode(searchString, StandardCharsets.UTF_8.toString())
        return "${ajaxConfig.ajaxUrl}?action=get_mila_data&nonce=${ajaxConfig.nonce}&search_string=$encodedSearchString"
    }
    
    /**
     * Convert JavaScript object syntax to valid JSON
     * Adds quotes around unquoted keys and handles single quotes
     * 
     * @param jsObject The JavaScript object string
     * @return Valid JSON string
     */
    private fun convertJsObjectToJson(jsObject: String): String {
        return jsObject
            // Replace single quotes with double quotes
            .replace("'", "\"")
            // Add quotes around unquoted keys (word characters followed by colon)
            .replace(Regex("""(\w+):"""), "\"$1\":")
            // Clean up any extra whitespace
            .trim()
    }
    
    /**
     * Remove script tags and their content from HTML
     * 
     * @param htmlContent The HTML content to clean
     * @return The cleaned HTML content
     */
    private fun removeScriptTags(htmlContent: String): String {
        val scriptPattern = Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL)
        return htmlContent.replace(scriptPattern, "")
    }
} 