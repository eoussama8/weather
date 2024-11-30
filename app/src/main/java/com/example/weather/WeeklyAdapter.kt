package com.example.weather

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class WeeklyAdapter(
    private val days: List<String>,
    private val temperatures: List<Int>,
    private val conditions: List<String>, // Weather conditions for each day
    private val getWeatherIcon: (String) -> Int // Lambda to map conditions to icons
) : RecyclerView.Adapter<WeeklyAdapter.WeeklyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeeklyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_view_days, parent, false)
        return WeeklyViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeeklyViewHolder, position: Int) {
        holder.dayTextView.text = days[position] // Set the day name
        holder.temperatureTextView.text = "${temperatures[position]}°" // Set the temperature

        val weatherCondition = conditions[position] // Get weather condition for the day
        val weatherIcon = getWeatherIcon(weatherCondition) // Get corresponding icon

        Glide.with(holder.itemView.context)
            .load(weatherIcon) // Load the icon into the ImageView
            .into(holder.weatherImageView)
    }

    override fun getItemCount(): Int = days.size

    class WeeklyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dayTextView: TextView = view.findViewById(R.id.days)
        val temperatureTextView: TextView = view.findViewById(R.id.tmp)
        val weatherImageView: ImageView = view.findViewById(R.id.icon_weather)
    }
}
