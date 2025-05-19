// app/src/main/java/com/example/recipes/data/repository/FirebaseRepository.kt
package com.example.recipes.data.repository

import com.example.recipes.data.model.CartItem
import com.example.recipes.data.model.Comment
import com.example.recipes.data.model.Order
import com.example.recipes.data.model.Recipe
import com.example.recipes.data.model.Review
import com.example.recipes.data.model.ShoppingItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val db           = FirebaseFirestore.getInstance()
    private val favoritesCol = db.collection("favorites")
    private val shoppingCol  = db.collection("shoppingLists")
    private val reviewsCol   = db.collection("reviews")
    private val cartsCol     = db.collection("carts")
    private val commentsCol  = db.collection("comments")

    /* ----------  COMMENTS (threaded)  ---------- */

    /** One top‐level comment per (recipeId, userId) */
    suspend fun getMyComment(recipeId: Int, userId: String): Comment? {
        return commentsCol
            .whereEqualTo("recipeId", recipeId)
            .whereEqualTo("userId", userId)
            .get().await()
            .toObjects(Comment::class.java)
            .firstOrNull()
    }

    /** Inserts new or updates existing comment (top‐level or reply) */
    suspend fun upsertComment(c: Comment) {
        if (c.id.isBlank()) {
            // new
            val doc = commentsCol.document()
            doc.set(c.copy(id = doc.id), SetOptions.merge()).await()
        } else {
            // update
            commentsCol.document(c.id).set(c, SetOptions.merge()).await()
        }
    }

    /** All comments & replies for a given recipe */
    suspend fun getCommentsForRecipe(recipeId: Int): List<Comment> {
        return commentsCol
            .whereEqualTo("recipeId", recipeId)
            .get().await()
            .toObjects(Comment::class.java)
            .sortedBy { it.timestamp }
    }


    suspend fun saveReply(c: Comment) {
        val doc = commentsCol.document()
        doc.set(c.copy(id = doc.id), SetOptions.merge()).await()
    }



    /** Save a new comment or reply */
    suspend fun saveComment(c: Comment) {
        if (c.id.isBlank()) {
            // new comment
            val doc = commentsCol.document()
            doc.set(c.copy(id = doc.id)).await()
        } else {
            // existing comment (if you ever support editing top‐level ones)
            commentsCol.document(c.id).set(c, SetOptions.merge()).await()
        }
    }

    /* ----------  Favourites  ---------- */

    suspend fun getFavorites(userId: String): List<Recipe> =
        favoritesCol.document(userId).collection("items")
            .get().await().toObjects(Recipe::class.java)

    suspend fun addFavorite(userId: String, recipe: Recipe) =
        favoritesCol.document(userId).collection("items")
            .document(recipe.id.toString())
            .set(recipe).await()

    suspend fun removeFavorite(userId: String, recipe: Recipe) =
        favoritesCol.document(userId).collection("items")
            .document(recipe.id.toString())
            .delete().await()

    /* ----------  Shopping list  ---------- */

    suspend fun getShoppingList(userId: String): List<ShoppingItem> =
        shoppingCol.document(userId).collection("items")
            .get().await().toObjects(ShoppingItem::class.java)

    suspend fun addShoppingItem(userId: String, item: ShoppingItem): ShoppingItem {
        val newDoc  = shoppingCol.document(userId).collection("items").document()
        val stamped = item.copy(id = newDoc.id)
        newDoc.set(stamped).await()
        return stamped
    }

    suspend fun removeShoppingItem(userId: String, item: ShoppingItem) {
        require(item.id.isNotEmpty()) { "ShoppingItem.id is blank" }
        shoppingCol.document(userId).collection("items")
            .document(item.id).delete().await()
    }

    suspend fun removeShoppingItems(userId: String, items: List<ShoppingItem>) {
        if (items.isEmpty()) return
        val batch = db.batch()
        val base  = shoppingCol.document(userId).collection("items")
        items.filter { it.id.isNotEmpty() }
            .forEach { batch.delete(base.document(it.id)) }
        batch.commit().await()
    }

    /* ----------  Reviews (ratings)  ---------- */

    suspend fun getReview(recipeId: Int, userId: String): Review? =
        reviewsCol.document("$recipeId-$userId")
            .get().await().toObject(Review::class.java)

    suspend fun saveReview(review: Review) =
        reviewsCol.document("${review.recipeId}-${review.userId}")
            .set(review, SetOptions.merge()).await()

    /* ----------  Cart (simulated purchase)  ---------- */

    suspend fun getCart(userId: String): List<CartItem> =
        cartsCol.document(userId).collection("items")
            .get().await().toObjects(CartItem::class.java)

    suspend fun addToCart(userId: String, item: CartItem): CartItem {
        val newDoc  = cartsCol.document(userId).collection("items").document()
        val stamped = item.copy(id = newDoc.id)
        newDoc.set(stamped).await()
        return stamped
    }

    suspend fun removeFromCart(userId: String, item: CartItem) {
        require(item.id.isNotEmpty()) { "CartItem.id is blank" }
        cartsCol.document(userId).collection("items")
            .document(item.id).delete().await()
    }

    suspend fun clearCart(userId: String) {
        val col   = cartsCol.document(userId).collection("items")
        val batch = db.batch()
        col.get().await().forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    /* ----------  Orders  ---------- */

    suspend fun getOrdersForUser(userId: String): List<Order> =
        db.collection("orders")
            .document(userId)
            .collection("items")
            .get().await()
            .toObjects(Order::class.java)

    suspend fun saveOrder(order: Order) {
        val userDoc = db.collection("orders").document(order.userId)
        val newDoc  = userDoc.collection("items").document()
        val stamped = order.copy(id = newDoc.id)
        newDoc.set(stamped).await()
    }
}
