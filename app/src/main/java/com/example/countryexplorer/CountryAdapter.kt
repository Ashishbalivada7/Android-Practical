package com.example.countryexplorer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.countryexplorer.data.CountryResponse

class CountryAdapter(
    private var countryList: List<CountryResponse>,
    private val onCountryClick: (CountryResponse) -> Unit,
    private val onWishlistToggle: (CountryResponse, Boolean) -> Unit
) : RecyclerView.Adapter<CountryAdapter.CountryViewHolder>() {

    class CountryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgFlag: ImageView = view.findViewById(R.id.imgFlag)
        val txtCountryName: TextView = view.findViewById(R.id.txtCountryName)
        val txtRegion: TextView = view.findViewById(R.id.txtRegion)
        val checkboxWishlist: CheckBox = view.findViewById(R.id.checkboxWishlist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_country, parent, false)
        return CountryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CountryViewHolder, position: Int) {
        val country = countryList[position]
        val context = holder.itemView.context

        holder.txtCountryName.text = country.name?.common ?: "Unknown"
        holder.txtRegion.text = country.region ?: "N/A"

        val flagUrl = country.flags?.png ?: country.flags?.svg
        if (!flagUrl.isNullOrEmpty()) {
            holder.imgFlag.load(flagUrl)
        } else {
            holder.imgFlag.setImageDrawable(null)
        }

        val countryCode = country.cca2 ?: country.cca3
        val isWishlisted = WishlistManager.isWishlisted(context, countryCode)

        holder.checkboxWishlist.setOnCheckedChangeListener(null)
        holder.checkboxWishlist.isChecked = isWishlisted
        holder.checkboxWishlist.setOnCheckedChangeListener { _, isChecked ->
            onWishlistToggle(country, isChecked)
        }

        holder.itemView.setOnClickListener { onCountryClick(country) }
    }

    override fun getItemCount(): Int = countryList.size

    fun updateData(newList: List<CountryResponse>) {
        countryList = newList
        notifyDataSetChanged()
    }
}
