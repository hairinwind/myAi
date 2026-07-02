package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.data.AppDatabase
import com.example.data.MessageEntity
import com.example.data.MessageRepository
import com.example.scraper.ScrapedResult
import com.example.scraper.WebScraper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MessageRepository
    val messages: StateFlow<List<MessageEntity>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MessageRepository(database.messageDao())
        messages = repository.allMessages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    private fun isUrl(text: String): Boolean {
        val clean = text.trim()
        if (clean.startsWith("http://", ignoreCase = true) || clean.startsWith("https://", ignoreCase = true)) {
            return true
        }
        // Match standard domain style (e.g., google.com, baidu.com, github.com/trending)
        val urlPattern = "^(www\\.)?[a-zA-Z0-9-]+\\.[a-zA-Z]{2,}(/\\S*)?$".toRegex()
        return urlPattern.matches(clean)
    }

    fun sendMessage(inputText: String) {
        val text = inputText.trim()
        if (text.isEmpty()) return

        val isInputUrl = isUrl(text)
        val formattedUrl = if (isInputUrl && !text.startsWith("http://", ignoreCase = true) && !text.startsWith("https://", ignoreCase = true)) {
            "https://$text"
        } else {
            text
        }

        viewModelScope.launch {
            // Check for API key presence
            val apiKey = BuildConfig.GEMINI_API_KEY
            val isApiKeyMissing = apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY"

            if (isInputUrl) {
                // 1. Insert user message representing the URL
                val userMsg = MessageEntity(
                    text = text,
                    isUser = true,
                    isUrl = true,
                    url = formattedUrl
                )
                repository.insertMessage(userMsg)

                // 2. Insert AI pending message
                val aiPendingMsg = MessageEntity(
                    text = "正在获取网页内容...",
                    isUser = false,
                    isUrl = true,
                    url = formattedUrl,
                    status = "PENDING"
                )
                val aiPendingId = repository.insertMessage(aiPendingMsg)

                if (isApiKeyMissing) {
                    val errorMsg = aiPendingMsg.copy(
                        id = aiPendingId,
                        text = "API Key 缺失。请在 AI Studio 左侧的 Secrets 面板中配置您的 GEMINI_API_KEY 以开始使用网页翻译功能。",
                        status = "ERROR"
                    )
                    repository.updateMessage(errorMsg)
                    return@launch
                }

                // 3. Perform web scraping in background
                val scrapeResult = WebScraper.fetchAndExtractText(formattedUrl)
                when (scrapeResult) {
                    is ScrapedResult.Success -> {
                        val updateMsg = aiPendingMsg.copy(
                            id = aiPendingId,
                            text = "网页获取成功！正在通过 Gemini 过滤广告并翻译正文 (《${scrapeResult.title}》)...",
                            status = "PENDING"
                        )
                        repository.updateMessage(updateMsg)

                        // 4. Request translation and ad filtering from Gemini
                        try {
                            val systemPrompt = "You are a web page clean reader and translator. Below is the title and text extracted from a web page. Your task: 1) Filter out any advertisement noise, sidebar lists, navigation menus, login boxes, cookie banners, or promotional content. 2) Extract the main article/content. 3) Translate this main content into high-quality, fluent, and professional Chinese (简体中文). 4) Organize the output beautifully using standard Markdown, starting with a bold title like '# [Title]' and then clear paragraphs or bullet points."
                            val userPrompt = """
                                Web Page Title: ${scrapeResult.title}
                                Web Page URL: ${scrapeResult.url}
                                
                                Extracted Content:
                                ${scrapeResult.text}
                            """.trimIndent()

                            val request = GenerateContentRequest(
                                contents = listOf(Content(parts = listOf(Part(text = userPrompt)))),
                                systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                            )

                            val response = RetrofitClient.service.generateContent(apiKey, request)
                            val translatedText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                            if (translatedText != null) {
                                val successMsg = aiPendingMsg.copy(
                                    id = aiPendingId,
                                    text = translatedText,
                                    status = "SUCCESS"
                                )
                                repository.updateMessage(successMsg)
                            } else {
                                val errorMsg = aiPendingMsg.copy(
                                    id = aiPendingId,
                                    text = "翻译失败：Gemini 返回了空内容，可能是由于内容被安全过滤器拦截或字数过多。",
                                    status = "ERROR"
                                )
                                repository.updateMessage(errorMsg)
                            }
                        } catch (e: Exception) {
                            val errorMsg = aiPendingMsg.copy(
                                id = aiPendingId,
                                text = "Gemini 翻译失败: ${e.localizedMessage ?: "未知错误，请检查 API Key 或重试"}",
                                status = "ERROR"
                            )
                            repository.updateMessage(errorMsg)
                        }
                    }
                    is ScrapedResult.Error -> {
                        val errorMsg = aiPendingMsg.copy(
                            id = aiPendingId,
                            text = "获取网页失败: ${scrapeResult.errorMessage}",
                            status = "ERROR"
                        )
                        repository.updateMessage(errorMsg)
                    }
                }
            } else {
                // Regular chatbot conversation
                val userMsg = MessageEntity(
                    text = text,
                    isUser = true,
                    isUrl = false
                )
                repository.insertMessage(userMsg)

                val aiPendingMsg = MessageEntity(
                    text = "思考中...",
                    isUser = false,
                    isUrl = false,
                    status = "PENDING"
                )
                val aiPendingId = repository.insertMessage(aiPendingMsg)

                if (isApiKeyMissing) {
                    val errorMsg = aiPendingMsg.copy(
                        id = aiPendingId,
                        text = "API Key 缺失。请在 AI Studio 左侧的 Secrets 面板中配置您的 GEMINI_API_KEY 以开始使用对话服务。",
                        status = "ERROR"
                    )
                    repository.updateMessage(errorMsg)
                    return@launch
                }

                // Call Gemini using Chat History Context (max 6 turns to keep context tidy)
                try {
                    val chatHistory = messages.value.takeLast(6)
                    val contentsList = mutableListOf<Content>()
                    
                    for (msg in chatHistory) {
                        // Skip pending/error messages to avoid confusing context
                        if (msg.status == "PENDING" || msg.status == "ERROR") continue
                        
                        val roleText = if (msg.isUser) "user" else "model"
                        // Since Gemini 3.5 REST expects contents with text
                        contentsList.add(Content(parts = listOf(Part(text = msg.text))))
                    }
                    
                    // If history is empty or last item is user, ensure our new message is there
                    if (contentsList.isEmpty() || chatHistory.lastOrNull()?.isUser != true) {
                        contentsList.add(Content(parts = listOf(Part(text = text))))
                    }

                    val systemPrompt = "You are a polite, helpful Chinese AI assistant. Answer the user's questions in clear and fluent Chinese (简体中文). Use rich markdown where appropriate."

                    val request = GenerateContentRequest(
                        contents = contentsList,
                        systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                    )

                    val response = RetrofitClient.service.generateContent(apiKey, request)
                    val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                    if (responseText != null) {
                        val successMsg = aiPendingMsg.copy(
                            id = aiPendingId,
                            text = responseText,
                            status = "SUCCESS"
                        )
                        repository.updateMessage(successMsg)
                    } else {
                        val errorMsg = aiPendingMsg.copy(
                            id = aiPendingId,
                            text = "思考失败：无法获取模型回复。",
                            status = "ERROR"
                        )
                        repository.updateMessage(errorMsg)
                    }
                } catch (e: Exception) {
                    val errorMsg = aiPendingMsg.copy(
                        id = aiPendingId,
                        text = "思考失败: ${e.localizedMessage ?: "网络错误，请稍后重试"}",
                        status = "ERROR"
                    )
                    repository.updateMessage(errorMsg)
                }
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChat()
        }
    }
}
