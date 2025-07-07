package com.unofficial.ahl.util

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.unofficial.ahl.model.AhlDafMilaAjax
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Exception thrown when HTML parsing fails, preserving context for debugging
 */
class HtmlParsingException(
    message: String,
    val htmlContent: String,
    val jsObject: String? = null,
    cause: Throwable? = null
) : Exception(message, cause) {
    
    override fun toString(): String {
        return buildString {
            appendLine(super.toString())
            appendLine("HTML Content length: ${htmlContent.length} characters")
            jsObject?.let {
                appendLine("Extracted JS Object: $it")
            }
            cause?.let {
                appendLine("Caused by: ${it.javaClass.simpleName}: ${it.message}")
            }
        }
    }
}

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
     * @return The parsed AhlDafMilaAjax object
     * @throws HtmlParsingException if the variable is not found or parsing fails
     */
    fun extractAhlDafMilaAjax(htmlContent: String): AhlDafMilaAjax {
        // Look for the JavaScript variable declaration
        val variablePattern = Regex(
            """var\s+ahl_daf_mila_ajax\s*=\s*(\{[^}]+\});?""",
            RegexOption.DOT_MATCHES_ALL
        )
        
        val matchResult = variablePattern.find(htmlContent)
            ?: throw HtmlParsingException(
                "ahl_daf_mila_ajax variable not found in HTML content",
                htmlContent
            )
        
        // Extract the JavaScript object part
        val jsObject = matchResult.groupValues[1]
        
        try {
            // Parse the JSON directly (it's already valid JSON)
            return gson.fromJson(jsObject, AhlDafMilaAjax::class.java)
                ?: throw HtmlParsingException(
                    "Failed to parse JSON into AhlDafMilaAjax object (gson returned null)",
                    htmlContent,
                    jsObject
                )
                
        } catch (e: JsonSyntaxException) {
            throw HtmlParsingException(
                "JSON syntax error while parsing ahl_daf_mila_ajax",
                htmlContent,
                jsObject,
                e
            )
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