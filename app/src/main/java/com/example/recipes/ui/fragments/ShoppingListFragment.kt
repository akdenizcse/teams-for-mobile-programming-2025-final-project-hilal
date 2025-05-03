package com.example.recipes.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recipes.R
import com.example.recipes.data.model.ShoppingItem
import com.example.recipes.databinding.FragmentShoppingListBinding
import com.example.recipes.ui.activities.LoginActivity
import com.example.recipes.ui.adapters.ShoppingListAdapter
import com.example.recipes.viewmodel.ShoppingListViewModel
import com.google.firebase.auth.FirebaseAuth

class ShoppingListFragment : Fragment(R.layout.fragment_shopping_list) {

    private var _binding: FragmentShoppingListBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val vm   by viewModels<ShoppingListViewModel>()

    private lateinit var adapter: ShoppingListAdapter
    private var fullItems = listOf<ShoppingItem>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentShoppingListBinding.bind(view)

        // force login
        val uid = auth.currentUser?.uid ?: run {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
            return
        }

        // hide the app bar here
        (activity as? AppCompatActivity)?.supportActionBar?.hide()

        // 1) set up adapter
        adapter = ShoppingListAdapter()
        binding.shoppingListRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ShoppingListFragment.adapter
        }

        // 2) Add new item
        binding.addItemButton.setOnClickListener {
            binding.addItemEditText.text
                ?.toString()
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let {
                    vm.add(uid, ShoppingItem(name = it))
                    binding.addItemEditText.text?.clear()
                }
        }

        // 3) Select / Done toggle
        binding.selectModeButton.setOnClickListener {
            if (!adapter.selectMode) {
                // → ENTER select mode
                adapter.checkedIds.clear()
                adapter.selectMode = true

                binding.selectModeButton.text          = "Done"
                binding.deleteSelectedButton.isVisible = false
            } else {
                // → EXIT select mode, show only checked
                val chosen = adapter.getCheckedItems()

                adapter.selectMode = false
                adapter.submitList(chosen)

                binding.selectModeButton.isVisible     = false
                binding.deleteSelectedButton.isVisible = true
            }
        }

        // 4) Delete Selected
        binding.deleteSelectedButton.setOnClickListener {
            val chosen = adapter.getCheckedItems()
            if (chosen.isEmpty()) {
                Toast.makeText(requireContext(), "No items selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // perform batch delete then reload
            vm.removeItems(uid, chosen)

            // restore buttons & clear checks
            adapter.checkedIds.clear()
            binding.selectModeButton.isVisible     = true
            binding.selectModeButton.text          = "Select"
            binding.deleteSelectedButton.isVisible = false
        }

        // 5) Observe LiveData
        vm.items.observe(viewLifecycleOwner) { list ->
            fullItems = list
            // only auto-submit when not in select or filtered view
            if (!adapter.selectMode && binding.deleteSelectedButton.isVisible == false) {
                adapter.submitList(fullItems)
            }
            binding.emptyTextView.isVisible = fullItems.isEmpty()
        }
        vm.error.observe(viewLifecycleOwner) { err ->
            err?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                vm.clearError()
            }
        }

        // initial load
        vm.load(uid)
    }

    override fun onPause() {
        super.onPause()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
