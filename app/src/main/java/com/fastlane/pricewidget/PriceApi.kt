package com.fastlane.pricewidget

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object PriceApi {
    private const val API_URL = "https://fastlane.co.il/PageMethodsService.asmx/GetCurrentPrice"
    
    fun getCurrentPrice(): Int {
        var connection: HttpURLConnection? = null
        
        try {
            val url = URL(API_URL)
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                doInput = true
                connectTimeout = 10000
                readTimeout = 10000
            }
            
            // Send empty JSON body
            val writer = OutputStreamWriter(connection.outputStream)
            writer.write("{}")
            writer.flush()
            writer.close()
            
            // Read response
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                
                // Parse JSON response
                val jsonResponse = JSONObject(response.toString())
                
                // Try different possible JSON structures
                return when {
                    jsonResponse.has("d") -> {
                        // ASP.NET WebMethod format: {"d": "22"}
                        val d = jsonResponse.getString("d")
                        d.toIntOrNull() ?: parseFromString(d)
                    }
                    jsonResponse.has("price") -> {
                        jsonResponse.getInt("price")
                    }
                    jsonResponse.has("Price") -> {
                        jsonResponse.getInt("Price")
                    }
                    else -> {
                        // Try to extract any number from response
                        val numberRegex = "\\d+".toRegex()
                        val match = numberRegex.find(response.toString())
                        match?.value?.toInt() ?: throw Exception("Could not parse price from response")
                    }
                }
            } else {
                throw Exception("HTTP Error: $responseCode")
            }
        } catch (e: Exception) {
            throw Exception("Failed to fetch price: ${e.message}", e)
        } finally {
            connection?.disconnect()
        }
    }
    
    private fun parseFromString(str: String): Int {
        // Try to extract number from string like "22" or "Price: 22"
        val numberRegex = "\\d+".toRegex()
        val match = numberRegex.find(str)
        return match?.value?.toInt() ?: throw Exception("Could not parse price from: $str")
    }
}
