package com.oneforth.cousininthecity.domain.repository

import com.oneforth.cousininthecity.domain.model.AppUser

interface UserRepository {
    suspend fun getOrCreateDeviceId(): String
    suspend fun registerUser(deviceId: String): Result<AppUser>
}
