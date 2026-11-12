package com.example.countryexplorer

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object WishlistManager {
    private const val PREF_NAME = "country_explorer_wishlist"
    private const val KEY_WISHLIST = "wishlist_set"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getWishlistedCodes(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_WISHLIST, emptySet()) ?: emptySet()
    }

    fun isWishlisted(context: Context, code: String?): Boolean {
        if (code.isNullOrEmpty()) return false
        return getWishlistedCodes(context).contains(code.uppercase())
    }

    fun addWishlist(context: Context, code: String?) {
        if (code.isNullOrEmpty()) return
        val current = getWishlistedCodes(context).toMutableSet()
        current.add(code.uppercase())
        getPrefs(context).edit { putStringSet(KEY_WISHLIST, current) }
    }

    fun removeWishlist(context: Context, code: String?) {
        if (code.isNullOrEmpty()) return
        val current = getWishlistedCodes(context).toMutableSet()
        current.remove(code.uppercase())
        getPrefs(context).edit { putStringSet(KEY_WISHLIST, current) }
    }

    fun setWishlisted(context: Context, code: String?, isWishlisted: Boolean) {
        if (isWishlisted) {
            addWishlist(context, code)
        } else {
            removeWishlist(context, code)
        }
    }
}
