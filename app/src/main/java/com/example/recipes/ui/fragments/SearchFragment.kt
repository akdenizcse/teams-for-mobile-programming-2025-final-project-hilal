package com.example.recipes.ui.fragments

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recipes.R
import com.example.recipes.databinding.FragmentSearchBinding
import com.example.recipes.ui.adapters.RecipeAdapter
import com.example.recipes.viewmodel.SearchViewModel

class SearchFragment : Fragment(R.layout.fragment_search) {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val vm: SearchViewModel by activityViewModels()
    private lateinit var recipeAdapter: RecipeAdapter

    override fun onViewCreated(view: View, saved: Bundle?) {
        super.onViewCreated(view, saved)
        _binding = FragmentSearchBinding.bind(view)

        // 1) Read the initialQuery from arguments (fall back to empty string)
        val initialQuery = arguments
            ?.getString("initialQuery", "")
            .orEmpty()

        if (initialQuery.isNotBlank()) {
            binding.searchEditText.setText(initialQuery)
            vm.searchRecipes(initialQuery)
        }

        // 2) RecyclerView setup
        recipeAdapter = RecipeAdapter { recipe ->
            val action = SearchFragmentDirections
                .actionSearchToRecipeDetail(recipe.id)
            findNavController().navigate(action)
        }
        binding.searchRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recipeAdapter
        }

        // 3) Observe LiveData
        vm.recipes.observe(viewLifecycleOwner) { list ->
            recipeAdapter.submitList(list)
            if (list.isEmpty())
                Toast.makeText(requireContext(), "No recipes found", Toast.LENGTH_SHORT)
                    .show()
        }
        vm.isLoading.observe(viewLifecycleOwner) {
            binding.searchProgressBar.isVisible = it
        }
        vm.error.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Toast.makeText(requireContext(), "Error: $it", Toast.LENGTH_LONG).show()
                vm.clearError()
            }
        }

        // 4) Search button & IME “Search”
        binding.searchButton.setOnClickListener { doSearch() }
        binding.searchEditText.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEARCH) {
                doSearch()
                true
            } else false
        }
    }

    private fun doSearch() {
        val q = binding.searchEditText.text.toString().trim()
        if (q.isNotBlank()) {
            vm.searchRecipes(q)
        } else {
            Toast.makeText(requireContext(), "Enter a search term", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
