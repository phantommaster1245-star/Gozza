package io.github.phantom.gps.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.github.phantom.gps.R
import io.github.phantom.gps.utils.LicenseGuard
import io.github.phantom.gps.utils.LicenseStore

/** Keeps an in-flight operation across rotation; checking never submits an activation code. */
class ActivationViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application.applicationContext
    private val state = MutableLiveData(ActivationUiState(ActivationMode.CHECKING))
    val uiState: LiveData<ActivationUiState> = state

    init {
        when {
            LicenseStore.isLocked(app) -> state.value = ActivationUiState(ActivationMode.OPEN_LOCKED)
            LicenseStore.isActivated(app) && LicenseStore.hasLicenseData(app) -> verifyExisting()
            else -> {
                if (LicenseStore.isActivated(app)) LicenseGuard.clearLicenseAndDeactivate(app)
                state.value = ActivationUiState(ActivationMode.READY)
            }
        }
    }

    fun submit(code: String) {
        if (state.value?.canSubmit != true) return
        state.value = ActivationUiState(ActivationMode.SUBMITTING)
        LicenseGuard.activate(app, code) { result ->
            state.value = if (result.success) ActivationUiState(ActivationMode.OPEN_MAIN)
                else ActivationUiState(ActivationMode.ACTIVATION_FAILED, result.errorMessage ?: app.getString(R.string.activation_error_generic))
        }
    }

    fun retryVerification() {
        if (state.value?.canRetryVerification != true) return
        verifyExisting()
    }

    private fun verifyExisting() {
        state.value = ActivationUiState(ActivationMode.CHECKING)
        LicenseGuard.verifyNowForeground(app) { success ->
            state.value = when {
                success && !LicenseStore.isLocked(app) -> ActivationUiState(ActivationMode.OPEN_MAIN)
                LicenseStore.isLocked(app) -> ActivationUiState(ActivationMode.OPEN_LOCKED)
                else -> ActivationUiState(ActivationMode.CHECK_FAILED, app.getString(R.string.activation_error_network))
            }
        }
    }
}
