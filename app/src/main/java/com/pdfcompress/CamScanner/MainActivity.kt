package com.pdfcompress.CamScanner

import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.app.KeyguardManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pdfcompress.CamScanner.core.ui.components.ModernGradientButton
import com.pdfcompress.CamScanner.core.ui.theme.PDFShopTheme
import com.pdfcompress.CamScanner.domain.model.AppLanguage
import com.pdfcompress.CamScanner.domain.model.AppTheme
import com.pdfcompress.CamScanner.presentation.navigation.AppNavHost

class MainActivity : ComponentActivity() {

    private var isAuthenticated by mutableStateOf(false)
    private var authError by mutableStateOf<String?>(null)

    override fun onStop() {
        super.onStop()
        // Re-lock the application session when it leaves the foreground
        isAuthenticated = false
        authError = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PdfShopApplication
        val container = app.container

        setContent {
            val settings by container.settingsRepository.settings.collectAsState(
                initial = com.pdfcompress.CamScanner.domain.model.UserSettings()
            )

            val isDark = when (settings.theme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }

            val layoutDirection = when (settings.language) {
                AppLanguage.ARABIC -> LayoutDirection.Rtl
                AppLanguage.ENGLISH -> LayoutDirection.Ltr
            }

            val requiresAuth = settings.isAppLockEnabled && !isAuthenticated

            LaunchedEffect(settings.isAppLockEnabled) {
                if (settings.isAppLockEnabled && !isAuthenticated) {
                    showBiometricPrompt()
                }
            }

            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    }
                )
                onDispose {}
            }

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                PDFShopTheme(
                    darkTheme = isDark,
                    primaryColorLong = settings.colorTheme.primaryColorLong,
                    primaryContainerLightLong = settings.colorTheme.primaryContainerLightLong,
                    primaryContainerDarkLong = settings.colorTheme.primaryContainerDarkLong,
                    secondaryColorLong = settings.colorTheme.secondaryColorLong
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        if (requiresAuth) {
                            LockScreen(
                                errorMessage = authError,
                                onAuthenticateClick = { showBiometricPrompt() },
                                modifier = Modifier.safeDrawingPadding()
                            )
                        } else {
                            AppNavHost(container = container)
                        }
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val keyguardManager = getSystemService(KeyguardManager::class.java)
            if (keyguardManager == null || !keyguardManager.isDeviceSecure) {
                isAuthenticated = true
                authError = null
                return
            }

            val executor = ContextCompat.getMainExecutor(this)
            val promptBuilder = android.hardware.biometrics.BiometricPrompt.Builder(this)
                .setTitle("قفل التطبيق")
                .setSubtitle("يرجى التحقق من بصمتك أو رمز قفل الجهاز للمتابعة")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                promptBuilder.setAllowedAuthenticators(
                    android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            } else {
                promptBuilder.setNegativeButton("إلغاء", executor) { _, _ ->
                    authError = "تم إلغاء التحقق"
                }
            }

            val prompt = promptBuilder.build()
            val cancellationSignal = CancellationSignal()

            prompt.authenticate(
                cancellationSignal,
                executor,
                object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        isAuthenticated = true
                        authError = null
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        if (errorCode == android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS ||
                            errorCode == android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT
                        ) {
                            isAuthenticated = true
                            authError = null
                        } else {
                            authError = errString?.toString()
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        authError = "فشل التحقق من البصمة، يرجى المحاولة مرة أخرى"
                    }
                }
            )
        } else {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (keyguardManager == null || !keyguardManager.isKeyguardSecure) {
                isAuthenticated = true
                authError = null
            } else {
                isAuthenticated = true
                authError = null
            }
        }
    }
}

@Composable
fun LockScreen(
    errorMessage: String?,
    onAuthenticateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "التطبيق مقفل لحماية بياناتك",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "يرجى تأكيد بصمة الإصبع أو القياسات الحيوية للوصول إلى أدوات ومستندات التطبيق.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            ModernGradientButton(
                text = "إلغاء القفل بالبصمة",
                icon = Icons.Default.Fingerprint,
                onClick = onAuthenticateClick
            )
        }
    }
}
