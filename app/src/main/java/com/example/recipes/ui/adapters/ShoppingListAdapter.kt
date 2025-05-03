package com.example.recipes.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.recipes.data.model.ShoppingItem
import com.example.recipes.databinding.ItemShoppingBinding

class ShoppingListAdapter : ListAdapter<ShoppingItem, ShoppingListAdapter.VH>(DIFF) {

    /** IDs of items the user has checked */
    val checkedIds = mutableSetOf<String>()

    /** When true: we’re in “select” mode, so show checkboxes */
    var selectMode = false
        set(v) {
            field = v
            notifyDataSetChanged()
        }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ShoppingItem>() {
            override fun areItemsTheSame(a: ShoppingItem, b: ShoppingItem) = a.id == b.id
            override fun areContentsTheSame(a: ShoppingItem, b: ShoppingItem) = a == b
        }
    }

    inner class VH(val binding: ItemShoppingBinding)
        : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.checkbox.setOnCheckedChangeListener { _, checked ->
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val item = getItem(pos)
                    if (checked) checkedIds.add(item.id) else checkedIds.remove(item.id)
                }
            }
        }

        fun bind(item: ShoppingItem) {
            binding.tvName.text = item.name

            // show the checkbox only in selectMode
            binding.checkbox.isVisible = selectMode

            // restore its checked state from checkedIds
            binding.checkbox.isChecked = checkedIds.contains(item.id)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemShoppingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ))

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    /** helper to grab the ShoppingItems the user checked */
    fun getCheckedItems(): List<ShoppingItem> =
        currentList.filter { checkedIds.contains(it.id) }
}
