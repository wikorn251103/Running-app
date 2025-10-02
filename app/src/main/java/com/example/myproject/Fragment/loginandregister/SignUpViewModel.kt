package com.example.myproject.Fragment.loginandregister

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myproject.data.signup.UserModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SignUpViewModel(
    private val signUpAdapter: SignUpAdapter = SignUpAdapter()
) : ViewModel() {

    private val _signUpState = MutableStateFlow<SignUpState>(SignUpState.Idle)
    val signUpState: StateFlow<SignUpState> get() = _signUpState

    fun signUp(user: UserModel, password: String) {
        viewModelScope.launch {
            _signUpState.value = SignUpState.Loading
            val result = signUpAdapter.signUpUser(
                name = user.name,
                email = user.email,
                password = password,
                height = user.height,
                weight = user.weight,
                age = user.age,
                gender = user.gender
            )
            _signUpState.value = if (result.isSuccess) {
                SignUpState.Success(user)
            } else {
                SignUpState.Error(result.exceptionOrNull()?.message ?: "Unknown Error")
            }
        }
    }
}

sealed class SignUpState {
    object Idle : SignUpState()
    object Loading : SignUpState()
    data class Success(val user: UserModel) : SignUpState()
    data class Error(val message: String) : SignUpState()
}