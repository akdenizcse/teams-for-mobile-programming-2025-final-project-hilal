package com.example.recipes.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipes.R
import com.example.recipes.data.model.Order
import com.example.recipes.databinding.ItemOrderBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrdersAdapter(
    private val onClick: (Order) -> Unit
) : ListAdapter<Order, OrdersAdapter.OrderViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = OrderViewHolder(
        ItemOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderViewHolder(private val binding: ItemOrderBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            // Join all dish titles
            binding.orderTitleText.text = order.items.joinToString(", ") { it.title }

            // Friendly date
            binding.orderDateText.text = order.timestamp.toFriendlyDate()

            // Total
            binding.orderTotalText.text = "€%.2f".format(order.total)

            binding.root.setOnClickListener { onClick(order) }
        }
    }


    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Order>() {
            override fun areItemsTheSame(a: Order, b: Order) = a.id == b.id
            override fun areContentsTheSame(a: Order, b: Order) = a == b
        }
    }
}

// Extension for formatting timestamps
private fun Long.toFriendlyDate(): String {
    val sdf = SimpleDateFormat("MMM d, yyyy 'at' HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}
