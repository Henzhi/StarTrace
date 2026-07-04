package com.startrace.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.startrace.design.theme.StarColors

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.clearError() }

    // 监听登录成功事件
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is AuthEvent.LoginSuccess) {
                onLoginSuccess()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF0A0A0F), Color(0xFF0D0D1A), Color(0xFF1A1A3E)))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = StarColors.OnSurface)
                }
            }

            Spacer(Modifier.height(32.dp))
            Text("欢迎回来", style = MaterialTheme.typography.headlineLarge, color = StarColors.OnBackground, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("登录以继续你的星辰创作之旅", style = MaterialTheme.typography.bodyMedium, color = StarColors.OnSurface.copy(alpha = 0.5f))

            Spacer(Modifier.height(40.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it; viewModel.clearError() },
                label = { Text("用户名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = starTextFieldColors(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = StarColors.OnSurface)
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; viewModel.clearError() },
                label = { Text("密码") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "切换密码可见", tint = StarColors.OnSurface.copy(alpha = 0.5f))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                modifier = Modifier.fillMaxWidth(),
                colors = starTextFieldColors(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = StarColors.OnSurface)
            )

            if (uiState.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(uiState.error!!, color = StarColors.Error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.login(username.trim(), password)
                },
                enabled = username.isNotBlank() && password.isNotBlank() && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StarColors.Primary),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = StarColors.OnPrimary)
                } else {
                    Text("登录", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onNavigateToRegister) {
                Text("没有账号？", style = MaterialTheme.typography.bodySmall, color = StarColors.OnSurface.copy(alpha = 0.5f))
                Text(" 立即注册", style = MaterialTheme.typography.bodySmall, color = StarColors.Primary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
internal fun starTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = StarColors.Primary,
    unfocusedBorderColor = StarColors.SurfaceVariant,
    focusedLabelColor = StarColors.Primary,
    unfocusedLabelColor = StarColors.OnSurface.copy(alpha = 0.5f),
    cursorColor = StarColors.Primary
)
