package com.example.otovitrin.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import com.example.otovitrin.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class filter(private val listener: FiltreListener) : BottomSheetDialogFragment() {

    companion object {//veri taşıma
        fun newInstance(
            listener: FiltreListener,
            siralama: String,
            min: Int,
            max: Int,
            kendi: Boolean,
            favori: Boolean
        ): filter {
            val fragment = filter(listener)
            val args = Bundle()
            args.putString("siralama", siralama)
            args.putInt("min", min)
            args.putInt("max", max)
            args.putBoolean("kendi", kendi)
            args.putBoolean("favori", favori)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.filter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnUygula = view.findViewById<Button>(R.id.btnFiltreUygula)
        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroupSirala)
        val minFiyatInput = view.findViewById<EditText>(R.id.editMinFiyat)
        val maxFiyatInput = view.findViewById<EditText>(R.id.editMaxFiyat)
        val chbKendi = view.findViewById<CheckBox>(R.id.chbSadeceKendiIlanlarim)
        val chbFavori = view.findViewById<CheckBox>(R.id.chbSadeceFavoriler)


        arguments?.let { bundle ->

            val min = bundle.getInt("min")
            val max = bundle.getInt("max")

            if (min > 0) minFiyatInput.setText(min.toString())
            if (max < Int.MAX_VALUE) maxFiyatInput.setText(max.toString())

            //Checkbox
            chbKendi.isChecked = bundle.getBoolean("kendi")
            chbFavori.isChecked = bundle.getBoolean("favori")

            // Sıralama
            val siralama = bundle.getString("siralama")
            when (siralama) {
                "FiyatArtan" -> radioGroup.check(R.id.radioFiyatArtan)
                "FiyatAzalan" -> radioGroup.check(R.id.radioFiyatAzalan)
                else -> radioGroup.check(R.id.radioTarihYeniden)
            }
        }

        btnUygula.setOnClickListener {
            val secilenRadioId = radioGroup.checkedRadioButtonId
            val siralamaTuru = when (secilenRadioId) {
                R.id.radioFiyatArtan -> "FiyatArtan"
                R.id.radioFiyatAzalan -> "FiyatAzalan"
                else -> "TarihYeniden"
            }

            val minFiyat = minFiyatInput.text.toString().toIntOrNull() ?: 0
            val maxFiyat = maxFiyatInput.text.toString().toIntOrNull() ?: Int.MAX_VALUE
            val kendiIlanlarim = chbKendi.isChecked
            val favoriler = chbFavori.isChecked

            listener.filtreleriUygula(siralamaTuru, minFiyat, maxFiyat, kendiIlanlarim, favoriler)
            dismiss()//kapama
        }
    }
}