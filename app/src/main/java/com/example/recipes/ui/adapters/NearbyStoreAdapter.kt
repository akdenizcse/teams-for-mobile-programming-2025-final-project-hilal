// app/src/main/java/com/example/recipes/ui/adapters/NearbyStoresAdapter.kt
package com.example.recipes.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.recipes.R
import com.example.recipes.data.model.NearbyStore
import java.util.Locale

class NearbyStoresAdapter :
    ListAdapter<NearbyStore, NearbyStoresAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<NearbyStore>() {
            override fun areItemsTheSame(a: NearbyStore, b: NearbyStore) =
                a.name == b.name && a.address == b.address
            override fun areContentsTheSame(a: NearbyStore, b: NearbyStore) = a == b
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name     : TextView = view.findViewById(R.id.storeName)
        val address  : TextView = view.findViewById(R.id.storeAddress)
        val distance : TextView = view.findViewById(R.id.storeDistance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nearby_store, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = getItem(position)
        holder.name.text    = s.name
        holder.address.text = s.address
        holder.distance.text = String.format(
            Locale.getDefault(),
            "%.1f km",
            s.distanceMeters / 1000f
        )
    }
}
