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
    private val _categories = listOf(
        Category("Breakfast",   R.drawable.baseline_egg_24),
        Category("Lunch",       R.drawable.baseline_food_bank_24),
        Category("Dinner",      R.drawable.baseline_dinner_dining_24),
        Category("Dessert",     R.drawable.baseline_cake_24),
        Category("Vegan",       R.drawable.baseline_grass_24),
        Category("Vegetarian",  R.drawable.baseline_emoji_nature_24),
        Category("Gluten Free", R.drawable.baseline_grain_24),
        Category("Snack",       R.drawable.baseline_coffee_24)
    )
    val categories: LiveData<List<Category>> = MutableLiveData(_categories)

    // 2) Internal feeds
    private val _quickEasy   = MutableLiveData<List<Recipe>>(emptyList())
    private val _searchQuery = MutableLiveData<String>("")

    // 3) Exposed “recipes” stream: if search query blank → quickEasy, else → search results
    val recipes: LiveData<List<Recipe>> = _searchQuery.switchMap { q ->
        if (q.isBlank()) {
            _quickEasy
        } else {
            liveData {
                val list = runCatching {
                    repo.searchRecipes(query = q, number = 10)
                }.getOrNull() ?: emptyList()
                emit(list)
            }
        }
    }

    init {
        loadQuickEasy()
    }

    /** Load default “quick & easy” recipes. */
    fun loadQuickEasy() {
        viewModelScope.launch {
            val list = runCatching {
                repo.searchRecipes(query = "quick easy", number = 6)
            }.getOrDefault(emptyList())
            _quickEasy.value = list
        }
    }

    /** Called when user taps a category chip. */
    fun onCategorySelected(name: String) {
        _searchQuery.value = ""          // clear any search
        viewModelScope.launch {
            val list = runCatching {
                repo.searchByCategory(name, number = 6)
            }.getOrDefault(emptyList())
            _quickEasy.value = list     // repurpose quickEasy
        }
    }

    /** Called when user submits a search. */
    fun searchHomeRecipes(query: String) {
        _searchQuery.value = query
    }
}
