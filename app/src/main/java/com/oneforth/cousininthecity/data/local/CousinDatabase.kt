package com.oneforth.cousininthecity.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.oneforth.cousininthecity.data.local.dao.MessageDao
import com.oneforth.cousininthecity.data.local.dao.ThreadDao
import com.oneforth.cousininthecity.data.local.entity.MessageEntity
import com.oneforth.cousininthecity.data.local.entity.ThreadEntity

@Database(
    entities = [ThreadEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CousinDatabase : RoomDatabase() {
    abstract val threadDao: ThreadDao
    abstract val messageDao: MessageDao
}

