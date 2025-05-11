package com.example.recipes.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.gson.annotations.SerializedName

/**
 * A user’s star-rating for a given recipe.
 */
@IgnoreExtraProperties
data class Review(
    @SerializedName("userId")
    val userId: String = "",

    @SerializedName("recipeId")
    val recipeId: Int = 0,

    @SerializedName("stars")
    val stars: Int = 0 ,

    @SerializedName("comment")
    val comment: String = ""
)
