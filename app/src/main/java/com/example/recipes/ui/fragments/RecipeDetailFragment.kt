package com.example.recipes.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.*
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
    private val favoritesVM: FavoritesViewModel by activityViewModels()
    private val shoppingVM: ShoppingListViewModel by activityViewModels()
    private var currentRecipe: Recipe? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = FragmentRecipeDetailBinding.inflate(inflater, container, false)
        .also { _binding = it }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide any default action bar
        (activity as? AppCompatActivity)?.supportActionBar?.hide()

        // Toolbar back & menu clicks
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
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

        // Ingredients RecyclerView
        val ingAdapter = IngredientsAdapter { ing ->
            FirebaseAuth.getInstance().currentUser?.uid?.also { uid ->
                shoppingVM.add(uid, ShoppingItem(name = ing.original))
                Toast.makeText(requireContext(),
                    "Added “${ing.original}” to shopping list", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(requireContext(),
                "Log in to add to shopping list", Toast.LENGTH_SHORT).show()
        }
        binding.ingredientsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ingAdapter
        }

        // Load the recipe details
        binding.detailProgressBar.isVisible = true
        lifecycleScope.launch {
            val recipe = com.example.recipes.data.repository.RecipeRepository()
                .getRecipeDetails(args.recipeId)
            binding.detailProgressBar.isVisible = false
            if (recipe != null) {
                currentRecipe = recipe
                populate(recipe, ingAdapter)
            } else {
                Toast.makeText(requireContext(), "Failed to load recipe", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun populate(recipe: Recipe, adapter: IngredientsAdapter) {
        binding.title.text = recipe.title
        Glide.with(this).load(recipe.image).into(binding.image)

        // Stats chips
        binding.difficultyChip.text = recipe.diets?.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Easy"
        binding.timeChip.text       = "${recipe.readyInMinutes ?: 0} min"
        val cals = recipe.nutrition?.nutrients
            ?.firstOrNull { it.name.equals("Calories", true) }?.amount?.toInt() ?: 0
        binding.caloriesChip.text   = "$cals kcal"
        binding.servingsChip.text   = getString(R.string.servings_format, recipe.servings ?: 0)

        // Instructions
        binding.instructions.text = recipe.instructions
            ?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY) }
            ?: "No instructions available."

        // Ingredients list
        val has = recipe.ingredients?.isNotEmpty() == true
        binding.ingredientsLabel.isVisible           = has
        binding.ingredientsRecyclerView.isVisible    = has
        if (has) adapter.submitList(recipe.ingredients!!)
    }

    private fun addToFavorites(recipe: Recipe) {
        FirebaseAuth.getInstance().currentUser?.uid?.also { uid ->
            favoritesVM.addFavorite(uid, recipe)
            Toast.makeText(requireContext(),
                "Added “${recipe.title}” to favorites", Toast.LENGTH_SHORT).show()
        } ?: Toast.makeText(requireContext(),
            "Please log in to save favorites", Toast.LENGTH_SHORT).show()
    }

    private fun shareRecipe(recipe: Recipe) {
        val text = "Check out this recipe: ${recipe.title}\n" +
                "https://yourdomain.com/recipe?id=${recipe.id}"
        startActivity(Intent.createChooser(
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }, getString(R.string.share_recipe)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Restore default action bar
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        _binding = null
    }
}
