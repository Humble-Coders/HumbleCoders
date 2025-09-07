package com.humblecoders.humblecoders

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.humblecoders.humblecoders.ui.theme.HumbleCodersTheme
import com.razorpay.PaymentResultListener

class MainActivity : ComponentActivity(), PaymentResultListener {

    private lateinit var authRepository: AuthRepository
    private lateinit var authViewModel: AuthViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var paymentViewModel: PaymentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize repository and viewmodel
        authRepository = AuthRepository()
        userViewModel = UserViewModel()
        paymentViewModel = PaymentViewModel()
        authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(authRepository, userViewModel)
        )[AuthViewModel::class.java]

        setContent {
            HumbleCodersTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Check authentication status on app start
                    LaunchedEffect(Unit) {
                        authViewModel.checkAuthStatus()
                    }
                    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) {  innerPadding ->
                        AppNavigation(
                            navController = navController,
                            authViewModel = authViewModel,
                            userViewModel = userViewModel,
                            paymentViewModel = paymentViewModel,
                            context = this@MainActivity
                        )
                    }
                }
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        Log.d("MainActivity", "Payment successful: $razorpayPaymentId")
        paymentViewModel.handlePaymentSuccess(razorpayPaymentId ?: "")
    }

    override fun onPaymentError(code: Int, response: String?) {
        Log.e("MainActivity", "Payment failed: $code - $response")
        paymentViewModel.handlePaymentError("Payment failed: $response")
    }
}



class AuthViewModelFactory(
    private val repository: AuthRepository,
    private val userViewModel: UserViewModel
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(repository, userViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

