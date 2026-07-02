package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isUrl: Boolean = false,
    val url: String? = null,
    val status: String = "SUCCESS" // "PENDING", "SUCCESS", "ERROR"
)
