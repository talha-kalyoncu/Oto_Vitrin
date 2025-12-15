package com.example.otovitrin.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.otovitrin.api.BenzinAPI
import com.example.otovitrin.model.BenzinResponse
import com.example.otovitrin.api.DovizAPI
import com.example.otovitrin.model.DovizResponse
import com.example.otovitrin.R
import com.example.otovitrin.databinding.FragmentAnaSayfaBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AnaSayfaFragment : Fragment() {

    private var _binding: FragmentAnaSayfaBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnaSayfaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        rolKontroluYap()
        setupMenu()
        setupNavigationButtons()
        apiVerileriniCek()
    }

    private fun rolKontroluYap() {
        val userId = auth.currentUser?.uid
        if (userId != null) {

            firestore.collection("Users").document(userId).get()
                .addOnSuccessListener { document ->

                    if (_binding == null) return@addOnSuccessListener//

                    if (document != null && document.exists()) {
                        val rol = document.getString("rol")

                        if (rol == "Admin") {
                            binding.yonetimButton.visibility = View.VISIBLE
                            binding.istatistikButton.visibility = View.VISIBLE
                        }
                    }
                }
                .addOnFailureListener {

                }
        }
    }
    //menü popup ı
    private fun setupMenu() {
        binding.MenuButton.setOnClickListener { view ->
            val popup = PopupMenu(requireContext(), view)
            popup.menuInflater.inflate(R.menu.popup_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_ayarlar -> {
                        try {
                            findNavController().navigate(R.id.action_anaSayfaFragment_to_settingsFragment)
                        } catch (e: Exception) { }
                        true
                    }
                    R.id.action_cikis -> {
                        cikisYap()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun setupNavigationButtons() {
        binding.VitrinButton.setOnClickListener {
            findNavController().navigate(R.id.action_anaSayfaFragment_to_vitrinFragment)
        }

        binding.ilanEkleButton.setOnClickListener {
            findNavController().navigate(R.id.action_anaSayfaFragment_to_ilanEklemeFragment)
        }

        binding.yonetimButton.setOnClickListener {
            findNavController().navigate(R.id.action_anaSayfaFragment_to_vitrinFragment)
        }
        binding.istatistikButton.setOnClickListener {
            val istatistikPenceresi = istatistikFragment()
            istatistikPenceresi.show(parentFragmentManager, "IstatistikDialog")
        }
    }

    private fun cikisYap() {
        auth.signOut()
        Toast.makeText(requireContext(), "Çıkış Yapıldı", Toast.LENGTH_SHORT).show()
        findNavController().navigate(R.id.action_anaSayfaFragment_to_loginFragment)
    }

    private fun apiVerileriniCek() {

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.frankfurter.app/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val servis = retrofit.create(DovizAPI::class.java)

        //dolar
        servis.kurGetir("USD", "TRY").enqueue(object : Callback<DovizResponse> {
            override fun onResponse(call: Call<DovizResponse>, response: Response<DovizResponse>) {
                if (_binding == null) return
                if (response.isSuccessful) {
                    val kur = response.body()?.rates?.get("TRY")
                    if (kur != null) {
                        binding.txtDolarApi.text = "$kur ₺"
                    }
                }
            }
            override fun onFailure(call: Call<DovizResponse>, t: Throwable) {
                if (_binding == null) return
                binding.txtDolarApi.text = "..."
            }
        })

        //euro
        servis.kurGetir("EUR", "TRY").enqueue(object : Callback<DovizResponse> {
            override fun onResponse(call: Call<DovizResponse>, response: Response<DovizResponse>) {
                if (_binding == null) return
                if (response.isSuccessful) {
                    val kur = response.body()?.rates?.get("TRY")
                    if (kur != null) {
                        binding.txtEuroApi.text = "$kur ₺"
                    }
                }
            }
            override fun onFailure(call: Call<DovizResponse>, t: Throwable) {
                if (_binding == null) return
                binding.txtEuroApi.text = "..."
            }
        })


        val retrofitBenzin = Retrofit.Builder()
            .baseUrl("https://api.collectapi.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val servisBenzin = retrofitBenzin.create(BenzinAPI::class.java)

        servisBenzin.benzinFiyatlariniGetir("istanbul").enqueue(object : Callback<BenzinResponse> {
            override fun onResponse(call: Call<BenzinResponse>, response: Response<BenzinResponse>) {
                if (_binding == null) return


                if (response.isSuccessful && response.body()?.success == true) {
                    val liste = response.body()?.result

                    if (liste != null) {
                        val benzin = liste.find { it.marka?.contains("Kurşunsuz") == true }
                        val motorin = liste.find { it.marka?.contains("Motorin") == true }

                        if (benzin != null) binding.txtBenzinApi.text = "${benzin.fiyat} ₺"
                        if (motorin != null) binding.txtMotorinApi.text = "${motorin.fiyat} ₺"
                    }
                } else {
                    Toast.makeText(requireContext(),"Bilgiler yüklenemedi.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BenzinResponse>, t: Throwable) {
                if (_binding == null) return
                val retrofitBenzin = Retrofit.Builder()
                    .baseUrl("https://api.collectapi.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val servisBenzin = retrofitBenzin.create(BenzinAPI::class.java)

                servisBenzin.benzinFiyatlariniGetir("istanbul").enqueue(object : Callback<BenzinResponse> {
                    override fun onResponse(call: Call<BenzinResponse>, response: Response<BenzinResponse>) {
                        if (_binding == null) return


                        if (response.isSuccessful && response.body()?.success == true) {
                            val liste = response.body()?.result

                            if (liste != null) {
                                val benzin = liste.find { it.marka?.contains("Kurşunsuz") == true }
                                val motorin = liste.find { it.marka?.contains("Motorin") == true }

                                if (benzin != null) binding.txtBenzinApi.text = "${benzin.fiyat} ₺"
                                if (motorin != null) binding.txtMotorinApi.text = "${motorin.fiyat} ₺"
                            }
                        } else {
                            Toast.makeText(requireContext(),"Bilgiler yüklenemedi.", Toast.LENGTH_SHORT).show()

                        }
                    }

                    override fun onFailure(call: Call<BenzinResponse>, t: Throwable) {
                        if (_binding == null) return

                        Toast.makeText(requireContext(),"Bilgiler yüklenemedi.", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        })



        //euro için:
        servis.kurGetir("EUR", "TRY").enqueue(object : Callback<DovizResponse> {
            override fun onResponse(call: Call<DovizResponse>, response: Response<DovizResponse>) {
                if (_binding == null) return // bu internete daha erişemeden ekran vermesi gerektiği anda uygulamayı çöketmemesi için
                if (response.isSuccessful) {
                    val kur = response.body()?.rates?.get("TRY")
                    if (kur != null) {
                        binding.txtEuroApi.text = "$kur ₺"
                    }
                }
            }
            override fun onFailure(call: Call<DovizResponse>, t: Throwable) {
                //Hata olursa
                binding.txtEuroApi.text = "..."
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}