package com.example.recipes.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipes.data.model.CartItem
import com.example.recipes.databinding.ItemCartBinding
import java.util.Locale

/**
 * Shows each CartItem with image, title, price, and a remove-button.
 *
 * @param onRemove callback when the user taps the trash icon.
 */
class CartAdapter(
    private val onRemove: (CartItem) -> Unit
) : ListAdapter<CartItem, CartAdapter.CartVH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartVH {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CartVH(binding)
    }

    override fun onBindViewHolder(holder: CartVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CartVH(private val b: ItemCartBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(item: CartItem) = with(b) {
            titleTextView.text = item.title
            priceTextView.text = String.format(Locale.US, "$%.2f", item.price)

            Glide.with(imageView)
                .load(item.image)
                .centerCrop()
                .into(imageView)

            removeImageView.setOnClickListener { onRemove(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<CartItem>() {
            override fun areItemsTheSame(old: CartItem, new: CartItem) =
                old.id == new.id

            override fun areContentsTheSame(old: CartItem, new: CartItem) =
                old == new
        }
    }
}
