// app/src/main/java/com/example/recipes/viewmodel/FavoritesViewModel.kt
package com.example.recipes.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipes.data.model.Recipe
import com.example.recipes.data.repository.FirebaseRepository
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val firebaseRepo: FirebaseRepository = FirebaseRepository()
) : ViewModel() {

    // Backing MutableLiveData for the favorites list (starts empty)
    private val _favoritesList = MutableLiveData<List<Recipe>>(emptyList())
    val favoritesList: LiveData<List<Recipe>> = _favoritesList

    // Error channel for UI to observe
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    /**
     * Load the user’s favorites from Firestore.
     */
    fun loadFavorites(userId: String) {
        viewModelScope.launch {
            try {
                val results = firebaseRepo.getFavorites(userId)
                _favoritesList.postValue(results)
            } catch (e: Exception) {
                _error.postValue("Failed to load favorites: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Add a recipe to favorites remotely and then update the local list.
     */
    fun addFavorite(userId: String, recipe: Recipe) {
        viewModelScope.launch {
            try {
                firebaseRepo.addFavorite(userId, recipe)
                // Prepend to the list if not already present
                val updated = _favoritesList.value
                    .orEmpty()
                    .toMutableList()
                if (updated.none { it.id == recipe.id }) {
                    updated.add(0, recipe)
                    _favoritesList.postValue(updated)
                }
            } catch (e: Exception) {
                _error.postValue("Failed to add favorite: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Remove a recipe from favorites remotely and then update the local list.
     */
    // after
    fun removeFavorite(userId: String, recipe: Recipe) {
        // 1) Immediately update the UI
        val updated = _favoritesList.value.orEmpty()
            .filterNot { it.id == recipe.id }
        _favoritesList.value = updated

        // 2) Then kick off the Firestore delete in the background
        viewModelScope.launch {
            try {
                firebaseRepo.removeFavorite(userId, recipe)
            } catch (e: Exception) {
                // If you want, you can re-insert it on error or surface an error message
                _error.postValue("Could not remove from server: ${e.localizedMessage}")
            }
        }
    }


    /** Clear any currently posted error. */
    fun clearError() {
        _error.value = null
    }
}
