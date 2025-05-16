// app/src/main/java/com/example/recipes/data/repository/FirebaseRepository.kt
package com.example.recipes.data.repository

import com.example.recipes.data.model.Recipe
import com.example.recipes.data.model.Review
import com.example.recipes.data.model.ShoppingItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val favoritesCol = db.collection("favorites")
    private val shoppingCol  = db.collection("shoppingLists")
    private val reviewsCol   = db.collection("reviews")


    suspend fun getFavorites(userId: String): List<Recipe> =
        favoritesCol
            .document(userId)
            .collection("items")
            .get()
            .await()
            .toObjects(Recipe::class.java)

    suspend fun addFavorite(userId: String, recipe: Recipe) {
        favoritesCol
            .document(userId)
            .collection("items")
            .document(recipe.id.toString())
            .set(recipe)
            .await()
    }

    suspend fun removeFavorite(userId: String, recipe: Recipe) {
        favoritesCol
            .document(userId)
            .collection("items")
            .document(recipe.id.toString())
            .delete()
            .await()
    }

    // —————————————————————————————————————————————————————————————————————————————
    // Shopping List
    // —————————————————————————————————————————————————————————————————————————————

    /** Get the shopping list for a user */
    suspend fun getShoppingList(userId: String): List<ShoppingItem> =
        shoppingCol
            .document(userId)
            .collection("items")
            .get()
            .await()
            .toObjects(ShoppingItem::class.java)

    /**
     * Add an item:
     * 1) generate a fresh document ID
     * 2) bake it into the model
     * 3) persist
     * 4) return the copy with its real `.id`
     */
    suspend fun addShoppingItem(userId: String, item: ShoppingItem): ShoppingItem {
        val col     = shoppingCol.document(userId).collection("items")
        val newDoc  = col.document()                // auto‐ID
        val toStore = item.copy(id = newDoc.id)     // stamp ID
        newDoc.set(toStore).await()                 // save
        return toStore                              // return with ID
    }

    /**
     * Remove exactly that one item by its stamped ID.
     * Throws if `item.id` is blank.
     */
    suspend fun removeShoppingItem(userId: String, item: ShoppingItem) {
        require(item.id.isNotEmpty()) {
            "Cannot delete ShoppingItem without a valid ID"
        }
        shoppingCol
            .document(userId)
            .collection("items")
            .document(item.id)
            .delete()
            .await()
    }

    /**
     * NEW: Atomically delete a batch of ShoppingItems.
     * This uses a single Firestore WriteBatch.commit().
     */
    suspend fun removeShoppingItems(userId: String, items: List<ShoppingItem>) {
        if (items.isEmpty()) return

        val batch: WriteBatch = db.batch()
        val baseCol = shoppingCol.document(userId).collection("items")
        for (item in items) {
            if (item.id.isNotEmpty()) {
                batch.delete(baseCol.document(item.id))
            }
        }
        batch.commit().await()
    }


    /**
     * Fetch all user reviews for a recipe by querying where recipeId equals the given id.
     */
    suspend fun getAllReviewsForRecipe(recipeId: Int): List<Review> {
        return reviewsCol
            .whereEqualTo("recipeId", recipeId)
            .get()
            .await()
            .toObjects(Review::class.java)
    }

    /**
     * Fetch this user’s review for a recipe, or null if none exists.
     */
    suspend fun getReview(recipeId: Int, userId: String): Review? {
        val docId = "${recipeId}-$userId"
        return reviewsCol
            .document(docId)
            .get()
            .await()
            .toObject(Review::class.java)
    }

    /**
     * Save or overwrite a user’s review on a recipe
     */
    suspend fun saveReview(review: Review) {
        val key = "${review.recipeId}-${review.userId}"
        reviewsCol
            .document(key)
            .set(review)
            .await()
    }
}
