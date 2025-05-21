package com.example.recipes.ui.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
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
import java.util.Locale
import kotlin.random.Random

class RecipeDetailFragment : Fragment(R.layout.fragment_recipe_detail) {

    private var _binding: FragmentRecipeDetailBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<RecipeDetailFragmentArgs>()
    private val favoritesVM by activityViewModels<FavoritesViewModel>()
    private val shoppingVM by activityViewModels<ShoppingListViewModel>()
    private val recipeRepo = RecipeRepository()
    private val commentRepo = FirebaseRepository()
    private val auth get() = FirebaseAuth.getInstance()

    private var loadedRecipe: Recipe? = null
    private var ingredientPrices = listOf<IngredientWithPrice>()
    private var myTopLevelComment: Comment? = null
    private var replyToParentId: String? = null
    private var currentRecipePrice: Double = 0.0

    companion object {
        // Cache one‐time random price per kilogram (or per liter)
        private val basePriceCache = mutableMapOf<Int, Double>()
        private fun getOrCreateBasePrice(id: Int): Double =
            basePriceCache.getOrPut(id) { Random.nextInt(8, 21) * 0.5 }

        // Supported units & their conversion to the smallest measure (g or mL)
        private val WEIGHT_UNITS = listOf("g", "kg", "oz", "lb")
        private val VOLUME_UNITS = listOf("ml", "l", "cup", "tbsp", "tsp")
        private val UNIT_FACTORS = mapOf(
            "g" to 1.0,
            "kg" to 1_000.0,
            "oz" to 28.35,
            "lb" to 453.6,
            "ml" to 1.0,
            "l" to 1_000.0,
            "cup" to 240.0,
            "tbsp" to 15.0,
            "tsp" to 5.0
        )
    }

    private val ingredientsAdapter = IngredientsAdapter { ing ->
        showQuantityUnitDialog(ing)
    }

    private val commentsAdapter = CommentsAdapter { parent ->
        replyToParentId = parent.id
        binding.commentEditText.setText(
            getString(R.string.reply_to_user, parent.userName)
        )
        binding.commentEditText.setSelection(binding.commentEditText.text!!.length)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentRecipeDetailBinding.inflate(inflater, container, false)
        .also { _binding = it }
        .root

    @SuppressLint("SetTextI18n", "InflateParams")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.hide()

        // Toolbar
        binding.toolbar.apply {
            inflateMenu(R.menu.menu_recipe_detail)
            setNavigationOnClickListener { findNavController().navigateUp() }
            setOnMenuItemClickListener(::onToolbarItemSelected)
        }

        // Ingredient list
        binding.ingredientsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ingredientsAdapter
        }

