// app/src/main/java/com/example/recipes/ui/fragments/RecipeDetailFragment.kt
package com.example.recipes.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.example.recipes.data.model.Recipe
import com.example.recipes.data.model.Review
import com.example.recipes.data.model.ShoppingItem
import com.example.recipes.data.repository.FirebaseRepository
import com.example.recipes.data.repository.RecipeRepository
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

    private val favoritesVM by activityViewModels<FavoritesViewModel>()
    private val shoppingVM  by activityViewModels<ShoppingListViewModel>()

    private val firebaseRepo = FirebaseRepository()
    private val recipeRepo   = RecipeRepository()
    private val auth         get() = FirebaseAuth.getInstance()

    private var loadedRecipe: Recipe? = null
    private var existingReview: Review? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.hide()

        /* ---------- Ingredients RecyclerView ---------- */
        val ingAdapter = IngredientsAdapter { ing ->
            auth.currentUser?.uid?.let { uid ->
                shoppingVM.add(uid, ShoppingItem(name = ing.original))
                Toast.makeText(requireContext(), "Added to shopping list", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(requireContext(), "Log in to add items", Toast.LENGTH_SHORT).show()
        }
        binding.ingredientsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ingAdapter
        }

        binding.detailProgressBar.isVisible = true

        /* ---------- Load recipe + reviews ---------- */
        lifecycleScope.launch {
            val recipe = recipeRepo.getRecipeDetails(args.recipeId)
            binding.detailProgressBar.isVisible = false

            if (recipe == null) {
                Toast.makeText(requireContext(), "Failed to load recipe", Toast.LENGTH_SHORT).show()
                return@launch
            }
            loadedRecipe = recipe
            bindRecipe(recipe, ingAdapter)

            /* Reviews */
            val allReviews = firebaseRepo.getAllReviewsForRecipe(recipe.id)
            val currentUid = auth.currentUser?.uid       // cache once

            existingReview = allReviews.find { it.userId == currentUid }

            // Show other users' names + comments
            val otherComments = allReviews.filter { it.userId != currentUid }
            if (otherComments.isNotEmpty()) {
                val msg = otherComments.joinToString("\n\n") { rev ->
                    val name = if (rev.userName.isNullOrBlank()) "Anonymous" else rev.userName
                    "$name: ${rev.stars}★\n${rev.comment}"
                }

                AlertDialog.Builder(requireContext())
                    .setTitle("Other users' comments")
                    .setMessage(msg)
                    .setPositiveButton("OK", null)
                    .show()
            }

            // Populate my existing review
            existingReview?.let { rev ->
                binding.ratingBar.rating = rev.stars.toFloat()
                binding.commentEditText.setText(rev.comment)
                binding.ratingBar.setIsIndicator(true)
                binding.commentEditText.isEnabled = false
                binding.submitRatingButton.visibility = View.GONE
                binding.editReviewButton.visibility  = View.VISIBLE
            }
        }

        setupToolbar()
        setupReviewActions()
    }

    /* ---------- Bind recipe UI ---------- */
    private fun bindRecipe(r: Recipe, ingAdapter: IngredientsAdapter) {
        Glide.with(this).load(r.image).into(binding.image)
        binding.title.text              = r.title
        binding.difficultyChip.text     = r.diets?.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Easy"
        binding.timeChip.text           = "${r.readyInMinutes ?: 0} min"
        val cals = r.nutrition?.nutrients?.firstOrNull { it.name.equals("Calories", true) }?.amount?.toInt() ?: 0
        binding.caloriesChip.text       = "$cals kcal"
        binding.servingsChip.text       = getString(R.string.servings_format, r.servings ?: 0)
        binding.instructions.text       = r.instructions?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY) } ?: "No instructions."

        ingAdapter.submitList(r.ingredients.orEmpty())
        val hasIngredients = r.ingredients.orEmpty().isNotEmpty()
        binding.ingredientsLabel.isVisible        = hasIngredients
        binding.ingredientsRecyclerView.isVisible = hasIngredients
    }

    /* ---------- Toolbar ---------- */
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.toolbar.setOnMenuItemClickListener { item ->
            loadedRecipe?.let { r ->
                when (item.itemId) {
                    R.id.action_share    -> { shareRecipe(r); true }
                    R.id.action_favorite -> { addToFavorites(r); true }
                    else -> false
                }
            } ?: false
        }
    }

    /* ---------- Review buttons ---------- */
    private fun setupReviewActions() {
        binding.editReviewButton.setOnClickListener {
            binding.ratingBar.setIsIndicator(false)
            binding.commentEditText.isEnabled = true
            binding.editReviewButton.visibility = View.GONE
            binding.submitRatingButton.apply {
                text       = getString(R.string.update_review)
                visibility = View.VISIBLE
            }
        }

        binding.submitRatingButton.setOnClickListener {
            val stars   = binding.ratingBar.rating.toInt().coerceIn(1, 5)
            val comment = binding.commentEditText.text.toString().trim()
            val user = FirebaseAuth.getInstance().currentUser!!

            val friendlyName = when {
                !user.displayName.isNullOrBlank() -> user.displayName!!
                !user.email.isNullOrBlank()       -> user.email!!.substringBefore('@')
                else                              -> user.uid.take(6) // fallback
            }




            val review = Review(
                userId   = user.uid,
                userName = friendlyName,
                recipeId = args.recipeId,
                stars    = stars,
                comment  = comment
            )

            lifecycleScope.launch {
                firebaseRepo.saveReview(review)

                binding.ratingBar.apply {
                    rating = stars.toFloat()
                    setIsIndicator(true)
                }
                binding.commentEditText.isEnabled = false
                binding.submitRatingButton.visibility = View.GONE
                binding.editReviewButton.visibility   = View.VISIBLE
                Toast.makeText(requireContext(), "Review saved!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /* ---------- Helpers ---------- */
    private fun addToFavorites(r: Recipe) {
        auth.currentUser?.uid?.let { uid ->
            favoritesVM.addFavorite(uid, r)
            Toast.makeText(requireContext(), "Added to favorites", Toast.LENGTH_SHORT).show()
        } ?: Toast.makeText(requireContext(), "Log in to save favorites", Toast.LENGTH_SHORT).show()
    }

    private fun shareRecipe(r: Recipe) {
        val text = "Check out this recipe: ${r.title}\nhttps://yourdomain.com/recipe?id=${r.id}"
        startActivity(Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }, getString(R.string.share_recipe)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        _binding = null
    }
}
