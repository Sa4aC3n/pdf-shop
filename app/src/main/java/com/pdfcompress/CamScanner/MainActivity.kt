package com.pdfcompress.CamScanner

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
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
import androidx.compose.material3.Button
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
import androidx.fragment.app.FragmentActivity
import com.pdfcompress.CamScanner.core.ui.theme.PDFShopTheme
import com.pdfcompress.CamScanner.domain.model.AppLanguage
import com.pdfcompress.CamScanner.domain.model.AppTheme
import com.pdfcompress.CamScanner.presentation.navigation.AppNavHost

class MainActivity : FragmentActivity() {

    private var isAuthenticated by mutableStateOf(false)
    private var authError by mutableStateOf<String?>(null)

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

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                PDFShopTheme(darkTheme = isDark) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        if (requiresAuth) {
                            LockScreen(
                                errorMessage = authError,
                                onAuthenticateClick = { showBiometricPrompt() }
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
        val biometricManager = androidx.biometric.BiometricManager.from(this)
        val authenticators = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or
            androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val canAuth = biometricManager.canAuthenticate(authenticators)
        if (canAuth != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
            isAuthenticated = true
            authError = null
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isAuthenticated = true
                    authError = null
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                        errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                        errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL
                    ) {
                        isAuthenticated = true
                        authError = null
                    } else {
                        authError = errString.toString()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    authError = "فشل التحقق من البصمة، يرجى المحاولة مرة أخرى"
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("قفل التطبيق")
            .setSubtitle("يرجى التحقق من بصمتك أو رمز قفل الجهاز للمتابعة")
            .setAllowedAuthenticators(authenticators)
            .build()

        biometricPrompt.authenticate(promptInfo)
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
            Button(onClick = onAuthenticateClick) {
                Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("إلغاء القفل بالبصمة")
            }
        }
    }
}
