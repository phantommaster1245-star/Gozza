package io.github.phantom.gps.ui

enum class ActivationMode { CHECKING, CHECK_FAILED, READY, SUBMITTING, ACTIVATION_FAILED, OPEN_MAIN, OPEN_LOCKED }

data class ActivationUiState(val mode: ActivationMode, val error: String? = null) {
    val canSubmit: Boolean get() = mode == ActivationMode.READY || mode == ActivationMode.ACTIVATION_FAILED
    val showForm: Boolean get() = canSubmit || mode == ActivationMode.SUBMITTING
    val isBusy: Boolean get() = mode == ActivationMode.CHECKING || mode == ActivationMode.SUBMITTING
    val canRetryVerification: Boolean get() = mode == ActivationMode.CHECK_FAILED
}
