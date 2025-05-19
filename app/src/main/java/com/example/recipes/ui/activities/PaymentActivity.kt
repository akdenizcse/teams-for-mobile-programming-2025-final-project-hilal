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

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private val ordersVm: OrdersViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1) Prevent typing/pasting beyond allowed lengths
        binding.cardNumberEditText.filters = arrayOf(InputFilter.LengthFilter(16))
        binding.expiryEditText.filters     = arrayOf(InputFilter.LengthFilter(5))
        binding.cvvEditText.filters        = arrayOf(InputFilter.LengthFilter(3))

        // 2) Handle submit
        binding.buttonSubmitPayment.setOnClickListener {
            val cardNumber = binding.cardNumberEditText.text.toString().trim()
            val expiry     = binding.expiryEditText.text.toString().trim()   // MM/YY
            val cvv        = binding.cvvEditText.text.toString().trim()

            /* basic completeness */
            if (cardNumber.isBlank() || expiry.isBlank() || cvv.isBlank()) {
                Toast.makeText(this, R.string.enter_card_details, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.expiryEditText.addTextChangedListener(object : TextWatcher {
                private var editing = false
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable?) {}

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (editing) return
                    if (s.length == 2 && before == 0) {     // user just typed the 2nd digit
                        editing = true
                        binding.expiryEditText.append("/")
                        editing = false
                    }
                    // If user pasted "1226" (length 4) -> insert slash between 2&3
                    if (s.length == 4 && !s.contains("/")) {
                        editing = true
                        val new = "${s.substring(0,2)}/${s.substring(2)}"
                        binding.expiryEditText.setText(new)
                        binding.expiryEditText.setSelection(new.length)
                        editing = false
                    }
                }
            })


            /* ─── validate month ≤ 12 ─── */
            val monthPart = expiry.takeWhile { it != '/' }          // “MM”
            val monthOk   = monthPart.length == 2 && monthPart.toIntOrNull()?.let { it in 1..12 } == true
            if (!monthOk) {
                Toast.makeText(this, "Expiry month must be 01-12", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            /* (optional) validate full pattern MM/YY length == 4 */
            if (expiry.length != 5 || expiry[2] != '/') {
                Toast.makeText(this, "Expiry must be in MM/YY format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            /* now call ViewModel createOrder(...) */
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
