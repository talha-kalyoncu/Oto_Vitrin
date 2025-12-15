package com.example.otovitrin.view

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.otovitrin.databinding.FragmentIlanGoruntulemeBinding
import com.example.otovitrin.view.ilanGoruntulemeFragmentArgs
import com.example.otovitrin.view.ilanGoruntulemeFragmentDirections
import com.example.otovitrin.model.Araba
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Locale

class ilanGoruntulemeFragment : Fragment() {

    private var _binding: FragmentIlanGoruntulemeBinding? = null
    private val binding get() = _binding!!

    private val args: ilanGoruntulemeFragmentArgs by navArgs()

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var isFavori = false

    private var saticiTelefonu: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIlanGoruntulemeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val gelenAraba = args.secilenAraba

        if (gelenAraba != null) {
            verileriEkranaBas(gelenAraba)
            saticiBilgileriniGetir(gelenAraba.kullaniciId)
            favoriDurumunuKontrolEt(gelenAraba.ilanId)

            val benimId = auth.currentUser?.uid

            // hepsini önce gizle gizliyoruz
            binding.layoutSahipButonlar.visibility = View.GONE
            binding.iletisimLinearLayout.visibility = View.GONE
            binding.layoutAdminButonlar.visibility = View.GONE

            if (benimId != null) {
                if (benimId == gelenAraba.kullaniciId) {
                    binding.layoutSahipButonlar.visibility = View.VISIBLE
                } else {
                    firestore.collection("Users").document(benimId).get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.getString("rol") == "Admin") {
                                // adminsem -> admin butonlarını göster
                                binding.layoutAdminButonlar.visibility = View.VISIBLE
                            } else {
                                // admin  değilsem -> iletişim göster
                                binding.iletisimLinearLayout.visibility = View.VISIBLE
                            }
                        }
                }
            } else {
                //hata olursa da -> iletişim göste
                binding.iletisimLinearLayout.visibility = View.VISIBLE
            }
        }

        binding.btnFavori.setOnClickListener {
            favoriIslemiYap(gelenAraba?.ilanId)
        }

        binding.btnIlanSil.setOnClickListener {
            ilanSil(gelenAraba?.ilanId)
        }

        binding.btnIlanDuzenle.setOnClickListener {
            if (gelenAraba != null) {
                val action = ilanGoruntulemeFragmentDirections.Companion.actionIlanGoruntulemeFragmentToIlanEklemeFragment(gelenAraba)
                findNavController().navigate(action)
            }
        }

        binding.btnMailGonder.setOnClickListener {
            val saticiMail = binding.txtSaticiMail.text.toString()


            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$saticiMail")
                putExtra(Intent.EXTRA_SUBJECT, "${gelenAraba?.baslik} ilanı hakkında")
            }

            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Mail uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnNumaraGonder.setOnClickListener {
            if (!saticiTelefonu.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:$saticiTelefonu")
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Arama yapılamıyor.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Satıcının numarası bulunamadı.", Toast.LENGTH_SHORT).show()
            }
        }


        binding.btnAdminIlanSil.setOnClickListener {
            ilanSil(gelenAraba?.ilanId)
        }

        binding.btnAdminKullaniciSil.setOnClickListener {
            kullaniciyiBanla(gelenAraba?.kullaniciId, gelenAraba?.ilanId)
        }
    }
    private fun kullaniciyiBanla(userId: String?, ilanId: String?) {
        if (userId == null) return

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("KULLANICIYI BANLA")
        builder.setMessage("DİKKAT! Bu işlem:\n1. Kullanıcının yayınladığı TÜM ilanları silecek.\n2. Kullanıcının üyeliğini kalıcı olarak silecek.\n\nBu işlem geri alınamaz. Onaylıyor musun?")

        builder.setPositiveButton("EVET, BANLA") { _, _ ->

            //tüm ilanlarını bulamamız gerek
            firestore.collection("Ilanlar")
                .whereEqualTo("kullaniciId", userId)
                .get()
                .addOnSuccessListener { documents ->

                    // Firestore Batch başlatıyoruz
                    // bu sayede hepsini tek seferde silebiliriz
                    val batch = firestore.batch()

                    // bulunan her ilanı silme listesine ekliyoruz
                    for (document in documents) {
                        batch.delete(document.reference)
                    }

                    // kullanıcının kendisini de silme listesine ekleyerek komple hem ilanlarını hem de kullanıcıyı siliyoruz
                    val userRef = firestore.collection("Users").document(userId)
                    batch.delete(userRef)

                    //ve son olarak silme işlemii
                    batch.commit().addOnSuccessListener {
                        Toast.makeText(requireContext(), "Kullanıcı ve ${documents.size()} adet ilanı silindi.", Toast.LENGTH_LONG).show()
                        findNavController().popBackStack() // Geri dön
                    }.addOnFailureListener {
                        Toast.makeText(requireContext(), "Silme işleminde hata: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "İlanlar aranırken hata oluştu.", Toast.LENGTH_LONG).show()
                }
        }
        builder.setNegativeButton("İPTAL", null)
        builder.show()
    }

    private fun verileriEkranaBas(araba: Araba) {
        binding.txtDetayBaslik.text = araba.baslik
        binding.txtDetayFiyat.text = "${formatla((araba.fiyat ?: 0).toLong())} ₺"

        binding.txtDetayKonum.text = "📍 ${araba.konum}"
        binding.txtDetayAciklama.text = araba.aciklama

        binding.txtDetayMarka.text = araba.marka
        binding.txtDetayModel.text = araba.model
        binding.txtDetayYil.text = araba.yil.toString()
        binding.txtDetayKm.text = "${formatla((araba.km ?: 0).toLong())} km"
        binding.txtDetayYakit.text = araba.yakit
        binding.txtDetayVites.text = araba.vites
        binding.txtDetayTramer.text = "${formatla((araba.tramer ?: 0).toLong())} ₺"

        if (araba.ilanTarihi != null) {
            val date = araba.ilanTarihi.toDate()
            val format = SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR"))
            binding.txtDetayTarih.text = "🗓️ ${format.format(date)}"
        }

        Glide.with(this).load(araba.gorselUrl).into(binding.imgDetayResim)
    }

    private fun saticiBilgileriniGetir(kullaniciId: String?) {
        if (kullaniciId != null) {
            firestore.collection("Users").document(kullaniciId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        binding.txtSaticiAdSoyad.text = document.getString("adSoyad")
                        binding.txtSaticiMail.text = document.getString("email")
                        saticiTelefonu = document.getString("telefon")
                    }
                }
        }
    }

    private fun favoriDurumunuKontrolEt(ilanId: String?) {
        val userId = auth.currentUser?.uid
        if (userId != null && ilanId != null) {
            firestore.collection("Users").document(userId)
                .collection("Favoriler").document(ilanId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        isFavori = true
                        binding.btnFavori.setColorFilter(Color.parseColor("#FF9800"))
                    } else {
                        isFavori = false
                        binding.btnFavori.setColorFilter(Color.parseColor("#B0BEC5"))
                    }
                }
        }
    }

    private fun favoriIslemiYap(ilanId: String?) {
        val userId = auth.currentUser?.uid
        if (userId != null && ilanId != null) {
            if (isFavori) {
                firestore.collection("Users").document(userId)
                    .collection("Favoriler").document(ilanId).delete()
                    .addOnSuccessListener {
                        isFavori = false
                        binding.btnFavori.setColorFilter(Color.parseColor("#B0BEC5"))
                        Toast.makeText(requireContext(), "Favorilerden çıkarıldı", Toast.LENGTH_SHORT).show()
                    }
            } else {
                val data = hashMapOf("eklenmeTarihi" to Timestamp.now())
                firestore.collection("Users").document(userId)
                    .collection("Favoriler").document(ilanId).set(data)
                    .addOnSuccessListener {
                        isFavori = true
                        binding.btnFavori.setColorFilter(Color.parseColor("#FF9800"))
                        Toast.makeText(requireContext(), "Favorilere eklendi", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun ilanSil(ilanId: String?) {
        if (ilanId != null) {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("İlanı Sil")
            builder.setMessage("Bu ilanı kalıcı olarak silmek istediğine emin misin?")
            builder.setPositiveButton("EVET, SİL") { _, _ ->
                firestore.collection("Ilanlar").document(ilanId).delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "İlan silindi.", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
            }
            builder.setNegativeButton("İPTAL", null)
            builder.show()
        }
    }
    private fun formatla(fiyat: Long): String {
        val semboller = DecimalFormatSymbols()
        semboller.groupingSeparator = '.' // Binlik ayracı nokta olsun
        val formatter = DecimalFormat("#,###", semboller)
        return formatter.format(fiyat)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}