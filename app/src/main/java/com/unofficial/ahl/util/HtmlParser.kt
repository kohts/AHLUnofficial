package com.unofficial.ahl.util

/**
 * Utility class for parsing HTML content
 */
object HtmlParser {
    
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