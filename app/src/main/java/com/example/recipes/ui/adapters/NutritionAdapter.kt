package com.example.recipes.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recipes.R
import com.example.recipes.data.model.Nutrient

class NutritionAdapter(
    private val items: List<Nutrient>
) : RecyclerView.Adapter<NutritionAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nameView: TextView = view.findViewById(R.id.nutrientName)
        private val amountView: TextView = view.findViewById(R.id.nutrientAmount)

        fun bind(nutrient: Nutrient) {
            nameView.text = "${nutrient.name}:"
            amountView.text = "${nutrient.amount} ${nutrient.unit}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nutrient, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position])

    override fun getItemCount() = items.size
}
