package com.example.hackathonapp

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.hackathonapp.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.io.File
import java.io.FileOutputStream
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var hackathonAdapter: HackathonAdapter
    private val db = FirebaseFirestore.getInstance()
    private lateinit var binding: ActivityMainBinding
    private var selectedImageUri: Uri? = null
    private var imageViewPreviewDialog: ImageView? = null
    private var isAdmin = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fetchUserRole()

        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        binding.fabAddHackathon.setOnClickListener {
            showAddHackathonDialog()
        }
    }

    private fun setupFilters() {
        val cities = listOf("Все", "Москва", "Санкт-Петербург", "Новосибирск")
        val types = listOf("Все", "Онлайн", "Офлайн", "Гибрид")

        val cityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, cities)
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)

        binding.spinnerCity.adapter = cityAdapter
        binding.spinnerType.adapter = typeAdapter

        val filterListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCity = if (binding.spinnerCity.selectedItem == "Все") null else binding.spinnerCity.selectedItem.toString()
                val selectedType = if (binding.spinnerType.selectedItem == "Все") null else binding.spinnerType.selectedItem.toString()
                loadHackathons(selectedCity, selectedType)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerCity.onItemSelectedListener = filterListener
        binding.spinnerType.onItemSelectedListener = filterListener
    }

    private fun loadHackathons(city: String?, type: String?) {
        var query: Query = db.collection("hackathons")
        if (city != null) query = query.whereEqualTo("city", city)
        if (type != null) query = query.whereEqualTo("type", type)

        query.get().addOnSuccessListener { documents ->
            val hackathonList = documents.mapNotNull { it.toObject(Hackathon::class.java) }

            hackathonAdapter.submitList(hackathonList)

            // Если нет фильтрации по городу и типу — обновляем список городов
            if (city == null && type == null) {
                val uniqueCities = hackathonList.map { it.city }.distinct().sorted()
                val cityFilterList = listOf("Все") + uniqueCities
                val cityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, cityFilterList)
                binding.spinnerCity.adapter = cityAdapter
            }
        }
    }

    private fun showAddHackathonDialog() {

        val popularCities = listOf(
            "Москва", "Санкт-Петербург", "Новосибирск", "Екатеринбург", "Казань",
            "Нижний Новгород", "Челябинск", "Самара", "Ростов-на-Дону", "Уфа"
        )
        val cityAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, popularCities)

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_hackathon, null)
        val editTitle = dialogView.findViewById<EditText>(R.id.editTitle)
        val editDescription = dialogView.findViewById<EditText>(R.id.editDescription)
        val editCity = dialogView.findViewById<AutoCompleteTextView>(R.id.editCity)
        val editPrizeFund = dialogView.findViewById<EditText>(R.id.editPrizeFund)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerType)
        val buttonSelectImage = dialogView.findViewById<Button>(R.id.buttonSelectImage)
        imageViewPreviewDialog = dialogView.findViewById(R.id.imageViewPreview)
        editCity.setAdapter(cityAdapter)
        val hackathonTypes = listOf("Онлайн", "Офлайн", "Гибрид")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, hackathonTypes)
        spinnerType.adapter = adapter

        buttonSelectImage.setOnClickListener {
            openGallery()
        }


        val dialog = AlertDialog.Builder(this)
            .setTitle("Добавить хакатон")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val title = editTitle.text.toString()
                val description = editDescription.text.toString()
                val city = editCity.text.toString()
                val prizeFund = editPrizeFund.text.toString().toIntOrNull() ?: 0
                val type = spinnerType.selectedItem.toString()

                if (title.isNotEmpty() && description.isNotEmpty() && city.isNotEmpty() && selectedImageUri != null) {
                    val localImagePath = saveImageToInternalStorage(selectedImageUri!!)
                    if (localImagePath != null) {
                        saveHackathonToFirestore(title, description, city, prizeFund, type, localImagePath)
                        selectedImageUri = null
                    } else {
                        Toast.makeText(this, "Ошибка при сохранении изображения", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Заполните все поля!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryLauncher.launch(intent)
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            selectedImageUri = result.data?.data
            if (selectedImageUri != null) {
                imageViewPreviewDialog?.apply {
                    visibility = ImageView.VISIBLE
                    Glide.with(context).load(selectedImageUri).into(this)
                }
            }
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val dir = File(filesDir, "hackathon_images")
            if (!dir.exists()) dir.mkdir()
            val file = File(dir, fileName)
            FileOutputStream(file).use { output ->
                inputStream.copyTo(output)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun deleteImageFromStorage(imagePath: String) {
        val file = File(imagePath)
        if (file.exists()) file.delete()
    }

    fun deleteHackathon(hackathon: Hackathon) {
        db.collection("hackathons").document(hackathon.id).delete()
            .addOnSuccessListener {
                deleteImageFromStorage(hackathon.imageURL)
                Toast.makeText(this, "Хакатон удалён", Toast.LENGTH_SHORT).show()
                loadHackathons(null, null)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка при удалении", Toast.LENGTH_SHORT).show()
            }
    }
    private fun showEditHackathonDialog(hackathon: Hackathon) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_hackathon, null)
        val editTitle = dialogView.findViewById<EditText>(R.id.editTitle)
        val editDescription = dialogView.findViewById<EditText>(R.id.editDescription)
        val editCity = dialogView.findViewById<AutoCompleteTextView>(R.id.editCity)
        val editPrizeFund = dialogView.findViewById<EditText>(R.id.editPrizeFund)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerType)
        val buttonSelectImage = dialogView.findViewById<Button>(R.id.buttonSelectImage)
        imageViewPreviewDialog = dialogView.findViewById(R.id.imageViewPreview)

        editTitle.setText(hackathon.title)
        editDescription.setText(hackathon.description)
        editCity.setText(hackathon.city)
        editPrizeFund.setText(hackathon.prizeFund.toString())

        val hackathonTypes = listOf("Онлайн", "Офлайн", "Гибрид")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, hackathonTypes)
        spinnerType.adapter = adapter
        val selectedIndex = hackathonTypes.indexOf(hackathon.type)
        if (selectedIndex >= 0) spinnerType.setSelection(selectedIndex)

        val imageFile = File(hackathon.imageURL)
        if (imageFile.exists()) {
            imageViewPreviewDialog?.apply {
                visibility = ImageView.VISIBLE
                Glide.with(this).load(imageFile).into(this)
            }
        }

        buttonSelectImage.setOnClickListener {
            openGallery()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Редактировать хакатон")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val title = editTitle.text.toString()
                val description = editDescription.text.toString()
                val city = editCity.text.toString()
                val prizeFund = editPrizeFund.text.toString().toIntOrNull() ?: 0
                val type = spinnerType.selectedItem.toString()

                val newImagePath = if (selectedImageUri != null) {
                    saveImageToInternalStorage(selectedImageUri!!)?.also {
                        deleteImageFromStorage(hackathon.imageURL)
                    } ?: hackathon.imageURL
                } else {
                    hackathon.imageURL
                }

                val updated = mapOf(
                    "title" to title,
                    "description" to description,
                    "city" to city,
                    "prizeFund" to prizeFund,
                    "type" to type,
                    "imageURL" to newImagePath
                )

                db.collection("hackathons").document(hackathon.id).update(updated)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Хакатон обновлён", Toast.LENGTH_SHORT).show()
                        loadHackathons(null, null)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Ошибка обновления", Toast.LENGTH_SHORT).show()
                    }

                selectedImageUri = null
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
    }

    private fun saveHackathonToFirestore(title: String, description: String, city: String, prizeFund: Int, type: String, imageURL: String) {
        val newDocRef = db.collection("hackathons").document()
        val hackathon = hashMapOf(
            "id" to newDocRef.id,
            "title" to title,
            "description" to description,
            "city" to city,
            "prizeFund" to prizeFund,
            "type" to type,
            "imageURL" to imageURL
        )

        newDocRef.set(hackathon)
            .addOnSuccessListener {
                Toast.makeText(this, "Хакатон добавлен!", Toast.LENGTH_SHORT).show()
                loadHackathons(null, null)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка при добавлении!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUIByRole() {
        binding.fabAddHackathon.visibility = if (isAdmin) View.VISIBLE else View.GONE
    }


    private fun fetchUserRole() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                isAdmin = doc.getBoolean("isAdmin") == true
                Log.d("FIREBASE_ROLE", "UID=$userId, isAdmin=$isAdmin")

                updateUIByRole()

                hackathonAdapter = HackathonAdapter(
                    isAdmin = isAdmin,
                    onDelete = { deleteHackathon(it) },
                    onEdit = { showEditHackathonDialog(it) }
                )

                binding.recyclerView.layoutManager = LinearLayoutManager(this)
                binding.recyclerView.adapter = hackathonAdapter
                setupFilters()
                loadHackathons(null, null) // 🔽 перемещено сюда
            }

    }


}
