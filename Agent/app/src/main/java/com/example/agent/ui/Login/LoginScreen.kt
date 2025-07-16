package com.example.agent.ui.Login


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.agent.viewmodel.Login.LoginViewModel


@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = LoginViewModel(),
    onLoginSuccess: () -> Unit = {}
) {
    val userInfo by loginViewModel.userInfo.collectAsState()
    val loginResult by loginViewModel.loginResult.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "记账App", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "数据有温度！你的专属记账管家～", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = userInfo.emailOrPhone,
            onValueChange = loginViewModel::onEmailOrPhoneChange,
            label = { Text("邮箱或手机号") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = userInfo.password,
            onValueChange = loginViewModel::onPasswordChange,
            label = { Text("密码") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "忘记密码？",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { /* TODO: 添加忘记密码跳转 */ }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                loginViewModel.login()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("登录")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "没有账号？")
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "注册",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    // TODO: 添加注册跳转
                }
            )
        }

        if (loginResult != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = loginResult!!,
                color = if (loginResult == "登录成功") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )

            if (loginResult == "登录成功") {
                LaunchedEffect(Unit) {
                    onLoginSuccess()
                    loginViewModel.clearResult()
                }
            }
        }
    }
}



