package com.example.recipes.data.model

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * A single user review (rating + comment) for a recipe.
 * `userName` is stored so we can show human-readable names
 * without having to look-up the UID each time we display reviews.
 */
@Parcelize
@IgnoreExtraProperties
data class Review(

    @SerializedName("userId")
    val userId: String = "",

    @SerializedName("userName")          //display name / email
    val userName: String = "",

    @SerializedName("recipeId")
    val recipeId: Int = 0,

    @SerializedName("stars")
    val stars: Int = 0,                  // 1-5

    @SerializedName("comment")
    val comment: String = ""

) : Parcelable
