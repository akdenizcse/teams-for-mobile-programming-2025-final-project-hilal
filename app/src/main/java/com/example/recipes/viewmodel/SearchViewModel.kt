// app/src/main/java/com/example/recipes/viewmodel/SearchViewModel.kt
package com.example.recipes.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipes.data.model.Recipe
import com.example.recipes.data.repository.RecipeRepository
import kotlinx.coroutines.launch

class SearchViewModel(
    private val repo: RecipeRepository = RecipeRepository()
) : ViewModel() {
    /** Remember last search term so we can re-execute with new filters */
    var currentQuery: String = ""

    // Recipes stream
    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes

    // Loading indicator
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error messages
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /** Clear any shown error */
    fun clearError() {
        _error.value = null
    }

    /** Plain‐text search */
    fun searchRecipes(query: String) {
        // store the term
        currentQuery = query

        _isLoading.value = true
        viewModelScope.launch {
            try {
                // use stored query + currentFilters
                val results = repo.searchRecipes(currentQuery, currentFilters)
                _recipes.value = results
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Category‐based search */
    fun searchByCategory(category: String, number: Int = 10) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val results = repo.searchByCategory(category, number)
                _recipes.value = results
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Current filter string (e.g. "vegetarian, gluten free") */
    var currentFilters: String? = null

    /** Update filters (could re‐invoke last query if you store it) */
    fun applyFilters(filters: String) {
        // 1) Save the filters (null if blank)
        currentFilters = filters.ifBlank { null }
        // 2) Immediately re‐invoke the search with whatever query was last used
        searchRecipes(currentQuery)
    }
}
