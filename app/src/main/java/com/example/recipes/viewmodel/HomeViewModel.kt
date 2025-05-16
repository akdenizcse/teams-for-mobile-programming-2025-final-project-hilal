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
    private val categoriesList = listOf(
        Category("Breakfast",   R.drawable.baseline_egg_24),
        Category("Lunch",       R.drawable.baseline_food_bank_24),
        Category("Dinner",      R.drawable.baseline_dinner_dining_24),
        Category("Dessert",     R.drawable.baseline_cake_24),
        Category("Vegan",       R.drawable.baseline_grass_24),
        Category("Vegetarian",  R.drawable.baseline_emoji_nature_24),
        Category("Gluten Free", R.drawable.baseline_grain_24),
        Category("Snack",       R.drawable.baseline_coffee_24)
    )
    val categories: LiveData<List<Category>> = MutableLiveData(categoriesList)

    // Backing LiveData for the recipes to show in the grid
    private val _recipes = MutableLiveData<List<Recipe>>(emptyList())
    val recipes: LiveData<List<Recipe>> = _recipes

    init {
        // initial “Quick & Easy” load with no diet filter
        loadQuickEasy(null)
    }

    /**
     * Load default “Quick & Easy” recipes,
     * optionally filtered by diet (e.g. "vegan").
     */
    fun loadQuickEasy(diet: String?) {
        viewModelScope.launch {
            val list = repo.searchRecipes(
                query = "quick easy",
                dietFilters = diet,
                number = 6
            )
            _recipes.postValue(list)
        }
    }

    /**
     * User tapped a category chip.
     * Loads that category name, optionally filtered by diet.
     */
    fun fetchByCategory(category: String, diet: String? = null) {
        viewModelScope.launch {
            val list = repo.searchRecipes(
                query = category,
                dietFilters = diet,
                number = 20
            )
            _recipes.postValue(list)
        }
    }

    /**
     * User submitted text in the home‐screen search box.
     * Performs a search for the text, optionally filtered by diet.
     */
    fun searchHomeRecipes(query: String, diet: String? = null) {
        viewModelScope.launch {
            val list = repo.searchRecipes(
                query = query.trim(),
                dietFilters = diet,
                number = 20
            )
            _recipes.postValue(list)
        }
    }
}
