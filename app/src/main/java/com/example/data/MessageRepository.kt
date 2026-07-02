package com.example.data

import kotlinx.coroutines.flow.Flow

class MessageRepository(private val messageDao: MessageDao) {
    val allMessages: Flow<List<MessageEntity>> = messageDao.getAllMessages()

    suspend fun insertMessage(message: MessageEntity): Long {
        return messageDao.insertMessage(message)
    }

    suspend fun updateMessage(message: MessageEntity) {
        messageDao.updateMessage(message)
    }

    suspend fun clearChat() {
        messageDao.clearChat()
    }
}
