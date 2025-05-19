package com.example.recipes.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.text.Html
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.recipes.R
import com.example.recipes.data.model.Comment
import com.example.recipes.data.model.IngredientWithPrice
import com.example.recipes.data.model.Recipe
import com.example.recipes.data.model.ShoppingItem
import com.example.recipes.data.repository.FirebaseRepository
import com.example.recipes.data.repository.RecipeRepository
import com.example.recipes.databinding.FragmentRecipeDetailBinding
import com.example.recipes.ui.adapters.CommentsAdapter
import com.example.recipes.ui.adapters.IngredientsAdapter
import com.example.recipes.viewmodel.FavoritesViewModel
import com.example.recipes.viewmodel.ShoppingListViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlin.random.Random

class RecipeDetailFragment : Fragment(R.layout.fragment_recipe_detail) {

    private var _binding: FragmentRecipeDetailBinding? = null
    private val binding get() = _binding!!

    private val args            by navArgs<RecipeDetailFragmentArgs>()
    private val favoritesVM     by activityViewModels<FavoritesViewModel>()
    private val shoppingVM      by activityViewModels<ShoppingListViewModel>()
    private val recipeRepo      = RecipeRepository()
    private val commentRepo     = FirebaseRepository()
    private val auth            get() = FirebaseAuth.getInstance()

    private var loadedRecipe: Recipe? = null
    private var ingredientPrices: List<IngredientWithPrice> = emptyList()

    // Tracks whether the current user has already left a top‐level comment
    private var myTopLevelComment: Comment? = null

    // If non‐null, this is the parent ID we're replying to
    private var replyToParentId: String? = null

    // — Ingredients adapter —
    private val ingredientsAdapter = IngredientsAdapter { ing ->
        auth.currentUser?.uid?.let { uid ->
            shoppingVM.addWithPrice(uid, ShoppingItem(name = ing.name, price = ing.price))
            Toast.makeText(requireContext(), R.string.added_to_shopping_list, Toast.LENGTH_SHORT).show()
        } ?: Toast.makeText(requireContext(), R.string.login_to_add, Toast.LENGTH_SHORT).show()
    }

