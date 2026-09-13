package com.oneforth.cousininthecity.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_threads")
data class ThreadEntity(
    @PrimaryKey val id: String,
    val title: String,
    val deviceId: String,
    val isPinned: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

