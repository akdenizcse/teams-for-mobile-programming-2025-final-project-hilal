package com.example.recipes.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipes.data.model.Recipe
import com.example.recipes.data.repository.RecipeRepository
import com.example.recipes.data.storage.PreferencesHelper
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val preferencesHelper: PreferencesHelper,
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _preferences = MutableLiveData<Map<String, Boolean>>()
    val preferences: LiveData<Map<String, Boolean>> = _preferences

    private val _filteredRecipes = MutableLiveData<List<Recipe>>()
    val filteredRecipes: LiveData<List<Recipe>> = _filteredRecipes

    // Load preferences from SharedPreferences
    fun loadPreferences() {
        val loadedPreferences = preferencesHelper.loadPreferences()
        _preferences.value = loadedPreferences
        Log.d("ProfileViewModel", "Preferences loaded: $loadedPreferences")  // Log the loaded preferences
    }

    // Save dietary preferences and trigger filtering
    fun savePreferences(vegetarian: Boolean, vegan: Boolean, glutenFree: Boolean) {
        preferencesHelper.savePreferences(vegetarian, vegan, glutenFree)

        // Log the saved preferences to verify
        Log.d("ProfileViewModel", "Preferences saved: Vegetarian: $vegetarian, Vegan: $vegan, Gluten-Free: $glutenFree")

        // After saving preferences, trigger recipe filtering
        searchRecipes(vegetarian, vegan, glutenFree)
    }

    // Search recipes based on dietary preferences


    fun searchRecipes(vegetarian: Boolean, vegan: Boolean, glutenFree: Boolean) {
        val dietFilters = mutableListOf<String>()

        // Add preferences to the diet filters list
        if (vegetarian) {
            dietFilters.add("vegetarian")
        }
        if (vegan) {
            dietFilters.add("vegan")
        }
        if (glutenFree) {
            dietFilters.add("glutenFree")
        }

        // Combine all dietary preferences into one comma-separated string
        val filterString = dietFilters.joinToString(",")
        Log.d("ProfileViewModel", "Searching with diet filter: $filterString")  // Log the filter string

        // Fetch the recipes using the filter string
        viewModelScope.launch {
            try {
                val recipes = recipeRepository.searchRecipes("", dietFilters = filterString)
                Log.d("ProfileViewModel", "Received recipes: $recipes")  // Log the received recipes

                // Update the filteredRecipes LiveData
                _filteredRecipes.postValue(recipes)
            } catch (e: Exception) {
                // Log any errors encountered during the API call
                Log.e("ProfileViewModel", "Error fetching recipes", e)
            }
        }
    }

}
