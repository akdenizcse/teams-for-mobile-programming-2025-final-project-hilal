// app/src/main/java/com/example/recipes/ui/adapters/IngredientsAdapter.kt
package com.example.recipes.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recipes.R
import com.example.recipes.data.model.IngredientWithPrice

class IngredientsAdapter(
    private val onClick: (IngredientWithPrice) -> Unit
) : RecyclerView.Adapter<IngredientsAdapter.VH>() {

    private val data = mutableListOf<IngredientWithPrice>()

    fun submit(list: List<IngredientWithPrice>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(
        LayoutInflater.from(p.context).inflate(R.layout.item_ingredient, p, false)
    )

    override fun getItemCount() = data.size

    override fun onBindViewHolder(h: VH, i: Int) = h.bind(data[i])

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val name: TextView = v.findViewById(R.id.ingName)
        private val price: TextView = v.findViewById(R.id.ingPrice)

        fun bind(model: IngredientWithPrice) {
            name.text = model.name
            price.text = String.format("$%.2f", model.price)
            itemView.setOnClickListener { onClick(model) }
        }
    }
}
