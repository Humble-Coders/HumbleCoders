package com.humblecoders.humblecoders

import android.app.Activity
import android.content.Context
import android.util.Log
import com.razorpay.Checkout
import org.json.JSONObject

object RazorpayPaymentService {

    private const val RAZORPAY_KEY_ID = ""

    fun startPayment(
        activity: Activity,
        amount: Int,
        courseTitle: String,
        userEmail: String
    ) {
        try {
            val checkout = Checkout()
            checkout.setKeyID(RAZORPAY_KEY_ID)

            val options = JSONObject()
            options.put("name", "HumbleCoders")
            options.put("description", "Course: $courseTitle")
            options.put("image", "https://via.placeholder.com/300x100.png?text=HumbleCoders")
            options.put("order_id", "")
            options.put("currency", "INR")
            options.put("amount", amount * 100) // Amount in paise
            options.put("prefill", JSONObject().apply {
                put("email", userEmail)
                put("contact", "9999999999")
            })

            checkout.open(activity, options)
        } catch (e: Exception) {
            Log.e("RazorpayPaymentService", "Error starting payment", e)
        }
    }
}
