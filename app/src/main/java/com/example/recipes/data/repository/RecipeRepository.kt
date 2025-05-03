package com.example.recipes.data.repository

import com.example.recipes.BuildConfig
import com.example.recipes.data.model.Recipe
import com.example.recipes.data.model.RecipeResponse
import com.example.recipes.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response


class RecipeRepository {

    // Your Retrofit service
    private val api = RetrofitClient.spoonacularService

    /**
     * Search recipes by keyword and optional diet filters (comma-separated).
     */
    suspend fun searchRecipes(
        query: String,
        dietFilters: String? = null,
        number: Int = 10
    ): List<Recipe> = withContext(Dispatchers.IO) {
        val response = api.searchRecipes(
            apiKey   = BuildConfig.SPOONACULAR_API_KEY,
            query    = query,
            diet     = dietFilters,
            type     = null,
            number   = number,
            addInfo  = true               // <— ensure we ask for nutrition too
        )
        response.body()?.results ?: emptyList()
    }


    /**
     * Convenience for searching by category (e.g. “Breakfast”, “Dinner”).
     */
    suspend fun searchByCategory(
        category: String,
        number: Int = 10
    ): List<Recipe> {
        return withContext(Dispatchers.IO) {
            val response: Response<RecipeResponse> = this@RecipeRepository.api.searchRecipes(
                apiKey = BuildConfig.SPOONACULAR_API_KEY,
                query  = "",
                diet   = null,
                type   = category.lowercase(),  // use `lowercase()` instead of deprecated `toLowerCase()`
                number = number
            )
            response.body()?.results ?: emptyList()
        }
    }


    suspend fun getRecipeDetails(recipeId: Int): Recipe? = withContext(Dispatchers.IO) {
        val resp = api.getRecipeInformation(
            id          = recipeId,
            apiKey      = BuildConfig.SPOONACULAR_API_KEY,
            includeNutrition = true
        )
        resp.body()
}
}
