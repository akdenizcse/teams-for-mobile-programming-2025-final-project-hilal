// app/src/main/java/com/example/recipes/ui/fragments/OrdersFragment.kt
package com.example.recipes.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recipes.R
import com.example.recipes.databinding.FragmentOrdersBinding
import com.example.recipes.ui.activities.PaymentActivity
import com.example.recipes.ui.adapters.OrdersAdapter
import com.example.recipes.viewmodel.OrdersViewModel
import java.util.Locale

class OrdersFragment : Fragment(R.layout.fragment_orders) {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!
    private val vm by activityViewModels<OrdersViewModel>()

    private val adapter = OrdersAdapter { order -> showOrderDialog(order) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentOrdersBinding.bind(view)

        binding.ordersRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.ordersRecycler.adapter       = adapter

        vm.orders.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.ordersEmptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }


        vm.loadOrders()
    }

    /** Pop detailed order info */
    private fun showOrderDialog(order: com.example.recipes.data.model.Order) {
        val itemLines = order.items.joinToString("\n") { ci ->
            "• ${ci.title}: $%.2f".format(Locale.getDefault(), ci.price)
        }
        val msg = """
            $itemLines

            ───────────────
            Total: $%.2f
        """.trimIndent().format(order.total)

        AlertDialog.Builder(requireContext())
            .setTitle(order.readableTitle())   // date-based, user-friendly
            .setMessage(msg)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
