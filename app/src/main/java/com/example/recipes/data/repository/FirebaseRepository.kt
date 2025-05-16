package com.example.recipes.data.repository

import com.example.recipes.data.model.Recipe
import com.example.recipes.data.model.Review
import com.example.recipes.data.model.ShoppingItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val db           = FirebaseFirestore.getInstance()
    private val favoritesCol = db.collection("favorites")
    private val shoppingCol  = db.collection("shoppingLists")
    private val reviewsCol   = db.collection("reviews")

    /* ----------  Favourites  ---------- */

    suspend fun getFavorites(userId: String): List<Recipe> =
        favoritesCol.document(userId)
            .collection("items")
            .get()
            .await()
            .toObjects(Recipe::class.java)

    suspend fun addFavorite(userId: String, recipe: Recipe) =
        favoritesCol.document(userId)
            .collection("items")
            .document(recipe.id.toString())
            .set(recipe)
            .await()

    suspend fun removeFavorite(userId: String, recipe: Recipe) =
        favoritesCol.document(userId)
            .collection("items")
            .document(recipe.id.toString())
            .delete()
            .await()

    /* ----------  Shopping list  ---------- */

    suspend fun getShoppingList(userId: String): List<ShoppingItem> =
        shoppingCol.document(userId)
            .collection("items")
            .get()
            .await()
            .toObjects(ShoppingItem::class.java)

    suspend fun addShoppingItem(userId: String, item: ShoppingItem): ShoppingItem {
        val newDoc  = shoppingCol.document(userId)
            .collection("items")
            .document()                           // auto-ID
        val stamped = item.copy(id = newDoc.id)
        newDoc.set(stamped).await()
        return stamped
    }

    suspend fun removeShoppingItem(userId: String, item: ShoppingItem) {
        require(item.id.isNotEmpty()) { "ShoppingItem.id is blank" }
        shoppingCol.document(userId)
            .collection("items")
            .document(item.id)
            .delete()
            .await()
    }

    suspend fun removeShoppingItems(userId: String, items: List<ShoppingItem>) {
        if (items.isEmpty()) return
        val batch: WriteBatch = db.batch()
        val baseCol = shoppingCol.document(userId).collection("items")
        items.filter { it.id.isNotEmpty() }
            .forEach { batch.delete(baseCol.document(it.id)) }
        batch.commit().await()
    }

    /* ----------  Reviews  ---------- */

    /** All reviews for a given recipe (any user). */
    suspend fun getAllReviewsForRecipe(recipeId: Int): List<Review> =
        reviewsCol.whereEqualTo("recipeId", recipeId)
            .get()
            .await()
            .toObjects(Review::class.java)

    /** Review for this recipe by this exact user, or null. */
    suspend fun getReview(recipeId: Int, userId: String): Review? =
        reviewsCol.document("${recipeId}-${userId}")
            .get()
            .await()
            .toObject(Review::class.java)

    /** Insert or overwrite a review (now includes userName). */
    suspend fun saveReview(review: Review) =
        reviewsCol.document("${review.recipeId}-${review.userId}")
            .set(review, SetOptions.merge())
            .await()
}
