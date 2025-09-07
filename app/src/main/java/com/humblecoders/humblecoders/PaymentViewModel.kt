package com.humblecoders.humblecoders

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PaymentViewModel : ViewModel() {
    private val _isProcessingPayment = MutableStateFlow(false)
    val isProcessingPayment: StateFlow<Boolean> = _isProcessingPayment.asStateFlow()

    private val _paymentResult = MutableStateFlow<PaymentResult?>(null)
    val paymentResult: StateFlow<PaymentResult?> = _paymentResult.asStateFlow()

    fun startPayment(
        activity: Activity,
        courseDetails: CourseDetails,
        userEmail: String
    ) {
        viewModelScope.launch {
            Log.d("PaymentViewModel", "Starting payment process for course: ${courseDetails.title}")
            _isProcessingPayment.value = true
            _paymentResult.value = null

            try {
                RazorpayPaymentService.startPayment(
                    activity = activity,
                    amount = courseDetails.price,
                    courseTitle = courseDetails.title,
                    userEmail = userEmail
                )

                // Add timeout handling - if payment doesn't complete in 5 minutes, reset state
                viewModelScope.launch {
                    delay(300000) // 5 minutes timeout
                    if (_isProcessingPayment.value) {
                        Log.w("PaymentViewModel", "Payment timeout - resetting state")
                        _isProcessingPayment.value = false
                        _paymentResult.value = PaymentResult.Error("Payment timeout")
                    }
                }
            } catch (e: Exception) {
                Log.e("PaymentViewModel", "Exception in startPayment", e)
                _paymentResult.value = PaymentResult.Error(e.message ?: "Payment failed")
                _isProcessingPayment.value = false
            }
        }
    }

    fun clearPaymentResult() {
        _paymentResult.value = null
    }

    fun resetPaymentState() {
        _isProcessingPayment.value = false
        _paymentResult.value = null
        Log.d("PaymentViewModel", "Payment state reset")
    }

    fun handlePaymentSuccess(paymentId: String) {
        Log.d("PaymentViewModel", "Handling payment success: $paymentId")
        _paymentResult.value = PaymentResult.Success(paymentId)
        _isProcessingPayment.value = false
    }

    fun handlePaymentError(error: String) {
        Log.e("PaymentViewModel", "Handling payment error: $error")
        _paymentResult.value = PaymentResult.Error(error)
        _isProcessingPayment.value = false
    }
}

sealed class PaymentResult {
    data class Success(val paymentId: String) : PaymentResult()
    data class Error(val message: String) : PaymentResult()
}
