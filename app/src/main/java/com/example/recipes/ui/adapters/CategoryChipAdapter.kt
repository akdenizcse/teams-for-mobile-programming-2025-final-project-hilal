package com.example.recipes.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.recipes.data.model.Category
import com.example.recipes.databinding.ItemCategoryChipBinding

class CategoryChipAdapter(
    private val categories: List<Category>,
    private val onSelect: (Category) -> Unit
) : RecyclerView.Adapter<CategoryChipAdapter.ChipVH>() {

    private var selectedPos = 0

    inner class ChipVH(val binding: ItemCategoryChipBinding)
        : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val prev = selectedPos
                selectedPos = adapterPosition
                notifyItemChanged(prev)
                notifyItemChanged(selectedPos)
                onSelect(categories[selectedPos])
            }
        }

        fun bind(cat: Category, isSelected: Boolean) {
            binding.chip.text = cat.name
            binding.chip.isChecked = isSelected
            // If you have icons, you can do binding.chip.setChipIconResource(cat.image) etc.
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipVH {
        val b = ItemCategoryChipBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ChipVH(b)
    }

    override fun onBindViewHolder(holder: ChipVH, position: Int) {
        holder.bind(categories[position], position == selectedPos)
    }

    override fun getItemCount() = categories.size
}
