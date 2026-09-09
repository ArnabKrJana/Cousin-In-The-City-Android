package com.oneforth.cousininthecity.domain.usecase

import com.oneforth.cousininthecity.domain.model.AppUser
import com.oneforth.cousininthecity.domain.repository.UserRepository
import javax.inject.Inject

class RegisterUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Result<AppUser> {
        val deviceId = userRepository.getOrCreateDeviceId()
        return userRepository.registerUser(deviceId)
    }

    suspend fun getDeviceId(): String {
        return userRepository.getOrCreateDeviceId()
    }
}
