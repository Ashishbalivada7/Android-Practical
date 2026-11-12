package com.example.countryexplorer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.countryexplorer.data.CountriesApiService
import com.example.countryexplorer.data.CountryResponse
import com.google.android.gms.location.LocationServices
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var apiService: CountriesApiService
    private lateinit var countryAdapter: CountryAdapter
    private var allCountriesList: List<CountryResponse> = listOf()

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var txtEmpty: TextView
    private lateinit var searchView: SearchView
    private lateinit var chipGroup: ChipGroup
    private lateinit var btnDetectLocation: Button

    private var currentSearchQuery: String = ""
    private var currentSelectedRegion: String = "All"

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val isGuest = intent.getBooleanExtra("is_guest", false)
        if (auth.currentUser == null && !isGuest) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        apiService = CountriesApiService.create()

        initViews()
        setupRecyclerView()
        setupSearchAndFilters()
        setupLocationButton()

        fetchAllCountries()
    }

    override fun onResume() {
        super.onResume()
        filterCountries()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewCountries)
        progressBar = findViewById(R.id.progressBar)
        txtEmpty = findViewById(R.id.txtEmpty)
        searchView = findViewById(R.id.searchView)
        chipGroup = findViewById(R.id.chipGroupRegions)
        btnDetectLocation = findViewById(R.id.btnDetectLocation)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)

        countryAdapter = CountryAdapter(
            countryList = listOf(),
            onCountryClick = { country ->
                openCountryDetail(country)
            },
            onWishlistToggle = { country, isChecked ->
                val code = country.cca2 ?: country.cca3
                WishlistManager.setWishlisted(this, code, isChecked)
                if (currentSelectedRegion == "Wishlist") {
                    filterCountries()
                }
            }
        )
        recyclerView.adapter = countryAdapter
    }

    private fun openCountryDetail(country: CountryResponse) {
        val intent = Intent(this, DetailActivity::class.java).apply {
            putExtra(DetailActivity.EXTRA_COUNTRY, country)
        }
        startActivity(intent)
    }

    private fun fetchAllCountries() {
        progressBar.visibility = View.VISIBLE
        txtEmpty.visibility = View.GONE
        recyclerView.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getAllCountries()
                val sortedList = response.sortedBy { it.name?.common }
                withContext(Dispatchers.Main) {
                    allCountriesList = sortedList
                    progressBar.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    filterCountries()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    txtEmpty.text = "Failed to load countries: ${e.localizedMessage}"
                    txtEmpty.visibility = View.VISIBLE
                    Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupSearchAndFilters() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentSearchQuery = query.orEmpty()
                filterCountries()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText.orEmpty()
                filterCountries()
                return true
            }
        })

        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                currentSelectedRegion = "All"
            } else {
                val chip = group.findViewById<Chip>(checkedIds[0])
                currentSelectedRegion = chip?.text?.toString() ?: "All"
            }
            filterCountries()
        }
    }

    private fun filterCountries() {
        var filtered = allCountriesList

        if (currentSearchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.name?.common?.contains(currentSearchQuery, ignoreCase = true) == true ||
                        it.name?.official?.contains(currentSearchQuery, ignoreCase = true) == true ||
                        it.cca2?.contains(currentSearchQuery, ignoreCase = true) == true ||
                        it.cca3?.contains(currentSearchQuery, ignoreCase = true) == true
            }
        }

        if (currentSelectedRegion == "Wishlist") {
            filtered = filtered.filter {
                WishlistManager.isWishlisted(this, it.cca2 ?: it.cca3)
            }
        } else if (currentSelectedRegion != "All" && currentSelectedRegion.isNotEmpty()) {
            filtered = filtered.filter {
                it.region.equals(currentSelectedRegion, ignoreCase = true)
            }
        }

        countryAdapter.updateData(filtered)

        if (filtered.isEmpty()) {
            txtEmpty.text = if (currentSelectedRegion == "Wishlist") "No wishlisted countries" else "No countries found"
            txtEmpty.visibility = View.VISIBLE
        } else {
            txtEmpty.visibility = View.GONE
        }
    }

    private fun setupLocationButton() {
        btnDetectLocation.setOnClickListener {
            detectUserCountry()
        }
    }

    private fun detectUserCountry() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        btnDetectLocation.isEnabled = false

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            btnDetectLocation.isEnabled = true
            if (location != null) {
                val geocoder = Geocoder(this, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                        handleAddressResult(addresses)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    handleAddressResult(addresses)
                }
            } else {
                handleAddressResult(null)
            }
        }.addOnFailureListener {
            btnDetectLocation.isEnabled = true
            Toast.makeText(this, "Could not get location.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                detectUserCountry()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleAddressResult(addresses: List<Address>?) {
        val countryCode = addresses?.firstOrNull()?.countryCode ?: "IN"

        val detectedCountry = allCountriesList.firstOrNull {
            it.cca2.equals(countryCode, ignoreCase = true) || it.cca3.equals(countryCode, ignoreCase = true)
        }

        if (detectedCountry != null) {
            Toast.makeText(
                this,
                "Detected Country: ${detectedCountry.name?.common ?: "Unknown"}",
                Toast.LENGTH_LONG
            ).show()
            openCountryDetail(detectedCountry)
        } else {
            Toast.makeText(this, "Country details not found for code: $countryCode", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                auth.signOut()
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
