package com.example.recipes.ui.activities

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.recipes.R
import com.example.recipes.databinding.ActivityPaymentBinding
import com.example.recipes.viewmodel.OrdersViewModel
import com.google.android.gms.location.LocationServices
import java.util.Calendar
import java.util.Locale

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private val ordersVm: OrdersViewModel by viewModels()

    // Launches fine-location permission request
    private val locLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchAndFillAddress()
        else Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1️⃣ Ask for location at start to auto-fill street+neighborhood
        locLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        // 2️⃣ Card field length limits
        binding.cardNumberEditText.filters = arrayOf(InputFilter.LengthFilter(16))
        binding.expiryEditText.filters     = arrayOf(InputFilter.LengthFilter(5))
        binding.cvvEditText.filters        = arrayOf(InputFilter.LengthFilter(3))

        // 3️⃣ Auto-insert “/” in MM/YY
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

        // 4️⃣ Handle “Submit Payment” click
        binding.buttonSubmitPayment.setOnClickListener {
            // — Address validation —
            val street       = binding.etStreet.text.toString().trim()
            val neighborhood = binding.etNeighborhood.text.toString().trim()
            val apartment    = binding.etApartment.text.toString().trim()
            val floor        = binding.etFloor.text.toString().trim()

            if (street.isEmpty() ||
                neighborhood.isEmpty() ||
                apartment.isEmpty() ||
                floor.isEmpty()
            ) {
                Toast.makeText(this, R.string.error_address_incomplete, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val fullAddress = "$street, $neighborhood, Apt $apartment, Flr $floor"

            // — Card validation —
            val cardNumber = binding.cardNumberEditText.text.toString().trim()
            val expiry     = binding.expiryEditText.text.toString().trim()
            val cvv        = binding.cvvEditText.text.toString().trim()

            if (cardNumber.length < 12 || expiry.length != 5 || cvv.length != 3) {
                Toast.makeText(this, R.string.enter_card_details, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val parts = expiry.split("/")
            val mm = parts.getOrNull(0)?.toIntOrNull()
            val yy = parts.getOrNull(1)?.toIntOrNull()
            if (mm == null || yy == null || mm !in 1..12) {
                Toast.makeText(this, R.string.error_invalid_month, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Reject past expiry
            val cal = Calendar.getInstance()
            val currentYY = cal.get(Calendar.YEAR) % 100
            val currentMM = cal.get(Calendar.MONTH) + 1
            if (yy < currentYY || (yy == currentYY && mm < currentMM)) {
                Toast.makeText(this, R.string.error_expired_card, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // — Everything ok: create order with address —
            ordersVm.createOrder(
                cardNumber = cardNumber,
                expiry     = expiry,
                cvv        = cvv,
                address    = fullAddress
            ) { success ->
                if (success) {
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this, R.string.payment_failed, Toast.LENGTH_LONG).show()
                }
            }
        }

        // 5️⃣ Handle system back as “cancel”
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        })
    }

    /** Fetch last known location → Geocode → fill street & neighborhood */
    private fun fetchAndFillAddress() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val client = LocationServices.getFusedLocationProviderClient(this)
        client.lastLocation
            .addOnSuccessListener { loc ->
                loc?.let { location ->
                    try {
                        val addresses = Geocoder(this, Locale.getDefault())
                            .getFromLocation(location.latitude, location.longitude, 1)
                        addresses?.firstOrNull()?.let { geo ->
                            // thoroughfare = street name, subThoroughfare = number
                            binding.etStreet.setText(
                                listOfNotNull(geo.thoroughfare, geo.subThoroughfare)
                                    .joinToString(" ")
                            )
                            binding.etNeighborhood.setText(
                                geo.subLocality ?: geo.locality.orEmpty()
                            )
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            this,
                            R.string.address_lookup_failed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    R.string.address_lookup_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}
