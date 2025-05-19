// app/src/main/java/com/example/recipes/viewmodel/OrdersViewModel.kt
package com.example.recipes.viewmodel

import androidx.lifecycle.*
import com.example.recipes.data.model.CartItem
import com.example.recipes.data.model.Order
import com.example.recipes.data.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class OrdersViewModel(
    private val repo: FirebaseRepository = FirebaseRepository()
) : ViewModel() {

    private val _orders = MutableLiveData<List<Order>>(emptyList())
    val orders: LiveData<List<Order>> = _orders

    /** Reload past orders for current user */
    fun loadOrders() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            _orders.value = repo.getOrdersForUser(uid)
        }
    }

    /**
     * Called by PaymentActivity after card-validation.
     * Collects *shopping-list* items, converts them to CartItems,
     * saves the order, then clears the shopping list.
     */
    fun createOrder(
        cardNumber: String,
        expiry: String,
        cvv: String,
        onComplete: (Boolean) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            onComplete(false); return
        }

        // very simple validation
        val ok = cardNumber.length >= 12 && cvv.length >= 3
        viewModelScope.launch {
            if (!ok) {
                onComplete(false); return@launch
            }

            /* ▼ 1) pull items from SHOPPING LIST, not cart */
            val shopItems = repo.getShoppingList(uid)
            if (shopItems.isEmpty()) { onComplete(false); return@launch }

            /* ▼ 2) convert them to CartItem for storage in Order */
            val cartItems = shopItems.map {
                CartItem(
                    id       = "",                           // not needed inside order
                    recipeId = it.recipeId?.toIntOrNull() ?: 0,
                    title    = it.name,
                    price    = it.price,
                    image    = ""
                )
            }

            /* ▼ 3) build & save order (total auto-computed) */
            val order = Order(userId = uid, items = cartItems)
            repo.saveOrder(order)

            /* ▼ 4) clear shopping list, reload orders */
            repo.removeShoppingItems(uid, shopItems)
            loadOrders()
            onComplete(true)
        }
    }
}
