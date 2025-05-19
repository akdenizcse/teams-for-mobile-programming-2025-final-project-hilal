// app/src/main/java/com/example/recipes/data/model/Comment.kt
package com.example.recipes.data.model

data class Comment(
    val id: String          = "",
    val recipeId: Int       = 0,
    val userId: String      = "",
    val userName: String    = "",
    val text: String        = "",
    val timestamp: Long     = 0L,
    val parentId: String?   = null
)
