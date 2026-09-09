package com.oneforth.cousininthecity.data.repository

import com.oneforth.cousininthecity.data.local.UserPreferencesDataSource
import com.oneforth.cousininthecity.data.mapper.toDomain
import com.oneforth.cousininthecity.data.remote.CousinApi
import com.oneforth.cousininthecity.domain.model.AppUser
import com.oneforth.cousininthecity.domain.repository.UserRepository
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val api: CousinApi,
    private val localDataSource: UserPreferencesDataSource
) : UserRepository {

    override suspend fun getOrCreateDeviceId(): String {
        return localDataSource.getOrCreateDeviceId()
    }

    override suspend fun registerUser(deviceId: String): Result<AppUser> {
        return runCatching {
            api.registerUser(deviceId).toDomain()
        }
    }
}
