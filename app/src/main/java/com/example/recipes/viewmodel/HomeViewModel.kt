package com.example.recipes.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipes.R
import com.example.recipes.data.model.Category
import com.example.recipes.data.model.Recipe
import com.example.recipes.data.repository.RecipeRepository
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repo: RecipeRepository = RecipeRepository()
) : ViewModel() {

    private val categoriesList = listOf(
        Category("Breakfast", R.drawable.baseline_egg_24),
        Category("Lunch",     R.drawable.baseline_food_bank_24),
        Category("Dinner",    R.drawable.baseline_dinner_dining_24),
        Category("Dessert",   R.drawable.baseline_cake_24),
        Category("Snack",     R.drawable.baseline_coffee_24)
    )
    val categories: LiveData<List<Category>> = MutableLiveData(categoriesList)

    private val _recipes   = MutableLiveData<List<Recipe>>(emptyList())
    val recipes: LiveData<List<Recipe>> = _recipes

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error     = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadQuickEasy(null)
    }

    fun loadQuickEasy(diet: String?) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val list = repo.searchRecipes(
                    query       = "quick easy",
                    dietFilters = diet,
                    number      = 6
                )
                _recipes.postValue(list)
            } catch (e: Exception) {
                _error.postValue("Failed to load Quick & Easy: ${e.localizedMessage}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun fetchByCategory(category: String, diet: String? = null) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val list = repo.searchRecipes(
                    query       = category,
                    dietFilters = diet,
                    number      = 20
                )
                _recipes.postValue(list)
            } catch (e: Exception) {
                _error.postValue("Failed to load $category: ${e.localizedMessage}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun searchHomeRecipes(query: String, diet: String? = null) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val list = repo.searchRecipes(
                    query       = query.trim(),
                    dietFilters = diet,
                    number      = 20
                )
                _recipes.postValue(list)
            } catch (e: Exception) {
                _error.postValue("Search failed: ${e.localizedMessage}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
