package com.oneforth.cousininthecity.di

import com.oneforth.cousininthecity.data.repository.ChatRepositoryImpl
import com.oneforth.cousininthecity.data.repository.UserRepositoryImpl
import com.oneforth.cousininthecity.domain.repository.ChatRepository
import com.oneforth.cousininthecity.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository
}
