package com.example.recipes.viewmodel

import androidx.lifecycle.*
import com.example.recipes.R
import com.example.recipes.data.model.Category
import com.example.recipes.data.model.Recipe
import com.example.recipes.data.repository.RecipeRepository
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repo: RecipeRepository = RecipeRepository()
) : ViewModel() {

    // 1) Eight static categories
    private val _categories = MutableLiveData<List<Category>>(
        listOf(
            Category("Breakfast",   R.drawable.baseline_egg_24),
            Category("Lunch",       R.drawable.baseline_food_bank_24),
            Category("Dinner",      R.drawable.baseline_dinner_dining_24),
            Category("Dessert",     R.drawable.baseline_cake_24),
            Category("Vegan",       R.drawable.baseline_grass_24),
            Category("Vegetarian",  R.drawable.baseline_emoji_nature_24),
            Category("Gluten Free", R.drawable.baseline_grain_24),
            Category("Snack",       R.drawable.baseline_coffee_24)
        )
    )
    val categories: LiveData<List<Category>> = _categories

    // 2) “Quick & Easy” feed
    private val _quickEasy = MutableLiveData<List<Recipe>>(emptyList())
    val quickEasy: LiveData<List<Recipe>> = _quickEasy

    // 3) Search‐from‐home results
    private val _searchResults = MutableLiveData<List<Recipe>>(emptyList())
    val searchResults: LiveData<List<Recipe>> = _searchResults

    init {
        loadQuickEasy()
    }

    /** Initial or “Explore” refresh */
    fun loadQuickEasy() {
        viewModelScope.launch {
            runCatching {
                repo.searchRecipes(query = "quick easy", number = 6)
            }.onSuccess { _quickEasy.value = it }
                .onFailure { _quickEasy.value = emptyList() }
        }
    }

    /** Chip or grid‐tile taps */
    fun onCategorySelected(name: String) {
        viewModelScope.launch {
            runCatching {
                repo.searchByCategory(name, number = 6)
            }.onSuccess { _quickEasy.value = it }
                .onFailure { _quickEasy.value = emptyList() }
        }
    }

    /** The home‐screen search box */
    fun searchHomeRecipes(query: String) {
        if (query.isBlank()) {
            _searchResults.value = _quickEasy.value  // fallback
            return
        }
        viewModelScope.launch {
            runCatching {
                repo.searchRecipes(query = query, number = 10)
            }.onSuccess { _searchResults.value = it }
                .onFailure { _searchResults.value = emptyList() }
        }
    }
}
