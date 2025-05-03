package com.example.recipes.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.example.recipes.R
import com.example.recipes.data.model.Category
import com.example.recipes.databinding.FragmentHomeBinding
import com.example.recipes.databinding.FragmentCategoriesBinding
import com.example.recipes.ui.adapters.CategoryAdapter
import com.example.recipes.ui.adapters.RecipeAdapter
import com.example.recipes.viewmodel.SearchViewModel

class CategoriesFragment : Fragment(R.layout.fragment_categories) {
    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!

    private val args: CategoriesFragmentArgs by navArgs()
    private val viewModel: SearchViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCategoriesBinding.bind(view)

        val category = args.category.trim()
        (activity as? AppCompatActivity)?.supportActionBar?.title = if (category.isBlank()) {
            "All Recipes"
        } else {
            category
        }

        if (category.isBlank()) {
            fetchAllRecipes()
        } else {
            fetchByCategory(category)
        }

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val adapter = RecipeAdapter { recipe ->
            // pass only recipe.id now
            val action = CategoriesFragmentDirections
                .actionCategoriesToRecipeDetail(recipe.id)
            findNavController().navigate(action)
        }
        binding.categoriesRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            this.adapter = adapter
        }
        viewModel.recipes.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }
    }

    private fun fetchAllRecipes() {
        viewModel.searchRecipes("")
    }

    private fun fetchByCategory(category: String) {
        viewModel.searchByCategory(category)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
