// app/src/main/java/com/example/recipes/data/model/Recipe.kt
package com.example.recipes.data.model

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * A Recipe from Spoonacular (or Firestore).
 * All nullable, complex‐type fields are @RawValue so Parcelize will accept them.
 */

@IgnoreExtraProperties
@Parcelize
data class Recipe @JvmOverloads constructor(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("title")
    val title: String = "",

    @SerializedName("image")
    val image: String = "",

    @SerializedName("summary")
    val summary: String? = null,

    @SerializedName("instructions")
    val instructions: String? = null,

    @SerializedName("servings")
    val servings: Int? = null,

    @SerializedName("readyInMinutes")
    val readyInMinutes: Int? = null,

    /** A list of Ingredient objects (Spoonacular) */
    @SerializedName("extendedIngredients")
    val ingredients: @RawValue List<Ingredient>? = null,

    /** Nutrition block (Spoonacular) */
    @SerializedName("nutrition")
    val nutrition: @RawValue Nutrition? = null,

    /** Diet tags like ["vegetarian","gluten free"] */
    @SerializedName("diets")
    val diets: @RawValue List<String>? = null
) : Parcelable
