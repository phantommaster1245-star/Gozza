package io.github.phantom.gps.ui

import org.junit.Assert.*
import org.junit.Test

class ActivationUiStateTest {
    @Test fun existingLicenseCheckHidesFormAndForbidsSubmission() {
        val checking = ActivationUiState(ActivationMode.CHECKING)
        assertTrue(checking.isBusy)
        assertFalse(checking.canSubmit)
        assertFalse(checking.showForm)
        assertFalse(checking.canRetryVerification)
    }
    @Test fun failedVerificationOffersOnlyVerificationRetry() {
        val failed = ActivationUiState(ActivationMode.CHECK_FAILED)
        assertTrue(failed.canRetryVerification)
        assertFalse(failed.canSubmit)
        assertFalse(failed.showForm)
        assertFalse(failed.isBusy)
    }
    @Test fun repeatedActivationTapIsRejectedUntilSubmissionFinishes() {
        assertTrue(ActivationUiState(ActivationMode.READY).canSubmit)
        val submitting = ActivationUiState(ActivationMode.SUBMITTING)
        assertTrue(submitting.isBusy)
        assertFalse(submitting.canSubmit)
        assertTrue(submitting.showForm)
        assertTrue(ActivationUiState(ActivationMode.ACTIVATION_FAILED).canSubmit)
        assertFalse(ActivationUiState(ActivationMode.OPEN_MAIN).canSubmit)
        assertFalse(ActivationUiState(ActivationMode.OPEN_LOCKED).canSubmit)
    }
}
