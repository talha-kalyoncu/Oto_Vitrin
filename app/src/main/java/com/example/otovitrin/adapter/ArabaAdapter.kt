package com.example.otovitrin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.otovitrin.R
import com.example.otovitrin.view.VitrinFragmentDirections
import com.example.otovitrin.databinding.RecyclerRowBinding
import com.example.otovitrin.model.Araba
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class ArabaAdapter(private val arabaListesi: ArrayList<Araba>) : RecyclerView.Adapter<ArabaAdapter.ArabaHolder>() {

    class ArabaHolder(val binding: RecyclerRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArabaHolder {
        val binding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ArabaHolder(binding)
    }

    override fun onBindViewHolder(holder: ArabaHolder, position: Int) {
        val araba = arabaListesi[position]

        holder.binding.BaslikTextView.text = araba.baslik
        holder.binding.FiyatTextView.text = "${formatla((araba.fiyat ?: 0).toLong())} ₺"


        Glide.with(holder.itemView.context)
            .load(araba.gorselUrl)
            .override(300, 200) // resmi küçültme işlemi
            .fitCenter() // kutucuğa sığdırma
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.binding.imgAraba)


        holder.itemView.setOnClickListener {

                val action = VitrinFragmentDirections.Companion.actionVitrinFragmentToIlanGoruntulemeFragment(araba)
                Navigation.findNavController(it).navigate(action)
        }
    }
    // binlik ayracı nokta yapma fonksiyonu
    private fun formatla(fiyat: Long): String {
        val semboller = DecimalFormatSymbols()
        semboller.groupingSeparator = '.'
        val formatter = DecimalFormat("#,###", semboller)
        return formatter.format(fiyat)
    }
    override fun getItemCount(): Int {
        return arabaListesi.size
    }
}