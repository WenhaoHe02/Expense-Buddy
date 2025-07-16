package com.example.agent.viewmodel.Login


import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.agent.model.user.UserInformation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {



    private val _userInfo = MutableStateFlow(UserInformation())
    val userInfo: StateFlow<UserInformation> = _userInfo

    private val _loginResult = MutableStateFlow<String?>(null)
    val loginResult: StateFlow<String?> = _loginResult
    @Composable
    fun LoginViewModelFactory(): LoginViewModel {
        return androidx.lifecycle.viewmodel.compose.viewModel<LoginViewModel>()
    }

    fun onEmailOrPhoneChange(value: String) {
        _userInfo.value = _userInfo.value.copy(emailOrPhone = value)
    }

    fun onPasswordChange(value: String) {
        _userInfo.value = _userInfo.value.copy(password = value)
    }

    fun login() {
        viewModelScope.launch {
            if (_userInfo.value.emailOrPhone.isBlank() || _userInfo.value.password.isBlank()) {
                _loginResult.value = "请输入账号和密码"
            } else if (_userInfo.value.password.length < 6) {
                _loginResult.value = "密码长度至少为6位"
            } else {
                // 模拟成功登录
                _loginResult.value = "登录成功"
            }
        }
    }

    fun clearResult() {
        _loginResult.value = null
    }
}
