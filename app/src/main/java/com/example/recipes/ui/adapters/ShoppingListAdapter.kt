// app/src/main/java/com/example/recipes/ui/adapters/ShoppingListAdapter.kt
package com.example.recipes.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.recipes.R
import com.example.recipes.data.model.ShoppingItem
import java.util.Locale

class ShoppingListAdapter(
    private val onRemove: ((ShoppingItem) -> Unit)? = null
) : ListAdapter<ShoppingItem, ShoppingListAdapter.VH>(DIFF) {

    /** When true: show checkboxes; when false: show delete buttons */
    var selectMode: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    /** IDs of items checked in select mode */
    val checkedIds = mutableSetOf<String>()

    /** Returns the list of checked items when in select mode */
    fun getCheckedItems(): List<ShoppingItem> =
        currentList.filter { it.id in checkedIds }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping, parent, false)
        return VH(itemView)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)

        // bind name & price
        holder.name.text  = item.name
        holder.price.text = String.format(Locale.getDefault(), "$%.2f", item.price)

        // checkbox
        holder.checkBox.apply {
            visibility = if (selectMode) View.VISIBLE else View.GONE
            isChecked  = item.id in checkedIds
            setOnCheckedChangeListener { _, checked ->
                if (checked) checkedIds.add(item.id)
                else          checkedIds.remove(item.id)
            }
        }

        // delete button
        holder.deleteButton.apply {
            visibility = if (selectMode) View.GONE else View.VISIBLE
            setOnClickListener { onRemove?.invoke(item) }
        }
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox:   CheckBox     = view.findViewById(R.id.checkBox)
        val name:       TextView     = view.findViewById(R.id.nameTextView)
        val price:      TextView     = view.findViewById(R.id.priceTextView)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ShoppingItem>() {
            override fun areItemsTheSame(a: ShoppingItem, b: ShoppingItem) =
                a.id == b.id

            override fun areContentsTheSame(a: ShoppingItem, b: ShoppingItem) =
                a == b
        }
    }
}
