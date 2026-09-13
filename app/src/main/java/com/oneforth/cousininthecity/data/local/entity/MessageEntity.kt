package com.oneforth.cousininthecity.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ThreadEntity::class,
            parentColumns = ["id"],
            childColumns = ["threadId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("threadId")]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val threadId: String,
    val role: String,
    val content: String,
    val intentType: String? = null,
    val actionData: Map<String, String>? = null,
    val timestamp: Long = System.currentTimeMillis()
)

