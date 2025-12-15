package com.example.otovitrin.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.otovitrin.R
import com.example.otovitrin.databinding.FragmentIlanEklemeBinding
import com.example.otovitrin.view.ilanEklemeFragmentArgs
import com.example.otovitrin.model.Araba
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.IOException
import java.util.UUID

class ilanEklemeFragment : Fragment() {

    private var _binding: FragmentIlanEklemeBinding? = null
    private val binding get() = _binding!!

    private val args: ilanEklemeFragmentArgs by navArgs()
    private var gelenAraba: Araba? = null

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    var secilenGorsel: Uri? = null
    var secilenBitmap: Bitmap? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIlanEklemeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        registerLauncher()

        gelenAraba = args.duzenlenecekIlan
        if (gelenAraba != null) {
            // düzenleme modu için kutucukları dolduruyor
            eskiVerileriDoldur(gelenAraba!!)
            binding.btnIlanVer.text = "GÜNCELLE"
        } else {
            // ekleme modu
            binding.btnIlanVer.text = "İLAN VER"
        }

        binding.imgIlanResim.setOnClickListener {
            gorselSec(it)
        }

        binding.btnIlanVer.setOnClickListener {
            kaydetVeyaGuncelle()
        }
    }

    private fun eskiVerileriDoldur(araba: Araba) {
        binding.editBaslik.setText(araba.baslik)
        binding.editFiyat.setText(araba.fiyat.toString())
        binding.editKonum.setText(araba.konum)
        binding.editMarka.setText(araba.marka)
        binding.editModel.setText(araba.model)
        binding.editYil.setText(araba.yil.toString())
        binding.editKm.setText(araba.km.toString())
        binding.editTramer.setText(araba.tramer.toString())
        binding.editAciklama.setText(araba.aciklama)

        Glide.with(this).load(araba.gorselUrl).into(binding.imgIlanResim)
        binding.imgIlanResim.alpha = 1.0f
        binding.imgIlanResim.scaleType = ImageView.ScaleType.CENTER_CROP

        when (araba.yakit) {
            "Benzin" -> binding.radioBenzin.isChecked = true
            "Dizel" -> binding.radioDizel.isChecked = true
            "LPG" -> binding.radioLpg.isChecked = true
            "Elektrikli"-> binding.radioElektrikli.isChecked = true
        }
        when (araba.vites) {
            "Manuel" -> binding.radioManuel.isChecked = true
            "Otomatik" -> binding.radioOtomatik.isChecked = true
        }
    }

    private fun kaydetVeyaGuncelle() {
        val baslik = binding.editBaslik.text.toString().trim()
        val fiyatStr = binding.editFiyat.text.toString().trim()
        val konum = binding.editKonum.text.toString().trim()
        val marka = binding.editMarka.text.toString().trim()
        val model = binding.editModel.text.toString().trim()
        val yilStr = binding.editYil.text.toString().trim()
        val kmStr = binding.editKm.text.toString().trim()
        val tramerStr = binding.editTramer.text.toString().trim()
        val aciklama = binding.editAciklama.text.toString().trim()

        val secilenYakitId = binding.radioGroupYakit.checkedRadioButtonId
        val secilenVitesId = binding.radioGroupVites.checkedRadioButtonId

        if (gelenAraba == null && secilenGorsel == null) {
            Toast.makeText(requireContext(), "Lütfen bir fotoğraf seçin!", Toast.LENGTH_SHORT).show()
            return
        }

        if (baslik.isEmpty()) {
            binding.editBaslik.error = "Başlık gerekli"
            return
        }
        if (fiyatStr.isEmpty()) {
            binding.editFiyat.error = "Fiyat gerekli"
            return
        }
        if (konum.isEmpty()) {
            binding.editKonum.error = "Konum gerekli"
            return
        }
        if (marka.isEmpty()) {
            binding.editMarka.error = "Marka gerekli"
            return
        }
        if (model.isEmpty()) {
            binding.editModel.error = "Model gerekli"
            return
        }
        if (yilStr.isEmpty()) {
            binding.editYil.error = "Yıl gerekli"
            return
        }
        if (kmStr.isEmpty()) {
            binding.editKm.error = "KM gerekli"
            return
        }
        if (tramerStr.isEmpty()) {
            binding.editTramer.error = "Tramer gerekli (Yoksa 0 yazın)"
            return
        }
        if (aciklama.isEmpty()) {
            binding.editAciklama.error = "Açıklama gerekli"
            return
        }
        if (secilenYakitId == -1) {
            Toast.makeText(requireContext(), "Lütfen yakıt türü seçin!", Toast.LENGTH_SHORT).show()
            return
        }
        if (secilenVitesId == -1) {
            Toast.makeText(requireContext(), "Lütfen vites türü seçin!", Toast.LENGTH_SHORT).show()
            return
        }


        binding.btnIlanVer.isEnabled = false
        binding.btnIlanVer.text = "İşleniyor..."

        val yakit = try { binding.root.findViewById<RadioButton>(secilenYakitId).text.toString() } catch (e: Exception) { "" }
        val vites = try { binding.root.findViewById<RadioButton>(secilenVitesId).text.toString() } catch (e: Exception) { "" }
        val fiyat = fiyatStr.toIntOrNull() ?: 0
        val yil = yilStr.toIntOrNull() ?: 0
        val km = kmStr.toIntOrNull() ?: 0
        val tramer = tramerStr.toIntOrNull() ?: 0


        if (secilenGorsel != null) {
            val uuid = UUID.randomUUID()
            val gorselAdi = "${uuid}.jpg"
            val reference = storage.reference.child("ilanGorselleri").child(gorselAdi)

            reference.putFile(secilenGorsel!!).addOnSuccessListener {
                reference.downloadUrl.addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()
                    // yeni URL ile kaydet
                    firestoreIslemiYap(baslik, fiyat, konum, marka, model, yil, km, yakit, vites, tramer, aciklama, downloadUrl)
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Resim Yüklenemedi!", Toast.LENGTH_SHORT).show()
                binding.btnIlanVer.isEnabled = true
                binding.btnIlanVer.text = "TEKRAR DENE"
            }
        }
        //yeni resim seçilmedi ise eski foto kullanılacak
        else if (gelenAraba != null) {
            firestoreIslemiYap(baslik, fiyat, konum, marka, model, yil, km, yakit, vites, tramer, aciklama, gelenAraba!!.gorselUrl ?: "")
        }
    }

    private fun firestoreIslemiYap(
        baslik: String, fiyat: Int, konum: String, marka: String, model: String,
        yil: Int, km: Int, yakit: String, vites: String, tramer: Int, aciklama: String, gorselUrl: String
    ) {
        val kullaniciId = auth.currentUser!!.uid

        val ilanMap: HashMap<String, Any> = hashMapOf(
            "baslik" to baslik,
            "fiyat" to fiyat,
            "konum" to konum,
            "marka" to marka,
            "model" to model,
            "yil" to yil,
            "km" to km,
            "yakit" to yakit,
            "vites" to vites,
            "tramer" to tramer,
            "aciklama" to aciklama,
            "gorselUrl" to gorselUrl,
            "kullaniciId" to kullaniciId,
            // Güncelleme ise eski tarihi koru yoksa yeni tarih al olarak ayarlıyoruz
            "ilanTarihi" to (gelenAraba?.ilanTarihi ?: Timestamp.now())
        )

        if (gelenAraba != null) {
            firestore.collection("Ilanlar").document(gelenAraba!!.ilanId!!).update(ilanMap)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "İlan Güncellendi!", Toast.LENGTH_LONG).show()
                        findNavController().navigate(R.id.action_ilanEklemeFragment_to_anaSayfaFragment)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    binding.btnIlanVer.isEnabled = true
                    binding.btnIlanVer.text = "GÜNCELLE"
                }
        } else {
            // Yeni kayıt
            firestore.collection("Ilanlar").add(ilanMap)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "İlan Eklendi!", Toast.LENGTH_LONG).show()
                    try {
                        findNavController().navigate(R.id.action_ilanEklemeFragment_to_anaSayfaFragment)
                    } catch (e: Exception){
                        findNavController().popBackStack()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    binding.btnIlanVer.isEnabled = true
                    binding.btnIlanVer.text = "İLAN VER"
                }
        }
    }

    //görsel almak için izin kısmı
    private fun gorselSec(view: View) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permission)) {
                Snackbar.make(view, "Galeriye gitmek için izin lazım", Snackbar.LENGTH_INDEFINITE)
                    .setAction("İzin Ver") { permissionLauncher.launch(permission) }.show()
            } else { permissionLauncher.launch(permission) }
        } else {
            val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)
        }
    }

    private fun registerLauncher() {
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intentFromResult = result.data
                if (intentFromResult != null) {
                    secilenGorsel = intentFromResult.data
                    try {
                        val contentResolver = requireActivity().contentResolver
                        if (Build.VERSION.SDK_INT >= 28) {
                            val source = ImageDecoder.createSource(contentResolver, secilenGorsel!!)
                            secilenBitmap = ImageDecoder.decodeBitmap(source)
                        } else {
                            secilenBitmap = MediaStore.Images.Media.getBitmap(contentResolver, secilenGorsel)
                        }
                        binding.imgIlanResim.setImageBitmap(secilenBitmap)
                        binding.imgIlanResim.alpha = 1.0f
                        binding.imgIlanResim.scaleType = ImageView.ScaleType.CENTER_CROP
                    } catch (e: IOException) { e.printStackTrace() }
                }
            }
        }
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            } else { Toast.makeText(requireContext(), "İzin verilmedi!", Toast.LENGTH_LONG).show() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}