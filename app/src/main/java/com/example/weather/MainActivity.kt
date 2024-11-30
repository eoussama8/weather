package com.example.weather

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var temperatureTextView: TextView
    private lateinit var cityNameTextView: TextView
    private lateinit var hourlyRecyclerView: RecyclerView
    private lateinit var weeklyRecyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationReceiver: LocationReceiver

    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        temperatureTextView = findViewById(R.id.temperature)
        cityNameTextView = findViewById(R.id.cityName)
        hourlyRecyclerView = findViewById(R.id.recyclerview)
        weeklyRecyclerView = findViewById(R.id.daily_recyclerview)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        hourlyRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        weeklyRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize the LocationReceiver with a callback to fetch weather data on receiving location
        locationReceiver = LocationReceiver { latitude, longitude ->
            // Fetch city name from latitude and longitude and then fetch weather data
            val cityName = getCityName(latitude, longitude)
            fetchWeatherData(latitude, longitude, cityName)
        }

        // Register the receiver to listen for location updates
        val filter = IntentFilter("com.example.weather.LOCATION_UPDATED")
        registerReceiver(locationReceiver, filter)

        // Check location permission
        checkLocationPermission()

        // Set up swipe-to-refresh functionality
        swipeRefreshLayout.setOnRefreshListener {
            // Fetch the latest location and refresh the weather data
            getCurrentLocation()
        }
    }

    // Method to check and request location permission
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    // Handle permission request results
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Get current location
    private fun getCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                // Broadcast the location
                broadcastLocation(latitude, longitude)
            } else {
                Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Broadcast location
    private fun broadcastLocation(latitude: Double, longitude: Double) {
        val intent = Intent("com.example.weather.LOCATION_UPDATED")
        intent.putExtra("latitude", latitude)
        intent.putExtra("longitude", longitude)
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the receiver to avoid memory leaks
        unregisterReceiver(locationReceiver)
    }

    // Fetch weather data from API
    private fun fetchWeatherData(lat: Double, lon: Double, cityName: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(WeatherApi::class.java)

        lifecycleScope.launch {
            try {
                // Fetching weather data from the API
                val response = withContext(Dispatchers.IO) {
                    api.getWeatherAlerts(lat, lon)
                }

                updateUI(response, cityName)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error fetching data: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                // Stop the refresh animation
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun updateUI(data: Mydata, cityName: String) {
        // Display the city name
        cityNameTextView.text = cityName
        val currentTemperature = data.hourly.temperature_2m.firstOrNull() ?: 0.0
        temperatureTextView.text = "${currentTemperature.toInt()}°"

        // Hourly Data
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val times = data.hourly.time.filter { it.startsWith(currentDate) }
        val hourlyTemperatures = data.hourly.temperature_2m.take(times.size)

        // Inferred weather conditions based on temperature (since we don't have explicit conditions)
        val hourlyConditions = hourlyTemperatures.map { inferWeatherCondition(it) }

        // Hourly adapter setup
        val hourlyAdapter = HourlyAdapter(times, hourlyTemperatures, hourlyConditions) { condition ->
            getWeatherIcon(condition)
        }
        hourlyRecyclerView.adapter = hourlyAdapter

        // Weekly Data (aggregated from hourly data)
        val weeklyData = processHourlyDataForWeekly(data.hourly)
        val weeklyAdapter = WeeklyAdapter(
            weeklyData.map { it.first },
            weeklyData.map { it.second },
            weeklyData.map { it.third }
        ) { condition ->
            getWeatherIcon(condition)
        }
        weeklyRecyclerView.adapter = weeklyAdapter
    }

    // This function infers weather conditions based on temperature ranges
    private fun inferWeatherCondition(temperature: Double): String {
        return when {
            temperature < 0 -> "snow" // Below freezing
            temperature in 0.0..15.0 -> "cloudy" // Cold temperatures
            temperature in 15.1..25.0 -> "clear" // Moderate temperatures
            else -> "rain" // High temperatures (could be rainy or hot)
        }
    }

    // Function to determine the weather icon based on the inferred condition
    private fun getWeatherIcon(condition: String): Int {
        return when (condition.lowercase()) {
            "clear" -> R.drawable.sun
            "cloudy" -> R.drawable.cloud
            "snow" -> R.drawable.snow
            "rain" -> R.drawable.rain
            else -> R.drawable.warning // Default icon for unknown conditions
        }
    }

    // This function aggregates hourly data to daily data (maximum temperature for each day)
    private fun processHourlyDataForWeekly(hourly: Hourly): List<Triple<String, Int, String>> {
        val dailyData = mutableMapOf<String, MutableList<Pair<Double, String>>>()

        for (i in hourly.time.indices) {
            val date = hourly.time[i].substringBefore("T") // Extract the date (e.g., "2024-11-30")
            val temperature = hourly.temperature_2m[i]
            val condition = inferWeatherCondition(temperature) // Use the inferred condition
            dailyData.getOrPut(date) { mutableListOf() }.add(Pair(temperature, condition))
        }

        return dailyData.map { (date, entries) ->
            val maxTemp = entries.maxOfOrNull { it.first }?.toInt() ?: 0
            val mostFrequentCondition = entries.groupingBy { it.second }.eachCount().maxByOrNull { it.value }?.key ?: "unknown"
            Triple(date, maxTemp, mostFrequentCondition)
        }
    }

    // Function to get the city name from latitude and longitude using Geocoder
    private fun getCityName(latitude: Double, longitude: Double): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        return addresses?.firstOrNull()?.locality ?: "Unknown City"
    }
}
