package com.example.myproject.data.signup

import kotlinx.coroutines.flow.Flow

interface SignUpRepository {
    fun getSignUpFlow(): Flow<List<UserModel>>
}

class SignUpRepositoryImpl(
    private val SignUpService: SignUpService
) : SignUpRepository {
    override fun getSignUpFlow(): Flow<List<UserModel>> {
        return SignUpService.getSignUpFlow()
    }
}