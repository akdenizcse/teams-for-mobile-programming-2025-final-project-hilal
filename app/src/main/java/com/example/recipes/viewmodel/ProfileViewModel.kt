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

    /** Load flags from SharedPreferences into LiveData */
    fun loadPreferences() {
        val loaded = preferencesHelper.loadPreferences()
        _preferences.value = loaded
        Log.d("ProfileViewModel", "Preferences loaded: $loaded")
    }

    /**
     * Persist user choices and trigger a recipe refresh.
     * @param none        true if the “None” checkbox is selected (overrides the rest)
     * @param vegetarian  user chose Vegetarian
     * @param vegan       user chose Vegan
     * @param glutenFree  user chose Gluten-Free
     */
    fun savePreferences(
        vegetarian: Boolean,
        vegan: Boolean,
        glutenFree: Boolean,
        none: Boolean
    ) {
        // 1) Persist all four flags
        preferencesHelper.savePreferences(
            vegetarian  = vegetarian,
            vegan       = vegan,
            glutenFree  = glutenFree,
            none        = none
        )
        Log.d(
            "ProfileViewModel",
            "Saved → none:$none, vegetarian:$vegetarian, vegan:$vegan, glutenFree:$glutenFree"
        )

        // 2) Refresh recipe list immediately
        searchRecipes(none, vegetarian, vegan, glutenFree)
    }

    /**
     * Query Spoonacular according to current diet flags.
     * When **none == true**, no diet filter is applied.
     */
    private fun searchRecipes(
        none: Boolean,
        vegetarian: Boolean,
        vegan: Boolean,
        glutenFree: Boolean
    ) {
        // Build diet filter string only if none is false
        val filterString = if (none) {
            null
        } else {
            buildList {
                if (vegetarian)  add("vegetarian")
                if (vegan)       add("vegan")
                if (glutenFree)  add("gluten free")
            }.joinToString(",").ifBlank { null }
        }

        Log.d("ProfileViewModel", "Searching recipes with diet = $filterString")

        viewModelScope.launch {
            try {
                val recipes = recipeRepository.searchRecipes(
                    query       = "",
                    dietFilters = filterString
                )
                _filteredRecipes.postValue(recipes)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error fetching recipes", e)
            }
        }
    }
}

