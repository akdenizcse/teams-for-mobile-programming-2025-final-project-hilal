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

        // Init Places + FusedLocation
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.google_places_key))
        }
        placesClient = Places.createClient(requireContext())
        fusedClient  = LocationServices.getFusedLocationProviderClient(requireContext())

        // Tabs → switch panes
        binding.tabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position == 0) showShoppingPane() else showDiscoverPane()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        binding.tabLayout.getTabAt(0)?.select()

        // Shopping-list RecyclerView
        binding.shoppingListRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter       = shoppingAdapter
        }
        // Discover RecyclerView
        binding.nearbyStoresRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter       = nearbyAdapter
        }

        // Observe shopping list
        vm.items.observe(viewLifecycleOwner) { list ->
            shoppingAdapter.submitList(list)
            binding.emptyTextView.isVisible = list.isEmpty()
        }
        vm.totalPrice.observe(viewLifecycleOwner) { total ->
            binding.totalTextView.text = getString(R.string.total_format, total)
        }

        // Complete → Payment flow
        binding.completeButton.setOnClickListener {
            val items = vm.items.value.orEmpty()
            if (items.isEmpty()) {
                Toast.makeText(requireContext(), R.string.no_items, Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(requireContext(), PaymentActivity::class.java))
            }
        }

        // Find stores
        binding.btnFindStores.setOnClickListener {
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

        // — NEW: FAB manual entry —
        binding.fabAddItem.setOnClickListener {
            showAddItemDialog()
        }

        // Load shopping list
        vm.load(auth.currentUser!!.uid)

        // Back-gesture override
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigateUp()
                }
            }
        )

        binding.fabAddItem.setOnClickListener {
            showAddItemDialog()
        }
    }



    private fun showShoppingPane() {
        binding.shoppingContainer.isVisible  = true
        binding.discoverContainer.isVisible = false
    }
    private fun showDiscoverPane() {
        binding.shoppingContainer.isVisible  = false
        binding.discoverContainer.isVisible = true
    }

    /** Show AlertDialog with EditText to add a manual item. */


    private fun showAddItemDialog() {
        val ctx = requireContext()
        val dialogView = layoutInflater.inflate(R.layout.add_item, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.editItemName)

        AlertDialog.Builder(ctx)
            .setTitle(R.string.add_item)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(ctx, R.string.name_empty, Toast.LENGTH_SHORT).show()
                } else {
                    // assign random price between 1 and 6
                    val price = Random.nextDouble(1.0, 6.0).let {
                        // round to two decimals
                        String.format(Locale.getDefault(), "%.2f", it).toDouble()
                    }

                    auth.currentUser?.uid?.let { uid ->
                        vm.addWithPrice(uid, ShoppingItem(name = name, price = price))
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
        // 1) Guard permission
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        // 2) Request a fresh, high-accuracy location
        fusedClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { loc ->
            if (loc != null) {
                fetchFromWebService(loc)
            } else {
                // Fallback only if that fails
                fallbackLocation()?.let { fetchFromWebService(it) }
                    ?: Toast.makeText(requireContext(),
                        "Unable to determine location",
                        Toast.LENGTH_SHORT
                    ).show()
            }
        }.addOnFailureListener { e ->
            // In case of broker or other error, fall back
            fallbackLocation()?.let { fetchFromWebService(it) }
                ?: Toast.makeText(requireContext(),
                    "Location error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
        }
    }



    private fun doFallback() {
        fallbackLocation()?.let { loc ->
            Toast.makeText(
                requireContext(),
                "Fallback location: ${"%.5f".format(loc.latitude)}, ${"%.5f".format(loc.longitude)}",
                Toast.LENGTH_SHORT
            ).show()
            fetchFromWebService(loc)
        } ?: Toast.makeText(
            requireContext(),
            "Unable to retrieve any location",
            Toast.LENGTH_SHORT
        ).show()
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

    /**
     * Given a valid Location, query Google Places Nearby Search
     * and submit sorted results to the adapter.
     */
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

        val request = Request.Builder().url(url).build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
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
                val bodyString = response.body?.string().orEmpty()

                requireActivity().runOnUiThread {
                    // 1) HTTP status check
                    if (!response.isSuccessful) {
                        Toast.makeText(
                            requireContext(),
                            "Places API HTTP ${response.code}",
                            Toast.LENGTH_LONG
                        ).show()
                        return@runOnUiThread
                    }

                    // 2) JSON-level error
                    val root = JSONObject(bodyString)
                    if (root.has("error_message")) {
                        Toast.makeText(
                            requireContext(),
                            "Places API error: ${root.optString("error_message")}",
                            Toast.LENGTH_LONG
                        ).show()
                        return@runOnUiThread
                    }

                    // 3) Empty results?
                    val results = root.optJSONArray("results")
                    if (results == null || results.length() == 0) {
                        Toast.makeText(
                            requireContext(),
                            "No nearby grocery stores found",
                            Toast.LENGTH_SHORT
                        ).show()
                        nearbyAdapter.submitList(emptyList())
                        return@runOnUiThread
                    }

                    // 4) Parse & display
                    val stores = mutableListOf<NearbyStore>()
                    for (i in 0 until results.length()) {
                        val obj = results.optJSONObject(i) ?: continue
                        val name    = obj.optString("name", "")
                        val address = obj.optString("vicinity", "")
                        val geo     = obj.optJSONObject("geometry")
                            ?.optJSONObject("location") ?: continue
                        val lat     = geo.optDouble("lat")
                        val lng     = geo.optDouble("lng")
                        val distance = FloatArray(1).also {
                            Location.distanceBetween(
                                loc.latitude, loc.longitude,
                                lat, lng, it
                            )
                        }[0]

                        stores += NearbyStore(name, address, lat, lng, distance)
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
