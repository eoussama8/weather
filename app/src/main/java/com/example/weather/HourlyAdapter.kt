package com.example.weather

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class HourlyAdapter(
    private val times: List<String>,
    private val temperatures: List<Double>,
    private val conditions: List<String>, // Weather conditions for each hour
    private val getWeatherIcon: (String) -> Int // Lambda to map conditions to icons
) : RecyclerView.Adapter<HourlyAdapter.HourlyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_view_hours, parent, false)
        return HourlyViewHolder(view)
    }

    override fun onBindViewHolder(holder: HourlyViewHolder, position: Int) {
        val fullDateTime = times[position]
        val time = fullDateTime.substringAfter("T") // Extract time (e.g., "16:00") from "2024-11-30T16:00"
        holder.timeTextView.text = time
        holder.temperatureTextView.text = "${temperatures[position]}°"

        val weatherCondition = conditions[position] // Get weather condition for the hour
        val weatherIcon = getWeatherIcon(weatherCondition) // Get corresponding icon

        Glide.with(holder.itemView.context)
            .load(weatherIcon) // Load the icon image into ImageView
            .into(holder.weatherImageView)
    }

    override fun getItemCount(): Int = times.size

    class HourlyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeTextView: TextView = view.findViewById(R.id.textView2)
        val temperatureTextView: TextView = view.findViewById(R.id.textView)
        val weatherImageView: ImageView = view.findViewById(R.id.imageview)
    }
}
