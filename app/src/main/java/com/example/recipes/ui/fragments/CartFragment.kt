package com.example.recipes.ui.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recipes.R
import com.example.recipes.databinding.FragmentCartBinding
import com.example.recipes.ui.adapters.CartAdapter
import com.example.recipes.viewmodel.CartViewModel
import com.google.firebase.auth.FirebaseAuth

/**
 * Shows everything the user has “added to cart” and lets them
 * remove items or perform a simulated checkout (clears the cart).
 *
 * Layout file expected: res/layout/fragment_cart.xml
 * containing:
 *   - RecyclerView   @+id/cartRecycler
 *   - TextView       @+id/totalTextView
 *   - Button         @+id/checkoutButton
 *   - TextView       @+id/emptyTextView    (visible when list empty)
 */
class CartFragment : Fragment(R.layout.fragment_cart) {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private val cartVM: CartViewModel by activityViewModels()
    private val auth  get() = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCartBinding.bind(view)

        /* ---- Ensure user logged in ---- */
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "Log in to view your cart", Toast.LENGTH_SHORT).show()
            return
        }

        /* ---- Recycler + adapter ---- */
        val adapter = CartAdapter(
            onRemove = { cartItem -> cartVM.removeItem(uid, cartItem) }
        )
        binding.cartRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter  = adapter
        }

        /* ---- Observe LiveData ---- */
        cartVM.items.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.emptyTextView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        cartVM.totalPrice.observe(viewLifecycleOwner) { total ->
            binding.totalTextView.text = getString(R.string.cart_total, total)
        }

        /* ---- Load initial contents ---- */
        cartVM.load(uid)

        /* ---- Checkout ---- */
        binding.checkoutButton.setOnClickListener {
            cartVM.checkout(uid) {
                Toast.makeText(requireContext(), "Order placed! Thanks", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
