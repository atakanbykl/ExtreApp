package com.example.extreapp.domain.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.extreapp.data.local.entity.BiometricAvailability

object BiometricAuthManager {

    private const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun checkAvailability(context: Context): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            else -> BiometricAvailability.UNAVAILABLE
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "ExtreApp Kimlik Doğrulama",
        subtitle: String = "Hassas finansal verilerinizi korumak için doğrulayın",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: String) -> Unit = { _, _ -> }
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Biometric rejected, prompt remains active
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()

        prompt.authenticate(promptInfo)
    }
}

