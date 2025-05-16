package com.example.recipes.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipes.data.model.Recipe
import com.example.recipes.data.repository.RecipeRepository
import com.example.recipes.data.storage.PreferencesHelper
import kotlinx.coroutines.launch

class SearchViewModel(
    private val repo: RecipeRepository = RecipeRepository(),
    private val prefs: PreferencesHelper
) : ViewModel() {

    /** Remember last search term so we can re-execute with new diet filter */
    private var currentQuery: String = ""

    // — Recipes stream
    private val _recipes   = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> get() = _recipes

    // — Loading indicator
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // — Error messages
    private val _error     = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /** Clear any shown error */
    fun clearError() {
        _error.value = null
    }

    /**
     * Perform a free-text search, automatically including the user’s
     * saved diet preference (if any).
     */
    fun searchRecipes(query: String) {
        currentQuery = query.trim()
        val dietFilter = prefs.getDietQuery()    // e.g. "vegan" or null

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val results = repo.searchRecipes(
                    query       = currentQuery,
                    dietFilters = dietFilter,
                    number      = 20
                )
                _recipes.value = results
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Perform a category-based search, automatically including the
     * user’s saved diet preference.
     */
    fun searchByCategory(category: String, number: Int = 10) {
        val dietFilter = prefs.getDietQuery()

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val results = repo.searchRecipes(
                    query       = category,
                    dietFilters = dietFilter,
                    number      = number
                )
                _recipes.value = results
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Re-run the last free-text search (useful if the user changes
     * their diet preference and wants to refresh results).
     */
    fun refreshLastSearch() {
        if (currentQuery.isNotBlank()) {
            searchRecipes(currentQuery)
        }
    }
}
