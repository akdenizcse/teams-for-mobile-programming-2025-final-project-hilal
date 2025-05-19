// app/src/main/java/com/example/recipes/viewmodel/CartViewModel.kt
package com.example.recipes.viewmodel

import androidx.lifecycle.*
import com.example.recipes.data.model.CartItem
import com.example.recipes.data.model.Recipe
import com.example.recipes.data.repository.FirebaseRepository
import kotlinx.coroutines.launch
import kotlin.random.Random

class CartViewModel(
    private val repo: FirebaseRepository = FirebaseRepository()
) : ViewModel() {

    private val _items = MutableLiveData<List<CartItem>>(emptyList())
    val items: LiveData<List<CartItem>> = _items

    val totalPrice: LiveData<Double> = _items.map { it.sumOf { ci -> ci.price } }

    fun load(userId: String) = viewModelScope.launch {
        _items.value = repo.getCart(userId)
    }

    /**
     * Create a single CartItem whose price is the sum of all ingredient‐prices.
     * We generate a random price per ingredient, sum them, and save one CartItem.
     */
    fun addRecipe(userId: String, recipe: Recipe) = viewModelScope.launch {
        // 1) generate per‐ingredient random prices and sum
        val ingredientPrices = recipe.ingredients.orEmpty().map {
            Random.nextInt(from = 4, until = 21) * 0.5  // 2.0,2.5…10.0 per ingredient
        }
        val total = ingredientPrices.sum()

        // 2) build one CartItem
        val cartItem = CartItem(
            recipeId = recipe.id,
            title    = recipe.title,
            image    = recipe.image,
            price    = total
        )

        // 3) save & reload
        repo.addToCart(userId, cartItem)
        load(userId)
    }

    fun removeItem(userId: String, item: CartItem) = viewModelScope.launch {
        repo.removeFromCart(userId, item)
        load(userId)
    }

    fun checkout(userId: String, onSuccess: () -> Unit = {}) = viewModelScope.launch {
        repo.clearCart(userId)
        load(userId)
        onSuccess()
    }
}
