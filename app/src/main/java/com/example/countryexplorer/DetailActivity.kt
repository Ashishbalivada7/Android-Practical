package com.example.countryexplorer

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.countryexplorer.data.CountryResponse
import java.text.NumberFormat

class DetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_COUNTRY = "extra_country"
    }

    private lateinit var country: CountryResponse
    private lateinit var btnWishlistToggle: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        @Suppress("DEPRECATION")
        val countryObj = intent.getSerializableExtra(EXTRA_COUNTRY) as? CountryResponse
        if (countryObj == null) {
            Toast.makeText(this, "Country data not available", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        country = countryObj
        title = country.name?.common ?: "Country Details"

        val imgFlag = findViewById<ImageView>(R.id.imgDetailFlag)
        val txtName = findViewById<TextView>(R.id.txtDetailName)
        val txtOfficialName = findViewById<TextView>(R.id.txtDetailOfficialName)
        val txtCapital = findViewById<TextView>(R.id.txtDetailCapital)
        val txtRegion = findViewById<TextView>(R.id.txtDetailRegion)
        val txtPopulation = findViewById<TextView>(R.id.txtDetailPopulation)
        val txtCurrencies = findViewById<TextView>(R.id.txtDetailCurrencies)
        val txtLanguages = findViewById<TextView>(R.id.txtDetailLanguages)
        val txtCode = findViewById<TextView>(R.id.txtDetailCode)
        btnWishlistToggle = findViewById(R.id.btnWishlistToggle)

        val flagUrl = country.flags?.png ?: country.flags?.svg
        if (!flagUrl.isNullOrEmpty()) {
            imgFlag.load(flagUrl)
        }

        txtName.text = country.name?.common ?: "N/A"
        txtOfficialName.text = country.name?.official ?: ""
        txtCapital.text = "Capital: ${country.capital?.joinToString(", ") ?: "N/A"}"
        txtRegion.text = "Region: ${country.region ?: "N/A"} (${country.subregion ?: "N/A"})"

        val pop = country.population
        val formattedPopulation = if (pop != null && pop > 0) {
            NumberFormat.getInstance().format(pop)
        } else {
            "N/A"
        }
        txtPopulation.text = "Population: $formattedPopulation"

        val currenciesString = country.currencies?.values?.mapNotNull {
            if (!it.symbol.isNullOrEmpty()) "${it.name} (${it.symbol})" else it.name
        }?.joinToString(", ") ?: "N/A"
        txtCurrencies.text = "Currencies: $currenciesString"

        val languagesString = country.languages?.values?.joinToString(", ") ?: "N/A"
        txtLanguages.text = "Languages: $languagesString"

        txtCode.text = "Country Code: ${country.cca2 ?: "N/A"} / ${country.cca3 ?: "N/A"}"

        updateWishlistButton()

        btnWishlistToggle.setOnClickListener {
            val countryCode = country.cca2 ?: country.cca3
            val isWishlisted = WishlistManager.isWishlisted(this, countryCode)
            WishlistManager.setWishlisted(this, countryCode, !isWishlisted)
            updateWishlistButton()
            val message = if (!isWishlisted) "Added to Wishlist" else "Removed from Wishlist"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateWishlistButton() {
        val countryCode = country.cca2 ?: country.cca3
        val isWishlisted = WishlistManager.isWishlisted(this, countryCode)
        btnWishlistToggle.text = if (isWishlisted) "Remove from Wishlist" else "Add to Wish-to-Visit List"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
