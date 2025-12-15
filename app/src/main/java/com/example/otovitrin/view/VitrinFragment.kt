package com.example.otovitrin.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.otovitrin.adapter.ArabaAdapter
import com.example.otovitrin.databinding.FragmentVitrinBinding
import com.example.otovitrin.model.Araba
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class VitrinFragment : Fragment(), FiltreListener {

    private var _binding: FragmentVitrinBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var arabaListesi: ArrayList<Araba>
    private lateinit var arabaAdapter: ArabaAdapter
    private var snapshotListener: ListenerRegistration? = null
    private var favoriIlanIdleri = ArrayList<String>()
    private var sonSiralama = "TarihYeniden"
    private var sonMinFiyat = 0
    private var sonMaxFiyat = Int.MAX_VALUE
    private var sonKendiIlanim = false
    private var sonFavori = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVitrinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        arabaListesi = ArrayList()

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        arabaAdapter = ArabaAdapter(arabaListesi)
        binding.recyclerView.adapter = arabaAdapter

        // Başlangıç verilerini getir
        verileriGetir(sonSiralama, sonMinFiyat, sonMaxFiyat, sonKendiIlanim, sonFavori)

        binding.btnFiltre.setOnClickListener {
            val filterDialog = filter.newInstance(
                this,
                sonSiralama,
                sonMinFiyat,
                sonMaxFiyat,
                sonKendiIlanim,
                sonFavori
            )
            filterDialog.show(parentFragmentManager, "filterDialog")
        }
    }

    override fun filtreleriUygula(
        siralama: String, minFiyat: Int, maxFiyat: Int,
        sadeceKendiIlanlarim: Boolean, sadeceFavoriler: Boolean
    ) {
        // seçimleri hafızaya alma işlemi
        sonSiralama = siralama
        sonMinFiyat = minFiyat
        sonMaxFiyat = maxFiyat
        sonKendiIlanim = sadeceKendiIlanlarim
        sonFavori = sadeceFavoriler

        verileriGetir(siralama, minFiyat, maxFiyat, sadeceKendiIlanlarim, sadeceFavoriler)
    }

    private fun verileriGetir(
        siralama: String, min: Int, max: Int,
        kendiIlanim: Boolean, favori: Boolean
    ) {
        snapshotListener?.remove()

        val userId = auth.currentUser?.uid

        //sadece favoriler seçiliyse önce favori ID lerini çekmemiz lazım
        if (favori && userId != null) {
            firestore.collection("Users").document(userId)
                .collection("Favoriler").get()
                .addOnSuccessListener { documents ->
                    favoriIlanIdleri.clear()
                    for (doc in documents) {
                        favoriIlanIdleri.add(doc.id) // ID leri listeye at
                    }
                    // ID leri aldıktan sonra asıl ilanları çekmeye git
                    ilanlariFirestoreDanCek(siralama, min, max, kendiIlanim, true)
                }
        } else {
            // favori seçili değilse direkt ilanları çek
            ilanlariFirestoreDanCek(siralama, min, max, kendiIlanim, false)
        }
    }

    // Asıl veri çekme fonksiyonu
    private fun ilanlariFirestoreDanCek(
        siralama: String, min: Int, max: Int,
        kendiIlanim: Boolean, favoriModu: Boolean
    ) {
        var sorgu: Query = firestore.collection("Ilanlar")

        // kendi ilanlarım filtresi
        if (kendiIlanim) {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                sorgu = sorgu.whereEqualTo("kullaniciId", userId)
            }
        }

        // sıralama
        sorgu = when (siralama) {
            "FiyatArtan" -> sorgu.orderBy("fiyat", Query.Direction.ASCENDING)
            "FiyatAzalan" -> sorgu.orderBy("fiyat", Query.Direction.DESCENDING)
            else -> sorgu.orderBy("ilanTarihi", Query.Direction.DESCENDING)
        }

        snapshotListener = sorgu.addSnapshotListener { value, error ->
            if (error != null) {
                return@addSnapshotListener
            }

            if (value != null) {
                arabaListesi.clear()

                for (document in value.documents) {
                    val ilanId = document.id


                    if (favoriModu && !favoriIlanIdleri.contains(ilanId)) {
                        continue
                    }


                    val baslik = document.getString("baslik") ?: ""
                    val fiyat = document.getLong("fiyat")?.toInt() ?: 0
                    val konum = document.getString("konum") ?: ""
                    val marka = document.getString("marka") ?: ""
                    val model = document.getString("model") ?: ""
                    val yil = document.getLong("yil")?.toInt() ?: 0
                    val km = document.getLong("km")?.toInt() ?: 0
                    val yakit = document.getString("yakit") ?: ""
                    val vites = document.getString("vites") ?: ""
                    val tramer = document.getLong("tramer")?.toInt() ?: 0
                    val aciklama = document.getString("aciklama") ?: ""
                    val gorselUrl = document.getString("gorselUrl") ?: ""
                    val kullaniciId = document.getString("kullaniciId") ?: ""
                    val ilanTarihi = document.getTimestamp("ilanTarihi")

                    // fiyat filtresi
                    if (fiyat >= min && fiyat <= max) {
                        val araba = Araba(
                            ilanId, baslik, fiyat, konum, marka, model, yil, km,
                            yakit, vites, tramer, aciklama, gorselUrl, kullaniciId, ilanTarihi
                        )
                        arabaListesi.add(araba)
                    }
                }
                arabaAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        snapshotListener?.remove()
        _binding = null
    }
}