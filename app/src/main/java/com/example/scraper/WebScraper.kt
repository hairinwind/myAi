package com.example.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException

object WebScraper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun fetchAndExtractText(urlString: String): ScrapedResult = withContext(Dispatchers.IO) {
        var cleanUrl = urlString.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }

        try {
            val request = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ScrapedResult.Error("HTTP 状态错误: ${response.code} ${response.message}")
                }

                val html = response.body?.string() ?: return@withContext ScrapedResult.Error("响应内容为空")
                val doc = Jsoup.parse(html, cleanUrl)
                val title = doc.title().trim()

                // Extract main container if present
                val mainElements = doc.select("article, main, .post, .entry, .content, #content, .article")
                val elementsToExtract = if (mainElements.isNotEmpty()) {
                    mainElements.select("h1, h2, h3, h4, p, li")
                } else {
                    doc.select("h1, h2, h3, h4, p, li")
                }

                val textBuilder = StringBuilder()
                for (elem in elementsToExtract) {
                    val text = elem.text().trim()
                    if (text.isNotEmpty() && text.length > 5) {
                        textBuilder.append(text).append("\n\n")
                    }
                }

                var extractedText = textBuilder.toString().trim()
                if (extractedText.isEmpty()) {
                    extractedText = doc.body().text().trim()
                }

                val maxChars = 20000
                if (extractedText.length > maxChars) {
                    extractedText = extractedText.substring(0, maxChars) + "\n\n[网页正文内容过长，已被自动截断...]"
                }

                if (extractedText.isEmpty()) {
                    ScrapedResult.Error("未能提取到有效的网页文本内容")
                } else {
                    ScrapedResult.Success(title, extractedText, cleanUrl)
                }
            }
        } catch (e: IOException) {
            ScrapedResult.Error("网络连接失败: ${e.localizedMessage ?: "请检查网址或网络连接"}")
        } catch (e: Exception) {
            ScrapedResult.Error("网页解析失败: ${e.localizedMessage ?: "未知错误"}")
        }
    }
}

sealed class ScrapedResult {
    data class Success(val title: String, val text: String, val url: String) : ScrapedResult()
    data class Error(val errorMessage: String) : ScrapedResult()
}
