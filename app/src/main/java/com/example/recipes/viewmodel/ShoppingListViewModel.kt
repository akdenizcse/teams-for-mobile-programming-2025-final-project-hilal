// app/src/main/java/com/example/recipes/viewmodel/ShoppingListViewModel.kt
package com.example.recipes.viewmodel

import androidx.lifecycle.*
import com.example.recipes.data.model.ShoppingItem
import com.example.recipes.data.repository.FirebaseRepository
import kotlinx.coroutines.launch
import kotlin.random.Random

class ShoppingListViewModel(
    private val repo: FirebaseRepository = FirebaseRepository()
) : ViewModel() {

    /* ---------- state ---------- */

    private val _items = MutableLiveData<List<ShoppingItem>>(emptyList())
    val items: LiveData<List<ShoppingItem>> = _items

    /** Reactive total price */
    val totalPrice: LiveData<Double> = _items.map { list ->
        list.sumOf { it.price }
    }

    /* ---------- pricing helpers ---------- */

    /** Generates a random half-dollar price between $2.0 and $10.0 */
    private fun randomPrice(): Double =
        Random.nextInt(from = 4, until = 21) * 0.5   // 2.0, 2.5 … 10.0

    /* ---------- public API ---------- */

    /** Add an item and assign it a random price (for ad-hoc ingredients). */
    fun add(userId: String, item: ShoppingItem) = viewModelScope.launch {
        val stamped = item.copy(price = randomPrice())
        repo.addShoppingItem(userId, stamped)
        load(userId)
    }

    /**
     * Add an item **keeping** its existing `price`.
     * Use this when you have already calculated the dish price
     * (e.g. sum of ingredient prices).
     */
    fun addWithPrice(userId: String, item: ShoppingItem) = viewModelScope.launch {
        repo.addShoppingItem(userId, item)   // keep item.price as-is
        load(userId)
    }

    /** Remove a batch */
    fun removeItems(userId: String, items: List<ShoppingItem>) = viewModelScope.launch {
        repo.removeShoppingItems(userId, items)
        load(userId)
    }

    /** Clear entire list */
    fun clearAll(userId: String) = viewModelScope.launch {
        repo.removeShoppingItems(userId, _items.value ?: emptyList())
        load(userId)
    }

    /** Reload from Firestore */
    fun load(userId: String) = viewModelScope.launch {
        _items.value = repo.getShoppingList(userId)
    }
}
