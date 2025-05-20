package com.example.recipes.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recipes.BuildConfig
import com.example.recipes.R
import com.example.recipes.data.model.NearbyStore
import com.example.recipes.data.model.ShoppingItem
import com.example.recipes.databinding.FragmentShoppingListBinding
import com.example.recipes.ui.activities.PaymentActivity
import com.example.recipes.ui.adapters.NearbyStoresAdapter
import com.example.recipes.ui.adapters.ShoppingListAdapter
import com.example.recipes.viewmodel.ShoppingListViewModel
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import kotlin.random.Random

class ShoppingListFragment : Fragment() {

    private var _binding: FragmentShoppingListBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val vm   by viewModels<ShoppingListViewModel>()

    private val shoppingAdapter by lazy {
        ShoppingListAdapter { item ->
            vm.removeItems(auth.currentUser!!.uid, listOf(item))
        }
    }
    private val nearbyAdapter = NearbyStoresAdapter()

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient

    private val locLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchNearbyStores()
        else Toast.makeText(
            requireContext(),
            getString(R.string.places_error),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = FragmentShoppingListBinding
        .inflate(inflater, container, false)
        .also { _binding = it }
        .root

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.hide()

        // 0) Provide ViewModel with the full set of valid dish & ingredient names
        vm.setValidItems(
            setOf(
                // TODO: fill in your in-app recipe titles and their ingredients here,
                // e.g. "spaghetti bolognese", "tomato", "beef", "onion", …
            )
        )

        // 1) Check for Google Play services (disable find-stores if missing)
        val gmsStatus = GoogleApiAvailability
            .getInstance()
            .isGooglePlayServicesAvailable(requireContext())
        val hasGms = gmsStatus == ConnectionResult.SUCCESS
        if (!hasGms) {
            binding.btnFindStores.isEnabled = false
            binding.btnFindStores.alpha     = 0.5f
            Toast.makeText(
                requireContext(),
                getString(R.string.places_unavailable_toast),
                Toast.LENGTH_LONG
            ).show()
        }

        // 2) Init Places & fused location if available
        if (hasGms) {
            if (!Places.isInitialized()) {
                Places.initialize(requireContext(), getString(R.string.google_places_key))
            }
            placesClient = Places.createClient(requireContext())
            fusedClient  = LocationServices.getFusedLocationProviderClient(requireContext())
        }

        // 3) Tabs → switch panes & toggle FAB visibility
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position == 0) {
                    showShoppingPane()
                    binding.fabAddItem.isVisible = true
                } else {
                    showDiscoverPane()
                    binding.fabAddItem.isVisible = false
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        binding.tabLayout.getTabAt(0)?.select()

        // 4) RecyclerViews
        binding.shoppingListRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter       = shoppingAdapter
        }
        binding.nearbyStoresRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter       = nearbyAdapter
        }

        // 5) Observe shopping list and total price
        vm.items.observe(viewLifecycleOwner) { list ->
            shoppingAdapter.submitList(list)
            binding.emptyTextView.isVisible = list.isEmpty()
        }
        vm.totalPrice.observe(viewLifecycleOwner) { total ->
            binding.totalTextView.text = getString(R.string.total_format, total)
        }

        // 6) “Complete” button → payment activity
        binding.completeButton.setOnClickListener {
            val items = vm.items.value.orEmpty()
            if (items.isEmpty()) {
                Toast.makeText(requireContext(), R.string.no_items, Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(requireContext(), PaymentActivity::class.java))
            }
        }

        // 7) “Find Stores” click
        binding.btnFindStores.setOnClickListener {
            if (!hasGms) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.places_unavailable_toast),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fetchNearbyStores()
            } else {
                locLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        // 8) FAB → manual entry dialog
        binding.fabAddItem.setOnClickListener { showAddItemDialog() }

        // 9) Load the user’s shopping list
        auth.currentUser?.uid?.let { vm.load(it) }

        // 10) Override back-gesture
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigateUp()
                }
            }
        )
    }

    private fun showShoppingPane() {
        binding.shoppingContainer.isVisible  = true
        binding.discoverContainer.isVisible = false
    }
    private fun showDiscoverPane() {
        binding.shoppingContainer.isVisible  = false
        binding.discoverContainer.isVisible = true
    }

    private fun showAddItemDialog() {
        val ctx = requireContext()
        val dialogView = layoutInflater.inflate(R.layout.add_item, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.editItemName)

        AlertDialog.Builder(ctx)
            .setTitle(R.string.add_item)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val raw = nameInput.text.toString().trim()
                val key = raw.lowercase(Locale.ROOT)
                // Only allow names present in the validItems set
                if (vm.validItems.value?.contains(key) != true) {
                    Toast.makeText(ctx, R.string.invalid_food_name, Toast.LENGTH_SHORT).show()
                } else {
                    // Assign a random price between 1 and 6
                    val price = Random.nextDouble(1.0, 6.0)
                        .let { String.format(Locale.getDefault(), "%.2f", it).toDouble() }

                    auth.currentUser?.uid?.let { uid ->
                        vm.addWithPrice(uid, ShoppingItem(name = raw, price = price))
                        Toast.makeText(
                            ctx,
                            getString(R.string.added_to_shopping_list),
                            Toast.LENGTH_SHORT
                        ).show()
                    } ?: Toast.makeText(ctx, R.string.login_to_add, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun fetchNearbyStores() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { loc ->
            if (loc != null) {
                fetchFromWebService(loc)
            } else {
                fallbackLocation()?.let { fetchFromWebService(it) }
                    ?: Toast.makeText(
                        requireContext(),
                        "Unable to determine location",
                        Toast.LENGTH_SHORT
                    ).show()
            }
        }.addOnFailureListener { e ->
            fallbackLocation()?.let { fetchFromWebService(it) }
                ?: Toast.makeText(
                    requireContext(),
                    "Location error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun fallbackLocation(): Location? {
        val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.getProviders(true)
            .mapNotNull { provider ->
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) lm.getLastKnownLocation(provider) else null
            }
            .maxByOrNull { it.accuracy }
    }

    private fun fetchFromWebService(loc: Location) {
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("maps.googleapis.com")
            .addPathSegments("maps/api/place/nearbysearch/json")
            .addQueryParameter("location", "${loc.latitude},${loc.longitude}")
            .addQueryParameter("radius", "2000")
            .addQueryParameter("type", "grocery_or_supermarket")
            .addQueryParameter("key", BuildConfig.PLACES_API_KEY)
            .build()

        OkHttpClient().newCall(Request.Builder().url(url).build())
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "Network error: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string().orEmpty()
                    requireActivity().runOnUiThread {
                        if (!response.isSuccessful) {
                            Toast.makeText(
                                requireContext(),
                                "Places API HTTP ${response.code}",
                                Toast.LENGTH_LONG
                            ).show()
                            return@runOnUiThread
                        }
                        val root = JSONObject(body)
                        if (root.has("error_message")) {
                            Toast.makeText(
                                requireContext(),
                                "Places API error: ${root.optString("error_message")}",
                                Toast.LENGTH_LONG
                            ).show()
                            return@runOnUiThread
                        }
                        val arr = root.optJSONArray("results") ?: run {
                            Toast.makeText(
                                requireContext(),
                                "No nearby grocery stores found",
                                Toast.LENGTH_SHORT
                            ).show()
                            nearbyAdapter.submitList(emptyList())
                            return@runOnUiThread
                        }
                        val stores = mutableListOf<NearbyStore>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            val name    = obj.optString("name", "")
                            val address = obj.optString("vicinity", "")
                            val locObj  = obj.optJSONObject("geometry")
                                ?.optJSONObject("location") ?: continue
                            val lat  = locObj.optDouble("lat")
                            val lng  = locObj.optDouble("lng")
                            val dist = FloatArray(1).also {
                                android.location.Location.distanceBetween(
                                    loc.latitude, loc.longitude,
                                    lat, lng, it
                                )
                            }[0]
                            stores += NearbyStore(name, address, lat, lng, dist)
                        }
                        nearbyAdapter.submitList(stores.sortedBy { it.distanceMeters })
                    }
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }
}