        // Comments list
        binding.commentsRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = commentsAdapter
        }

        binding.submitRatingButton.setOnClickListener { postCommentOrReply() }
        binding.editReviewButton.setOnClickListener    { enterEditMode() }

        // Back press
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigateUp()
                }
            }
        )

        loadRecipeAndComments()
    }

    private fun onToolbarItemSelected(item: MenuItem): Boolean {
        loadedRecipe?.let { recipe ->
            return when (item.itemId) {
                R.id.action_favorite -> {
                    auth.currentUser?.uid?.also { uid ->
                        favoritesVM.addFavorite(uid, recipe)
                        Toast.makeText(
                            requireContext(),
                            R.string.added_to_favorites,
                            Toast.LENGTH_SHORT
                        ).show()
                    } ?: Toast.makeText(
                        requireContext(),
                        R.string.login_to_favorite,
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }
                R.id.action_add_to_shopping -> {
                    auth.currentUser?.uid?.also { uid ->
                       // random price instead of ingredients total
                        val total = currentRecipePrice
                        shoppingVM.addWithPrice(
                            uid,
                            ShoppingItem(
                                name     = recipe.title,
                                recipeId = recipe.id.toString(),
                                price    = total
                            )
                        )
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.added_to_cart, recipe.title),
                            Toast.LENGTH_SHORT
                        ).show()
                    } ?: Toast.makeText(
                        requireContext(),
                        R.string.login_to_add,
                        Toast.LENGTH_SHORT
                    ).show()
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

    private fun loadRecipeAndComments() = lifecycleScope.launch {
        binding.detailProgressBar.isVisible = true

        // 1) Load recipe header
        recipeRepo.getRecipeDetails(args.recipeId)?.let { r ->
            loadedRecipe = r

            currentRecipePrice = getOrCreateRecipePrice(r.id)

            Glide.with(this@RecipeDetailFragment)
                .load(r.image)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.baseline_broken_image_24)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.image)

            binding.title.text          = r.title
            binding.difficultyChip.text = r.diets
                ?.firstOrNull()?.replaceFirstChar { it.uppercase() }
                ?: "Easy"
            binding.timeChip.text = getString(
                R.string.minutes_format,
                r.readyInMinutes ?: 0
            )
            val kcal = r.nutrition
                ?.nutrients
                ?.firstOrNull { it.name.equals("Calories", true) }
                ?.amount?.toInt() ?: 0
            binding.caloriesChip.text  = getString(R.string.kcal_format, kcal)
            binding.servingsChip.text  = getString(
                R.string.servings_format,
                r.servings ?: 0
            )
            binding.instructions.text  = r.instructions
                ?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY) }
                ?: getString(R.string.no_instructions)

            // 2) Compute each ingredient’s true cost
            ingredientPrices = r.ingredients.orEmpty().map { ing ->
                // a) price per kg (or L)
                val basePerKg       = getOrCreateBasePrice(ing.id)
                // b) convert to price per gram or ml
                val pricePerSmall   = basePerKg / 1000.0
                // c) how many smallest units in recipe unit
                val factor          = UNIT_FACTORS[ing.unit] ?: 1.0
                val usedSmallAmount = ing.amount * factor
                // d) total cost
                val cost            = pricePerSmall * usedSmallAmount
                // e) display as "100.0 g cocoa"
                val displayName     = "${ing.amount.format(1)} ${ing.unit} ${ing.name}"

                IngredientWithPrice(displayName, cost)
            }
            ingredientsAdapter.submit(ingredientPrices)

            // 3) Dish total
            binding.recipePrice.text = "$${currentRecipePrice.format(2)}"

        }

        // 4) Comments (unchanged)
        val allComments = commentRepo.getCommentsForRecipe(args.recipeId)
        commentsAdapter.submitList(allComments.toMutableList())
        auth.currentUser?.uid?.let { uid ->
            myTopLevelComment = commentRepo.getMyComment(args.recipeId, uid)
        }
        if (myTopLevelComment != null) {
            binding.commentEditText.setText(myTopLevelComment!!.text)
            binding.submitRatingButton.isVisible = false
            binding.editReviewButton.isVisible   = true
        } else {
            binding.commentEditText.text?.clear()
            binding.submitRatingButton.isVisible = true
            binding.editReviewButton.isVisible   = false
        }

        binding.detailProgressBar.isVisible = false
    }

    /**
    +     * Look up a saved random price (20–60) for this recipe ID,
    +     * or generate & persist one if missing.
    +     */
        private fun getOrCreateRecipePrice(recipeId: Int): Double {
               val prefs = requireContext()
                    .getSharedPreferences("recipe_prices", AppCompatActivity.MODE_PRIVATE)
                val key = "recipe_price_$recipeId"
                // stored as float; default < 0 means “not set”
                val stored = prefs.getFloat(key, -1f)
                return if (stored >= 0f) {
                        stored.toDouble()
                    } else {
                        val generated = Random.nextDouble(20.0, 60.0)
                        prefs.edit()
                            .putFloat(key, generated.toFloat())
                            .apply()
                        generated
                    }
            }

    private fun postCommentOrReply() {
        val text = binding.commentEditText.text.toString().trim()
        if (text.isBlank()) return
        val user = auth.currentUser ?: run {
            Toast.makeText(
                requireContext(),
                R.string.login_to_review,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        lifecycleScope.launch {
            val topId = if (replyToParentId == null) myTopLevelComment?.id else null
            val c = Comment(
                id        = topId ?: "",
                recipeId  = args.recipeId,
                userId    = user.uid,
                userName  = user.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?: user.email?.substringBefore('@')
                    ?: getString(R.string.anonymous),
                text      = text,
                timestamp = System.currentTimeMillis(),
                parentId  = replyToParentId
            )
            if (replyToParentId == null) commentRepo.upsertComment(c)
            else                             commentRepo.saveReply(c)

            replyToParentId = null
            loadRecipeAndComments()
        }
    }

    private fun enterEditMode() {
        binding.submitRatingButton.isVisible = true
        binding.editReviewButton.isVisible   = false
    }

    private fun showQuantityUnitDialog(ing: IngredientWithPrice) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_quantity_unit, null)
        val qtyInput = dialogView.findViewById<EditText>(R.id.etQty)
        val spinner  = dialogView.findViewById<Spinner>(R.id.spinnerUnit)

        // pick unit list
        val liquidWords  = listOf("milk", "water", "juice", "oil")
        val allowedUnits = if (liquidWords.any { ing.name.contains(it, true) })
            VOLUME_UNITS else WEIGHT_UNITS

        spinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            allowedUnits
        )

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.choose_qty_unit, ing.name))
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val qty    = qtyInput.text.toString().toDoubleOrNull() ?: 1.0
                val unit   = allowedUnits[spinner.selectedItemPosition]
                val factor = UNIT_FACTORS[unit] ?: 1.0


                // ing.price is per-gram/ml
                        val total   = ing.price * (qty * factor)
                        val rounded = total.format(2).toDouble()

                        // strip off the "amount unit" prefix from ing.name,
                        // e.g. "2.0 tsp ginger" → ["2.0","tsp","ginger"] → "ginger"
                        val parts   = ing.name.split(" ")
                        val pureName = if (parts.size > 2)
                               parts.drop(2).joinToString(" ")
                          else
                            ing.name
                        auth.currentUser?.uid?.let { uid ->
                               shoppingVM.addWithPrice(
                                        uid,
                                       ShoppingItem(
                                                name  = "$pureName ($qty $unit)",
                                                price = rounded
                                       )
                                            )
                               Toast.makeText(
                                        requireContext(),
                                        R.string.added_to_shopping_list,
                                        Toast.LENGTH_SHORT
                                            ).show()
                            }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        _binding = null
    }

    /** format a Double to N decimal places */
    private fun Double.format(decimals: Int): String =
        String.format(Locale.getDefault(), "%.${decimals}f", this)
}
