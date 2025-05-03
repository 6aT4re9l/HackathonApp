package com.example.hackathonapp

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class MapKid : Application() {
    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey("e7411f17-9e36-4296-b738-87a5b73c9a6e")
        MapKitFactory.initialize(this)
    }
}
