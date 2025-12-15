package com.example.otovitrin.model

import com.google.gson.annotations.SerializedName

// internetten gelen veriyi tutacak
data class DovizResponse(
    @SerializedName("rates")
    val rates: Map<String, Double>
)