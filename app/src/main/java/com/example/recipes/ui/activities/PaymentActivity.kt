// app/src/main/java/com/example/recipes/ui/activities/PaymentActivity.kt
package com.example.recipes.ui.activities

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.recipes.R
import com.example.recipes.databinding.ActivityPaymentBinding
import com.example.recipes.viewmodel.OrdersViewModel
import java.util.*

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private val ordersVm: OrdersViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1) Limit lengths
        binding.cardNumberEditText.filters = arrayOf(InputFilter.LengthFilter(16))
        binding.expiryEditText.filters     = arrayOf(InputFilter.LengthFilter(5))
        binding.cvvEditText.filters        = arrayOf(InputFilter.LengthFilter(3))

        // 2) Auto-insert "/" after two digits
        binding.expiryEditText.addTextChangedListener(object : TextWatcher {
            private var selfChange = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (selfChange) return
                val text = s.toString()
                if (text.length == 2 && !text.contains("/")) {
                    selfChange = true
                    binding.expiryEditText.setText("$text/")
                    binding.expiryEditText.setSelection(binding.expiryEditText.text!!.length)
                    selfChange = false
                } else if (text.length == 4 && !text.contains("/")) {
                    selfChange = true
                    val new = text.substring(0, 2) + "/" + text.substring(2)
                    binding.expiryEditText.setText(new)
                    binding.expiryEditText.setSelection(new.length)
                    selfChange = false
                }
            }
        })

        // 3) Handle payment submission
        binding.buttonSubmitPayment.setOnClickListener {
            val cardNumber = binding.cardNumberEditText.text.toString().trim()
            val expiry     = binding.expiryEditText.text.toString().trim()   // should be "MM/YY"
            val cvv        = binding.cvvEditText.text.toString().trim()

            // Basic completeness
            if (cardNumber.length < 12 || expiry.length != 5 || cvv.length != 3) {
                Toast.makeText(this, R.string.enter_card_details, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Split month/year
            val parts = expiry.split("/")
            val mm = parts.getOrNull(0)?.toIntOrNull()
            val yy = parts.getOrNull(1)?.toIntOrNull()
            if (mm == null || yy == null || mm !in 1..12) {
                Toast.makeText(this, R.string.error_invalid_month, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Reject anything before 05/25
            if (yy < 25 || (yy == 25 && mm < 5)) {
                Toast.makeText(this, "Card expiry must be 05/25 or later", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Everything valid → process
            ordersVm.createOrder(cardNumber, expiry, cvv) { success ->
                if (success) {
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this, R.string.payment_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
