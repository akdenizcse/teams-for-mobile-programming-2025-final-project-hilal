package com.example.recipes.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.recipes.R
import com.example.recipes.data.model.Ingredient
import com.example.recipes.data.model.Recipe
import com.example.recipes.data.model.ShoppingItem
import com.example.recipes.databinding.FragmentRecipeDetailBinding
import com.example.recipes.ui.adapters.IngredientsAdapter
import com.example.recipes.viewmodel.FavoritesViewModel
import com.example.recipes.viewmodel.ShoppingListViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class RecipeDetailFragment : Fragment(R.layout.fragment_recipe_detail) {
    private var _binding: FragmentRecipeDetailBinding? = null
    private val binding get() = _binding!!

    private val args: RecipeDetailFragmentArgs by navArgs()
    private val favoritesViewModel: FavoritesViewModel by activityViewModels()
    private val shoppingViewModel: ShoppingListViewModel by activityViewModels()

    private var currentRecipe: Recipe? = null

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentRecipeDetailBinding
        .inflate(inflater, container, false)
        .also { _binding = it }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup toolbar actions
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.toolbar.inflateMenu(R.menu.menu_recipe_detail)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_share -> {
                    currentRecipe?.let { shareRecipe(it) }
                        ?: Toast.makeText(requireContext(), "Still loading…", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_favorite -> {
                    currentRecipe?.let { addToFavorites(it) }
                        ?: Toast.makeText(requireContext(), "Still loading…", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        // Setup ingredients list adapter
        val ingredientsAdapter = IngredientsAdapter { ing ->
            FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                shoppingViewModel.add(uid, ShoppingItem(name = ing.original))
                Toast.makeText(requireContext(), "Added “${ing.original}” to shopping list", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(requireContext(), "Log in to add to shopping list", Toast.LENGTH_SHORT).show()
        }
        binding.ingredientsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ingredientsAdapter
        }

        // Fetch and display recipe based on deep-link ID
        binding.detailProgressBar.isVisible = true
        lifecycleScope.launch {
            val recipe = com.example.recipes.data.repository.RecipeRepository()
                .getRecipeDetails(args.recipeId)
            binding.detailProgressBar.isVisible = false
            if (recipe != null) {
                currentRecipe = recipe
                displayRecipe(recipe, ingredientsAdapter)
            } else {
                Toast.makeText(requireContext(), "Failed to load recipe", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayRecipe(recipe: Recipe, adapter: IngredientsAdapter) {
        binding.title.text = recipe.title
        Glide.with(this).load(recipe.image).into(binding.image)
        bindStats(recipe)
        bindInstructions(recipe.instructions)
        bindIngredients(recipe.ingredients, adapter)
    }

    private fun addToFavorites(recipe: Recipe) {
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            favoritesViewModel.addFavorite(uid, recipe)
            Toast.makeText(requireContext(), "Added “${recipe.title}” to favorites", Toast.LENGTH_SHORT).show()
        } ?: Toast.makeText(requireContext(), "Please log in to save favorites", Toast.LENGTH_SHORT).show()
    }

    private fun shareRecipe(recipe: Recipe) {
        val shareText = "Check out this recipe: ${recipe.title}\nhttps://www.yourdomain.com/recipe?id=${recipe.id}"
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_recipe)))
    }

    private fun bindStats(r: Recipe) {
        binding.difficultyChip.text = r.diets?.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Easy"
        binding.timeChip.text = "${r.readyInMinutes ?: 0} min"
        val calories = r.nutrition?.nutrients?.firstOrNull { it.name.equals("Calories", true) }?.amount?.toInt() ?: 0
        binding.caloriesChip.text = "$calories kcal"
        binding.servingsChip.text = getString(R.string.servings_format, r.servings ?: 0)
    }

    private fun bindInstructions(instr: String?) {
        binding.instructions.text = instr?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY) } ?: "No instructions available."
    }

    private fun bindIngredients(list: List<Ingredient>?, adapter: IngredientsAdapter) {
        val hasIngredients = !list.isNullOrEmpty()
        binding.ingredientsLabel.isVisible = hasIngredients
        binding.ingredientsRecyclerView.isVisible = hasIngredients
        if (hasIngredients) adapter.submitList(list!!)
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
