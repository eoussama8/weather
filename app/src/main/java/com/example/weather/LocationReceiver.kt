package com.example.weather

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class LocationReceiver(private val onLocationReceived: (latitude: Double, longitude: Double) -> Unit) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        // Retrieve latitude and longitude from the intent. Default to 0.0 if not found.
        val latitude = intent?.getDoubleExtra("latitude", 0.0) ?: 0.0
        val longitude = intent?.getDoubleExtra("longitude", 0.0) ?: 0.0

        // Check if valid location data is received
        if (latitude != 0.0 && longitude != 0.0) {
            // Pass the location to the callback function
            onLocationReceived(latitude, longitude)

            // Optionally, log the received location
            android.util.Log.d("LocationReceiver", "Received Location: Latitude: $latitude, Longitude: $longitude")
        } else {
            // Show a toast message if the location data is invalid
            context?.let {
                Toast.makeText(it, "Invalid location data received", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
