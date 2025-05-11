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

    // 2) Backing state for "quick & easy" and for the current search query
    private val _quickEasy   = MutableLiveData<List<Recipe>>(emptyList())
    private val _searchQuery = MutableLiveData("")

    // 3) Exposed recipes: when query is blank → quickEasy feed; else → liveData search
    val recipes: LiveData<List<Recipe>> = _searchQuery.switchMap { query ->
        if (query.isBlank()) {
            _quickEasy
        } else {
            liveData {
                val list = runCatching {
                    repo.searchRecipes(query = query, number = 10)
                }.getOrDefault(emptyList())
                emit(list)
            }
        }
    }

    init {
        loadQuickEasy()
    }

    /** Load default “Quick & Easy” recipes */
    fun loadQuickEasy() {
        viewModelScope.launch {
            val list = runCatching {
                repo.searchRecipes(query = "quick easy", number = 6)
            }.getOrDefault(emptyList())
            _quickEasy.value = list
        }
    }

    /**
     * User tapped one of the category chips.
     * Clears any pending search and loads that category.
     */
    fun onCategorySelected(name: String) {
        _searchQuery.value = ""
        viewModelScope.launch {
            val list = runCatching {
                repo.searchByCategory(name, number = 6)
            }.getOrDefault(emptyList())
            _quickEasy.value = list
        }
    }

    /** User submitted text in the home‐screen search box */
    fun searchHomeRecipes(query: String) {
        _searchQuery.value = query.trim()
    }
}
