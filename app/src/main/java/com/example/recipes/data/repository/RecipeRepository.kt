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



    /** Full details (nutrition + instructions) for one recipe. */
    suspend fun getRecipeDetails(recipeId: Int): Recipe? = withContext(Dispatchers.IO) {
        api.getRecipeInformation(
            id               = recipeId,
            apiKey           = BuildConfig.SPOONACULAR_API_KEY,
            includeNutrition = true
        ).body()
    }


    /**
     * Fetch this user’s review for a recipe, or null if none exists.
     */


    suspend fun searchRecipes(
        query: String,
        dietFilters: String? = null,
        number: Int = 10
    ): List<Recipe> = withContext(Dispatchers.IO) {

        Log.d(
            "RecipeRepository",
            "searchRecipes query='$query' diet='$dietFilters' number=$number"
        )

        api.searchRecipes(
            apiKey  = BuildConfig.SPOONACULAR_API_KEY,
            query   = query,
            diet    = dietFilters,
            number  = number,
            addInfo = true
        ).body()?.results.orEmpty()
    }

    // In any suspend repository method (e.g. RecipeRepository):
    suspend fun safeGetRecipeDetails(recipeId: Int): Result<Recipe> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getRecipeInformation(recipeId, BuildConfig.SPOONACULAR_API_KEY, true)
            response.body() ?: throw IllegalStateException("Empty body")
        }
    }


}
