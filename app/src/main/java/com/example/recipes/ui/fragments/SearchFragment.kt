package com.example.recipes.ui.fragments

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recipes.R
import com.example.recipes.databinding.FragmentSearchBinding
import com.example.recipes.ui.adapters.RecipeAdapter
import com.example.recipes.viewmodel.SearchViewModel

class SearchFragment : Fragment(R.layout.fragment_search) {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    // Shared ViewModel with FiltersFragment
    private val viewModel: SearchViewModel by activityViewModels()
    private lateinit var recipeAdapter: RecipeAdapter

    override fun onResume() {
        super.onResume()
        // Hide the toolbar when Search is visible
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchBinding.bind(view)

        // Setup RecyclerView
        recipeAdapter = RecipeAdapter { recipe ->
            // Pass only recipe.id now
            val action = SearchFragmentDirections.actionSearchToRecipeDetail(recipe.id)
            findNavController().navigate(action)
        }
        binding.searchRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recipeAdapter
        }

        // Observe LiveData
        viewModel.recipes.observe(viewLifecycleOwner) { list ->
            recipeAdapter.submitList(list)
            if (list.isEmpty()) Toast.makeText(requireContext(), "No recipes found", Toast.LENGTH_SHORT).show()
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { binding.searchProgressBar.isVisible = it }
        viewModel.error.observe(viewLifecycleOwner) { err ->
            err?.let { msg ->
                Toast.makeText(requireContext(), "Error: $msg", Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Initial load (use any existing filters)
        viewModel.searchRecipes("")

        // Search button triggers search with current filters
        binding.searchButton.setOnClickListener {
            val query = binding.searchEditText.text.toString().trim()
            if (TextUtils.isEmpty(query)) {
                Toast.makeText(requireContext(), "Please enter a search term", Toast.LENGTH_SHORT).show()
            } else {
                // update stored query
                viewModel.currentQuery = query
                viewModel.searchRecipes(query)
            }
        }

        // Filters button: save current query, then navigate to Filters
        binding.filterButton.setOnClickListener {
            viewModel.currentQuery = binding.searchEditText.text.toString().trim()
            findNavController().navigate(R.id.action_search_to_filters)
        }
    }

    override fun onPause() {
        super.onPause()
        // Show the toolbar when leaving Search
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
