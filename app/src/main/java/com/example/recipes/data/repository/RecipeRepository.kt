// app/src/main/java/com/example/recipes/data/repository/RecipeRepository.kt
package com.example.recipes.data.repository

import android.util.Log
import com.example.recipes.BuildConfig
import com.example.recipes.data.model.Recipe
import com.example.recipes.data.model.RecipeResponse
import com.example.recipes.data.model.Review
import com.example.recipes.network.RetrofitClient
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Response

class RecipeRepository {
    private val api = RetrofitClient.spoonacularService
    private val db = FirebaseFirestore.getInstance()
    private val reviewsCol   = db.collection("reviews")



    /** Fetch full details (including nutrition & instructions) for one recipe */
    suspend fun getRecipeDetails(recipeId: Int): Recipe? = withContext(Dispatchers.IO) {
        val resp = api.getRecipeInformation(
            id                 = recipeId,
            apiKey             = BuildConfig.SPOONACULAR_API_KEY,
            includeNutrition   = true
        )
        resp.body()
    }

    suspend fun saveReview(review: Review) {
        // e.g. "42_user123"
        val docId = "${review.recipeId}_${review.userId}"
        reviewsCol
            .document(docId)
            .set(review, SetOptions.merge())
            .await()
    }

    /**
     * Fetch this user’s review for a recipe, or null if none exists.
     */
    suspend fun getReview(recipeId: Int, userId: String): Review? {
        val docId = "${recipeId}_$userId"
        return reviewsCol
            .document(docId)
            .get()
            .await()
            .toObject(Review::class.java)
    }

    suspend fun searchByCategory(category: String, number: Int = 10): List<Recipe> = withContext(Dispatchers.IO) {
        val resp: Response<RecipeResponse> = api.searchRecipes(
            apiKey = BuildConfig.SPOONACULAR_API_KEY,
            query  = "",
            type   = category.lowercase(),
            number = number
        )
        resp.body()?.results.orEmpty()
    }



    suspend fun searchRecipesWithDietaryFilters(query: String, vegetarian: Boolean, vegan: Boolean, glutenFree: Boolean): List<Recipe> {
        val dietFilters = buildDietQueryString(vegetarian, vegan, glutenFree)
        return searchRecipes(query, dietFilters)
    }

    suspend fun searchRecipes(query: String, dietFilters: String? = null, number: Int = 10): List<Recipe> {
        // Log the URL being requested with diet filters
        Log.d("RecipeRepository", "Requesting recipes with URL: https://api.spoonacular.com/recipes/complexSearch?apiKey=3294a7df48d04932aa410da3d34b382c&query=$query&diet=$dietFilters&number=$number")

        val resp = api.searchRecipes(
            apiKey = BuildConfig.SPOONACULAR_API_KEY,
            query = query,
            diet = dietFilters,
            number = number,
            addInfo = true
        )

        return resp.body()?.results.orEmpty()
    }



    // Helper function to build the diet filter query string
    private fun buildDietQueryString(vegetarian: Boolean, vegan: Boolean, glutenFree: Boolean): String? {
        val filters = mutableListOf<String>()

        if (vegetarian) filters.add("vegetarian")
        if (vegan) filters.add("vegan")
        if (glutenFree) filters.add("gluten-free")

        val dietFilterString = if (filters.isNotEmpty()) filters.joinToString(",") else null
        Log.d("RecipeRepository", "Built diet filter string: $dietFilterString")  // Log the filter string
        return dietFilterString
    }



}
