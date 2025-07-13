package com.unofficial.ahl.repository

import org.junit.Test
import org.junit.Assert.*
import com.google.gson.Gson
import com.unofficial.ahl.model.DafMilaResponse
import java.io.File

/**
 * Unit test for HebrewWordsRepository to test JSON parsing scenarios
 * Uses data-driven testing with JSON files from test resources
 */
class HebrewWordsRepositoryTest {
    
    private val gson = Gson()
    private val testResourcesPath = "json/daf_mila_responses"
    
    /**
     * Test that all JSON files in the test resources directory can be parsed successfully
     * This is a data-driven test that automatically tests any JSON file you add to the resources
     */
    @Test
    fun testParseDafMilaJsonFromAllFiles() {
        val jsonFiles = loadAllJsonTestFiles()
        
        assertTrue("No JSON test files found in resources", jsonFiles.isNotEmpty())
        
        jsonFiles.forEach { (fileName, jsonContent) ->
            val result = parseDafMilaJson(jsonContent)
            
            // Every file should parse successfully
            assertNotNull("Failed to parse JSON from file: $fileName", result)
            
            // Basic validation - every response should have a non-null koteret
            assertNotNull("Koteret should not be null in file: $fileName", result?.koteret)
            
            println("✓ Successfully parsed: $fileName - Koteret: ${result?.koteret}")
        }
    }
    
    /**
     * Test specific validations for the complex MunnahimList case
     */
    @Test
    fun testComplexMunnahimListSpecificValidations() {
        val jsonContent = loadJsonFile("complex_munnahim_list.json")
        val result = parseDafMilaJson(jsonContent)
        
        assertNotNull("Complex MunnahimList should parse successfully", result)
        
        // Test Hebrew content parsing
        assertNotNull("Koteret should contain Hebrew text", result?.koteret)
        assertTrue("Koteret should contain Hebrew unicode characters, but found: " + result?.koteret?.toString(),
                   result?.koteret?.contains('\u05E0') == true) // nun
        
        // Test MunnahimList structure
        assertNotNull("MunnahimList should be parsed correctly", result?.munnahimList)
        assertEquals("https://terms.hebrew-academy.org.il/munnah?kodErekhIvrit=14621", 
                     result?.munnahimList?.reshimaMelleha)
        assertNotNull("KoteretMaagarMunnahim should be present", 
                      result?.munnahimList?.koteretMaagarMunnahim)
        
        // Test nested structures
        assertNotNull("KtaimHtml should be present", result?.munnahimList?.ktaimHtml)
        assertTrue("KtaimHtml should not be empty", 
                   result?.munnahimList?.ktaimHtml?.isNotEmpty() == true)
        
        // Test deeply nested structures
        val firstKeta = result?.munnahimList?.ktaimHtml?.firstOrNull()
        assertNotNull("First KetaHtml should be present", firstKeta)
        assertNotNull("Halakim should be present", firstKeta?.halakim)
        
        val firstHelek = firstKeta?.halakim?.firstOrNull()
        assertNotNull("First Helek should be present", firstHelek)
        assertNotNull("NirdafLink should be present", firstHelek?.nirdafLink)
        assertEquals("Hebrew", firstHelek?.nirdafLink?.safa)
    }
    
    /**
     * Test that simple responses work correctly
     */
    @Test
    fun testSimpleResponseValidations() {
        val jsonContent = loadJsonFile("simple_response.json")
        val result = parseDafMilaJson(jsonContent)
        
        assertNotNull("Simple response should parse successfully", result)
        assertEquals("Test Word", result?.koteret)
        
        // Test MunnahimList
        assertNotNull("MunnahimList should be parsed", result?.munnahimList)
        assertEquals("https://example.com", result?.munnahimList?.reshimaMelleha)
        assertEquals("Test", result?.munnahimList?.koteretMaagarMunnahim)
        assertEquals("test-info", result?.munnahimList?.info)
        
        // Test that empty lists are handled correctly
        assertTrue("KtaimHtml should be empty", result?.munnahimList?.ktaimHtml?.isEmpty() == true)
    }
    
    /**
     * Test that null MunnahimList is handled correctly
     */
    @Test
    fun testNullMunnahimListHandling() {
        val jsonContent = loadJsonFile("null_munnahim_list.json")
        val result = parseDafMilaJson(jsonContent)
        
        assertNotNull("Response with null MunnahimList should parse successfully", result)
        assertEquals("Test Word", result?.koteret)
        assertNull("MunnahimList should be null", result?.munnahimList)
    }
    
    /**
     * Load all JSON files from the test resources directory
     * @return Map of filename to JSON content
     */
    private fun loadAllJsonTestFiles(): Map<String, String> {
        val resourceUrl = javaClass.classLoader.getResource(testResourcesPath)
            ?: throw IllegalStateException("Test resources directory not found: $testResourcesPath")
        
        val resourcesDir = File(resourceUrl.toURI())
        val jsonFiles = resourcesDir.listFiles { file -> file.name.endsWith(".json") }
            ?: throw IllegalStateException("No JSON files found in: $testResourcesPath")
        
        return jsonFiles.associate { file ->
            file.name to file.readText()
        }
    }
    
    /**
     * Load a specific JSON file from test resources
     * @param fileName The name of the JSON file to load
     * @return The JSON content as a string
     */
    private fun loadJsonFile(fileName: String): String {
        val resourcePath = "$testResourcesPath/$fileName"
        return javaClass.classLoader.getResource(resourcePath)?.readText()
            ?: throw IllegalArgumentException("JSON file not found: $resourcePath")
    }
    
    /**
     * Parse JSON string into DafMilaResponse object
     * @param json The JSON string to parse
     * @return Parsed DafMilaResponse or null if parsing fails
     */
    private fun parseDafMilaJson(json: String): DafMilaResponse? {
        return try {
            gson.fromJson(json, DafMilaResponse::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    companion object {
        /**
         * Static method to test JSON parsing from files without running JUnit
         * Useful for debugging and manual testing
         */
        @JvmStatic
        fun debugJsonParsingFromFiles(): String {
            val test = HebrewWordsRepositoryTest()
            val results = mutableListOf<String>()
            
            return try {
                val jsonFiles = test.loadAllJsonTestFiles()
                
                jsonFiles.forEach { (fileName, jsonContent) ->
                    val result = test.parseDafMilaJson(jsonContent)
                    if (result != null) {
                        results.add("✓ $fileName: SUCCESS - Koteret: ${result.koteret}")
                    } else {
                        results.add("✗ $fileName: FAILED to parse")
                    }
                }
                
                "JSON Parsing Test Results:\n" + results.joinToString("\n")
            } catch (e: Exception) {
                "ERROR: ${e.javaClass.simpleName}: ${e.message}"
            }
        }
    }
} 