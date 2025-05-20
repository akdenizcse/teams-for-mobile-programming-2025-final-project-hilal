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

    /** Reload past orders */
    fun loadOrders() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            _orders.value = repo.getOrdersForUser(uid)
        }
    }

    /**
     * Called by PaymentActivity.  Now accepts an address.
     */
    fun createOrder(
        cardNumber: String,
        expiry: String,
        cvv: String,
        address: String,
        onComplete: (Boolean) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return onComplete(false)

        // simple client‐side check
        if (cardNumber.length < 12 || cvv.length < 3) {
            onComplete(false); return
        }

        viewModelScope.launch {
            // 1) get current shopping‐list
            val shopItems = repo.getShoppingList(uid)
            if (shopItems.isEmpty()) {
                onComplete(false); return@launch
            }

            // 2) map to CartItem
            val cartItems = shopItems.map {
                CartItem(
                    id       = "",
                    recipeId = it.recipeId?.toIntOrNull() ?: 0,
                    title    = it.name,
                    price    = it.price,
                    image    = ""
                )
            }

            // 3) build & save order (address included)
            val order = Order(
                userId  = uid,
                items   = cartItems,
                address = address   // ← newly stored
            )
            repo.saveOrder(order)

            // 4) clear shopping list & reload orders
            repo.removeShoppingItems(uid, shopItems)
            loadOrders()
            onComplete(true)
        }
    }
}
