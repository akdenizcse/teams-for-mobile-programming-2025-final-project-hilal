// app/src/main/java/com/example/recipes/ui/adapters/IngredientsAdapter.kt
package com.example.recipes.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recipes.R
import com.example.recipes.data.model.IngredientWithPrice

// app/src/main/java/com/example/recipes/ui/adapters/IngredientsAdapter.kt
class IngredientsAdapter(
    private val onClick: (IngredientWithPrice) -> Unit
) : RecyclerView.Adapter<IngredientsAdapter.VH>() {

    private val data = mutableListOf<IngredientWithPrice>()

    fun submit(list: List<IngredientWithPrice>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingredient, parent, false)
    )

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(data[position])

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val name: TextView      = v.findViewById(R.id.ingName)
        private val addIcon: ImageView  = v.findViewById(R.id.ingAddIcon)

        fun bind(model: IngredientWithPrice) {
            // model.name now contains the recipe’s amount+unit+ingredient
            name.text = model.name
            addIcon.setOnClickListener { onClick(model) }
        }
    }
}
