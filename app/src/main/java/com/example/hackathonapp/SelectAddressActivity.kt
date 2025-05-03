package com.example.hackathonapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class SelectAddressActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var addressInput: AutoCompleteTextView
    private lateinit var selectButton: Button

    private var selectedPoint: Point? = null
    private var selectedAddress: String? = null

    private val client = OkHttpClient()
    private val daDataToken = "cb38543110bd14eca20950383ef8dadbc82a9265"

    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_address)

        val initialAddress = intent.getStringExtra("address")
        val initialLat = intent.getDoubleExtra("latitude", 0.0)
        val initialLon = intent.getDoubleExtra("longitude", 0.0)

        mapView = findViewById(R.id.mapView)
        addressInput = findViewById(R.id.addressInput)
        addressInput.dropDownWidth = (resources.displayMetrics.widthPixels * 1.5).toInt()
        selectButton = findViewById(R.id.selectButton)

        adapter = ArrayAdapter(this, R.layout.item_address_suggestion, R.id.suggestionItem, mutableListOf())
        addressInput.setAdapter(adapter)
        addressInput.threshold = 1

        mapView.mapWindow.map.move(CameraPosition(Point(55.751244, 37.618423), 10.0f, 0.0f, 0.0f))


        if (!initialAddress.isNullOrBlank() && initialLat != 0.0 && initialLon != 0.0) {
            addressInput.setText(initialAddress)

            selectedPoint = Point(initialLat, initialLon)
            selectedAddress = initialAddress

            mapView.mapWindow.map.mapObjects.clear()
            mapView.mapWindow.map.mapObjects.addPlacemark(selectedPoint!!)
            mapView.mapWindow.map.move(CameraPosition(selectedPoint!!, 15.0f, 0.0f, 0.0f))
        }

        addressInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                val query = text.toString()
                if (query.length >= 3) loadSuggestions(query)
            }
        })

        addressInput.setOnItemClickListener { _, _, position, _ ->
            val selected = adapter.getItem(position)
            if (selected != null) {
                selectedAddress = selected
                geocodeViaDaData(selected)
            }
        }

        selectButton.setOnClickListener {
            if (selectedPoint != null && selectedAddress != null) {
                val intent = Intent().apply {
                    putExtra("latitude", selectedPoint!!.latitude)
                    putExtra("longitude", selectedPoint!!.longitude)
                    putExtra("address", selectedAddress)
                }
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
    }

    private fun loadSuggestions(query: String) {
        val request = Request.Builder()
            .url("https://suggestions.dadata.ru/suggestions/api/4_1/rs/suggest/address")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Token $daDataToken")
            .post(RequestBody.create(null, """{ "query": "$query" }"""))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("DaData", "Ошибка подсказки: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string() ?: return
                val suggestionsArray = JSONObject(json)
                    .getJSONArray("suggestions")

                val resultList = mutableListOf<String>()
                for (i in 0 until suggestionsArray.length()) {
                    val item = suggestionsArray.getJSONObject(i)
                    val value = item.getString("value")
                    resultList.add(value)
                }

                runOnUiThread {
                    adapter.clear()
                    adapter.addAll(resultList)
                    adapter.notifyDataSetChanged()
                    addressInput.showDropDown()
                }
            }
        })
    }

    private fun geocodeViaDaData(address: String) {
        val request = Request.Builder()
            .url("https://suggestions.dadata.ru/suggestions/api/4_1/rs/suggest/address")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Token $daDataToken")
            .post(RequestBody.create(null, """{ "query": "$address", "count": 1 }"""))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("DaData", "Ошибка геокодирования: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string() ?: return
                val obj = JSONObject(json)
                val data = obj
                    .getJSONArray("suggestions")
                    .optJSONObject(0)
                    ?.getJSONObject("data")

                val lat = data?.optString("geo_lat")?.toDoubleOrNull()
                val lon = data?.optString("geo_lon")?.toDoubleOrNull()

                if (lat != null && lon != null) {
                    val point = Point(lat, lon)
                    runOnUiThread {
                        selectedPoint = point
                        mapView.mapWindow.map.mapObjects.clear()
                        mapView.mapWindow.map.mapObjects.addPlacemark(point)
                        mapView.mapWindow.map.move(CameraPosition(point, 15.0f, 0.0f, 0.0f))
                    }
                } else {
                    Log.w("DaData", "Координаты не найдены")
                }
            }
        })
    }


    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
}
