package com.example.countryexplorer.data

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CountryResponse(
    @SerializedName("name") val name: Name? = null,
    @SerializedName("capital") val capital: List<String>? = null,
    @SerializedName("region") val region: String? = null,
    @SerializedName("subregion") val subregion: String? = null,
    @SerializedName("population") val population: Long? = null,
    @SerializedName("currencies") val currencies: Map<String, Currency>? = null,
    @SerializedName("languages") val languages: Map<String, String>? = null,
    @SerializedName("flags") val flags: Flags? = null,
    @SerializedName("cca2") val cca2: String? = null,
    @SerializedName("cca3") val cca3: String? = null
) : Serializable

data class Name(
    @SerializedName("common") val common: String? = null,
    @SerializedName("official") val official: String? = null
) : Serializable

data class Currency(
    @SerializedName("name") val name: String? = null,
    @SerializedName("symbol") val symbol: String? = null
) : Serializable

data class Flags(
    @SerializedName("png") val png: String? = null,
    @SerializedName("svg") val svg: String? = null,
    @SerializedName("alt") val alt: String? = null
) : Serializable
