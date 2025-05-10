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
import com.example.recipes.ui.adapters.CategoryChipAdapter
import com.example.recipes.ui.adapters.RecipeAdapter
import com.example.recipes.viewmodel.HomeViewModel
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val vm: HomeViewModel by viewModels()
    private lateinit var recipeAdapter: RecipeAdapter

    override fun onViewCreated(view: View, saved: Bundle?) {
        super.onViewCreated(view, saved)
        _binding = FragmentHomeBinding.bind(view)

        // 1) Hide AppBar & greet
        val name = FirebaseAuth.getInstance().currentUser
            ?.displayName
            ?.substringBefore("@")
            ?.replaceFirstChar { it.uppercase() }
            ?: "there"
        binding.greetingTextView.text = "Hi $name"

        // 2) Recipes grid + adapter
        recipeAdapter = RecipeAdapter { recipe ->
            val action = HomeFragmentDirections
                .actionHomeToRecipeDetail(recipe.id)
            findNavController().navigate(action)
        }
        binding.recipesRecycler.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = recipeAdapter
        }

        // 3) Observe unified feed
        vm.recipes.observe(viewLifecycleOwner) { list ->
            recipeAdapter.submitList(list)
        }

        // 4) Search wiring
        binding.homeSearchEditText.apply {
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    vm.searchHomeRecipes(text.toString())
                    true
                } else false
            }
            setOnKeyListener { _, key, ev ->
                if (key == KeyEvent.KEYCODE_ENTER && ev.action == KeyEvent.ACTION_UP) {
                    vm.searchHomeRecipes(text.toString())
                    true
                } else false
            }
        }
        binding.homeSearchLayout.setEndIconOnClickListener {
            vm.searchHomeRecipes(binding.homeSearchEditText.text.toString())
        }

        // 5) Chips row
        binding.chipRecycler.apply {
            layoutManager = LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false
            )
            adapter = CategoryChipAdapter(vm.categories.value ?: emptyList()) { cat ->
                binding.homeSearchEditText.text?.clear()
                vm.onCategorySelected(cat.name)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        _binding = null
    }
}
