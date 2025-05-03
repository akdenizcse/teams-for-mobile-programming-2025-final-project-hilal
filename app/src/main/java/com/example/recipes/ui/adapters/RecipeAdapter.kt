// app/src/main/java/com/example/recipes/ui/adapters/RecipeAdapter.kt
package com.example.recipes.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipes.R
import com.example.recipes.data.model.Recipe
import com.example.recipes.databinding.ItemRecipesSummaryBinding

/**
 * Adapter for displaying recipes, with optional delete functionality.
 * Provides a secondary constructor for concise item-click binding.
 */
class RecipeAdapter(
    private var recipes: List<Recipe> = emptyList(),
    private val showDelete: Boolean = false,
    private val onItemClick: ((Recipe) -> Unit)? = null,
    private val onDeleteClick: ((Recipe) -> Unit)? = null
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    /** Expose current list for swipe-to-delete or external access */
    val currentList: List<Recipe> get() = recipes

    /**
     * Secondary constructor: allow a trailing lambda for item clicks.
     */
    constructor(onItemClick: (Recipe) -> Unit) : this(
        emptyList(), false, onItemClick, null
    )

    inner class RecipeViewHolder(
        private val binding: ItemRecipesSummaryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(recipes[pos])
                }
            }
            binding.btnDelete.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onDeleteClick?.invoke(recipes[pos])
                }
            }
        }

        fun bind(recipe: Recipe) {
            binding.tvTitle.text = recipe.title
            binding.tvSummary.text = recipe.summary.orEmpty()
            Glide.with(binding.root)
                .load(recipe.image)
                .placeholder(R.drawable.placeholder_image)
                .into(binding.ivRecipeImage)

            binding.btnDelete.isVisible = showDelete
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipesSummaryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(recipes[position])
    }

    override fun getItemCount(): Int = recipes.size

    /** Update the list and refresh the RecyclerView */
    fun submitList(newList: List<Recipe>) {
        recipes = newList
        notifyDataSetChanged()
    }
}