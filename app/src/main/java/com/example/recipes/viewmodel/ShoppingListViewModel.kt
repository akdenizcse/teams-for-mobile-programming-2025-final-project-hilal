package com.example.recipes.viewmodel

import androidx.lifecycle.*
import com.example.recipes.data.model.ShoppingItem
import com.example.recipes.data.repository.FirebaseRepository
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

class ShoppingListViewModel(
    private val repo: FirebaseRepository = FirebaseRepository()
) : ViewModel() {

    // Current list of items
    private val _items = MutableLiveData<List<ShoppingItem>>(emptyList())
    val items: LiveData<List<ShoppingItem>> = _items

    // Total price of the list
    val totalPrice: LiveData<Double> = _items.map { list ->
        list.sumOf { it.price }
    }

    // Master set of valid dish & ingredient names
    private val _validItems = MutableLiveData<Set<String>>(emptySet())
    val validItems: LiveData<Set<String>> = _validItems

    /** Populate the valid-names set once on startup. */
    fun setValidItems(names: Set<String>) {
        _validItems.value = names.map { it.trim().lowercase(Locale.ROOT) }.toSet()
    }

    /** Generate a random half-dollar price between 2.0 and 10.0 */
    private fun randomPrice(): Double =
        Random.nextInt(from = 4, until = 21) * 0.5

    /** Add an item with a random price. */
    fun add(userId: String, item: ShoppingItem) = viewModelScope.launch {
        val stamped = item.copy(price = randomPrice())
        repo.addShoppingItem(userId, stamped)
        load(userId)
    }

    /** Add an item keeping its given price. */
    fun addWithPrice(userId: String, item: ShoppingItem) = viewModelScope.launch {
        repo.addShoppingItem(userId, item)
        load(userId)
    }

    /** Remove one or more items. */
    fun removeItems(userId: String, items: List<ShoppingItem>) = viewModelScope.launch {
        repo.removeShoppingItems(userId, items)
        load(userId)
    }

    /** Clear the entire list. */
    fun clearAll(userId: String) = viewModelScope.launch {
        repo.removeShoppingItems(userId, _items.value ?: emptyList())
        load(userId)
    }

    /** Reload from Firestore. */
    fun load(userId: String) = viewModelScope.launch {
        _items.value = repo.getShoppingList(userId)
    }
}
