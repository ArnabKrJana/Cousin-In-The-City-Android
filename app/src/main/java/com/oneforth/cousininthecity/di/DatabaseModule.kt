package com.oneforth.cousininthecity.di

import android.app.Application
import androidx.room.Room
import com.oneforth.cousininthecity.data.local.CousinDatabase
import com.oneforth.cousininthecity.data.local.dao.MessageDao
import com.oneforth.cousininthecity.data.local.dao.ThreadDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(app: Application): CousinDatabase {
        return Room.databaseBuilder(
            app,
            CousinDatabase::class.java,
            "cousin_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideThreadDao(db: CousinDatabase): ThreadDao {
        return db.threadDao
    }

    @Provides
    @Singleton
    fun provideMessageDao(db: CousinDatabase): MessageDao {
        return db.messageDao
    }
}

