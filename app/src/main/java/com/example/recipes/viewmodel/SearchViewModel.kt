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

    private var currentQuery: String = ""
    private val _recipes   = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error     = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun clearError() {
        _error.value = null
    }

    fun searchRecipes(query: String) {
        currentQuery = query.trim()
        val dietFilter = prefs.getDietQuery()

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
                _error.value = "Search failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

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
                _error.value = "Category search failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshLastSearch() {
        if (currentQuery.isNotBlank()) searchRecipes(currentQuery)
    }
}
