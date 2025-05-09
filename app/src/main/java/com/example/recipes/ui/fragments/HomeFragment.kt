package com.example.recipes.ui.fragments

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recipes.R
import com.example.recipes.databinding.FragmentHomeBinding
import com.example.recipes.ui.adapters.CategoryAdapter
import com.example.recipes.ui.adapters.CategoryChipAdapter
import com.example.recipes.ui.adapters.RecipeAdapter
import com.example.recipes.viewmodel.HomeViewModel
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        // 1) Hide the AppBar and show personalized greeting
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        val name = FirebaseAuth.getInstance()
            .currentUser
            ?.displayName
            ?.substringBefore("@")
            ?.replaceFirstChar { it.uppercase() }
            ?: "there"
        binding.greetingTextView.text = "Hi $name"

        // 2) Wire up search IME/Enter/end-icon to navigate to SearchFragment
        binding.homeSearchEditText.apply {
            // IME action "Search"
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    navigateToSearch()
                    true
                } else false
            }
            // Physical Enter key
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                    navigateToSearch()
                    true
                } else false
            }
        }
        // End-icon tap
        binding.homeSearchLayout.setEndIconOnClickListener {
            navigateToSearch()
        }


        // 4) Horizontal filter‐chips row
        binding.chipRecycler.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = CategoryChipAdapter(viewModel.categories.value ?: emptyList()) { category ->
                viewModel.onCategorySelected(category.name)
            }
        }

        // 5) Quick & Easy 2-column grid
        binding.quickEasyRecycler.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = RecipeAdapter { recipe ->
                // navigate to detail
                val action = HomeFragmentDirections
                    .actionHomeToRecipeDetail(recipeId = recipe.id)
                findNavController().navigate(action)
            }
        }

        // 6) Observe both feeds: initial quickEasy and any searchResults
        viewModel.quickEasy.observe(viewLifecycleOwner) { list ->
            (binding.quickEasyRecycler.adapter as RecipeAdapter).submitList(list)
        }
        viewModel.searchResults.observe(viewLifecycleOwner) { list ->
            (binding.quickEasyRecycler.adapter as RecipeAdapter).submitList(list)
        }
    }

    private fun navigateToSearch() {
        val query = binding.homeSearchEditText.text.toString().trim()
        if (query.isNotBlank()) {
            val action = HomeFragmentDirections
                .actionHomeToSearch(initialQuery = query)
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // restore AppBar
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        _binding = null
    }
}
