package com.lucidera.investigations

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucidera.investigations.data.AppContainer
import com.lucidera.investigations.ui.LucidEraApp
import com.lucidera.investigations.ui.screens.LockScreen
import com.lucidera.investigations.ui.theme.LucidEraTheme
import com.lucidera.investigations.ui.viewmodel.AppLockViewModel

class MainActivity : FragmentActivity() {

    private val container by lazy { AppContainer(applicationContext) }
    private val lockViewModel: AppLockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LucidEraTheme {
                val isUnlocked by lockViewModel.isUnlocked.collectAsStateWithLifecycle()
                if (isUnlocked) {
                    LucidEraApp(container = container)
                } else {
                    LockScreen(onAuthRequest = { showBiometricPrompt() })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lockViewModel.onForeground()
    }

    override fun onPause() {
        super.onPause()
        lockViewModel.onBackground()
    }

    private fun showBiometricPrompt() {
        val allowedAuthenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val canAuth = BiometricManager.from(this).canAuthenticate(allowedAuthenticators)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            lockViewModel.unlock()
            return
        }

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    lockViewModel.unlock()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL) {
                        lockViewModel.unlock()
                    }
                    // Other errors (user cancel, etc.) leave the lock screen visible
                    // so the user can tap Unlock to try again.
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Field Notes")
            .setSubtitle("Your investigation data is protected")
            .setAllowedAuthenticators(allowedAuthenticators)
            .build()

        prompt.authenticate(promptInfo)
    }
}
