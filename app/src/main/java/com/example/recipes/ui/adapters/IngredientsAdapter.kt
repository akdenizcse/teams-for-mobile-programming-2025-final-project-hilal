package com.example.recipes.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.recipes.data.model.Ingredient
import com.example.recipes.databinding.ItemIngredientBinding

class IngredientsAdapter(
    private val onAddClick: (Ingredient) -> Unit
) : RecyclerView.Adapter<IngredientsAdapter.VH>() {

    private var items: List<Ingredient> = emptyList()

    fun submitList(list: List<Ingredient>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(private val binding: ItemIngredientBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(ing: Ingredient) {
            // Use the correct ID from your layout (tvIngredientName)
            binding.tvIngredientName.text = ing.original
            binding.btnAddIngredient.setOnClickListener {
                onAddClick(ing)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemIngredientBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false
        )
        return VH(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }
}