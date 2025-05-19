package com.example.recipes.ui.adapters

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.recipes.R
import com.example.recipes.data.model.Category
import com.example.recipes.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val categories: List<Category>,
    private val onClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: Category) {
            binding.apply {
                categoryName = category.name
                clickListener = View.OnClickListener { onClick(category) }

                // Set the icon resource
                ivCategoryImage.setImageResource(category.image)

                // Dynamically tint the icon (e.g. warm mercan)
                val tintColor = ContextCompat.getColor(root.context, R.color.colorPrimary)
                ivCategoryImage.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN)

                executePendingBindings()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        CategoryViewHolder(
            ItemCategoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size
}