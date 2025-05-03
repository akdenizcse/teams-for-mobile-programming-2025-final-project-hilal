package com.example.recipes.network

import com.example.recipes.data.model.Recipe
import com.example.recipes.data.model.RecipeResponse
import retrofit2.Response         // ← add this import
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path


interface SpoonacularApi {
    @GET("recipes/complexSearch")
    suspend fun searchRecipes(
        @Query("apiKey") apiKey: String,
        @Query("query")   query: String,
        @Query("diet")    diet: String?    = null,
        @Query("type")    type: String?    = null,
        @Query("number")  number: Int      = 10,
        @Query("addRecipeInformation") addInfo: Boolean = true
    ): Response<RecipeResponse>         // now Response is resolved

    @GET("recipes/{id}/information")
    suspend fun getRecipeInformation(
        @Path("id") id: Int,
        @Query("apiKey") apiKey: String,
        @Query("includeNutrition") includeNutrition: Boolean = true
    ): Response<Recipe>

}