    // — Comments adapter — replies trigger storing parentId
    private val commentsAdapter = CommentsAdapter { parent ->
        replyToParentId = parent.id
        binding.commentEditText.setText("@${parent.userName} ")
        binding.commentEditText.setSelection(binding.commentEditText.text!!.length)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun onToolbarItemSelected(item: MenuItem): Boolean {
        loadedRecipe?.let { recipe ->
            return when (item.itemId) {
                R.id.action_favorite -> {
                    auth.currentUser?.uid?.also { uid ->
                        favoritesVM.addFavorite(uid, recipe)
                        Toast.makeText(requireContext(), R.string.added_to_favorites, Toast.LENGTH_SHORT).show()
                    } ?: Toast.makeText(requireContext(), R.string.login_to_favorite, Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_add_to_shopping -> {
                    auth.currentUser?.uid?.also { uid ->
                        val total = ingredientPrices.sumOf { it.price }
                        shoppingVM.addWithPrice(uid,
                            ShoppingItem(
                                name     = recipe.title,
                                recipeId = recipe.id.toString(),
                                price    = total
                            )
                        )
                        Toast.makeText(requireContext(),
                            getString(R.string.added_to_cart, recipe.title),
                            Toast.LENGTH_SHORT
                        ).show()
                    } ?: Toast.makeText(requireContext(), R.string.login_to_add, Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_share -> {
                    val text = getString(R.string.share_text, recipe.title, recipe.id)
                    startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND)
                                .putExtra(Intent.EXTRA_TEXT, text)
                                .setType("text/plain"),
                            getString(R.string.share_recipe)
                        )
                    )
                    true
                }
                else -> false
            }
        }
        return false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Hide host bar
        (activity as? AppCompatActivity)?.supportActionBar?.hide()

        // 2) Toolbar
        binding.toolbar.apply {
            inflateMenu(R.menu.menu_recipe_detail)
            setNavigationOnClickListener { findNavController().navigateUp() }
            setOnMenuItemClickListener(::onToolbarItemSelected)
        }

        // 3) Ingredients listonT
        binding.ingredientsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter       = ingredientsAdapter
        }

        // 4) Comments list
        binding.commentsRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter       = commentsAdapter
        }

        // 5) Submit or reply
        binding.submitRatingButton.setOnClickListener { postCommentOrReply() }
        //    Edit existing top‐level
        binding.editReviewButton.setOnClickListener { enterEditMode() }

        // 6) Intercept system back to restore host bar
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigateUp()
                }
            }
        )

        // 7) Kick off load
        loadRecipeAndComments()
    }

    /** Loads recipe details **and** the comments, then figures out submit vs edit. */
    private fun loadRecipeAndComments() = lifecycleScope.launch {
        binding.detailProgressBar.isVisible = true

        // — Recipe details —
        recipeRepo.getRecipeDetails(args.recipeId)?.let { r ->
            loadedRecipe = r

            Glide.with(this@RecipeDetailFragment)
                .load(r.image)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.baseline_broken_image_24)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.image)

            binding.title.text = r.title
            binding.difficultyChip.text =
                r.diets?.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Easy"
            binding.timeChip.text = getString(R.string.minutes_format, r.readyInMinutes ?: 0)
            val kcal = r.nutrition
                ?.nutrients
                ?.firstOrNull { it.name.equals("Calories", true) }
                ?.amount?.toInt()
                ?: 0
            binding.caloriesChip.text = getString(R.string.kcal_format, kcal)
            binding.servingsChip.text = getString(R.string.servings_format, r.servings ?: 0)
            binding.instructions.text = r.instructions
                ?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY) }
                ?: getString(R.string.no_instructions)

            // Generate random prices once
            val rnd = Random(System.currentTimeMillis())
            ingredientPrices = r.ingredients.orEmpty()
                .map { IngredientWithPrice(it.original, rnd.nextInt(4, 21) * 0.5) }
            ingredientsAdapter.submit(ingredientPrices)
        }

        // — Comments thread —
        val allComments = commentRepo.getCommentsForRecipe(args.recipeId)
        commentsAdapter.submitList(allComments.toMutableList())
        // Check if *we* already left a top‐level comment
        auth.currentUser?.uid?.let { uid ->
            myTopLevelComment = commentRepo.getMyComment(args.recipeId, uid)
        }

        if (myTopLevelComment != null) {
            // Prefill the text & show EDIT instead of SUBMIT
            binding.commentEditText.setText(myTopLevelComment!!.text)
            binding.submitRatingButton.isVisible = false
            binding.editReviewButton  .isVisible = true
        } else {
            binding.commentEditText.text?.clear()
            binding.submitRatingButton.isVisible = true
            binding.editReviewButton  .isVisible = false
        }

        binding.detailProgressBar.isVisible = false
    }

    /** Called by “Submit” or “Edit” buttons. */
    private fun postCommentOrReply() {
        val text = binding.commentEditText.text.toString().trim()
        if (text.isBlank()) return

        val user = auth.currentUser ?: run {
            Toast.makeText(requireContext(), R.string.login_to_review, Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            // If no parentId → top‐level → upsert so each user only has one
            val topLevelId = if (replyToParentId == null) {
                myTopLevelComment?.id  // existing ID if editing
            } else null

            val c = Comment(
                id        = topLevelId ?: "",
                recipeId  = args.recipeId,
                userId    = user.uid,
                userName  = user.displayName?.takeIf(String::isNotBlank)
                    ?: user.email?.substringBefore('@')
                    ?: getString(R.string.anonymous),
                text      = text,
                timestamp = System.currentTimeMillis(),
                parentId  = replyToParentId
            )

            // Save or update
            if (replyToParentId == null) {
                commentRepo.upsertComment(c)
            } else {
                commentRepo.saveReply(c)
            }

            // Reset state
            replyToParentId = null

            // Reload comments & button states
            loadRecipeAndComments()
        }
    }

    /** Switch UI into “edit top‐level comment” mode */
    private fun enterEditMode() {
        binding.submitRatingButton.isVisible = true
        binding.editReviewButton.isVisible   = false
        // leave the existing text there for editing
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }
}