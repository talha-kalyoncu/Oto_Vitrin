package com.example.otovitrin.view

interface FiltreListener {
    fun filtreleriUygula(
        siralama: String,
        minFiyat: Int,
        maxFiyat: Int,
        sadeceKendiIlanlarim: Boolean,
        sadeceFavoriler: Boolean
    )
}