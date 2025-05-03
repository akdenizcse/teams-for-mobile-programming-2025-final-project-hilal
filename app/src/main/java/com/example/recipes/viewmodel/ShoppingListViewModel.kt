package com.example.recipes.viewmodel

import androidx.lifecycle.*
import com.example.recipes.data.model.ShoppingItem
import com.example.recipes.data.repository.FirebaseRepository
import kotlinx.coroutines.launch

class ShoppingListViewModel(
    private val repo: FirebaseRepository = FirebaseRepository()
) : ViewModel() {

    private val _items = MutableLiveData<List<ShoppingItem>>(emptyList())
    val items: LiveData<List<ShoppingItem>> = _items

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun load(userId: String) = viewModelScope.launch {
        try { _items.value = repo.getShoppingList(userId) }
        catch(e: Exception) { _error.value = "Could not load: ${e.message}" }
    }

    fun add(userId: String, item: ShoppingItem) = viewModelScope.launch {
        try {
            repo.addShoppingItem(userId, item)
            load(userId)
        } catch(e: Exception) {
            _error.value = "Could not add: ${e.message}"
        }
    }

    fun removeItems(userId: String, toDelete: List<ShoppingItem>) = viewModelScope.launch {
        try {
            toDelete.forEach { repo.removeShoppingItem(userId, it) }
            load(userId)
        } catch(e: Exception) {
            _error.value = "Could not delete selected: ${e.message}"
        }
    }

    fun clearError() { _error.value = null }
}
