package com.example.sariaudit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sariaudit.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class UserRole { ADMIN, EMPLOYEE, NONE }

data class CartItem(val product: Product, val quantity: Int) {
    val total: Double get() = product.sellingPrice * quantity
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)

    private val _userRole = MutableStateFlow(UserRole.NONE)
    val userRole = _userRole.asStateFlow()

    // Data Flows
    val allProducts = db.productDao().getAllProducts().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val lowStockProducts = db.productDao().getLowStockProducts().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val allSales = db.saleDao().getAllSales().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val allUtang = db.utangDao().getAllUtang().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val allUtangTransactions = db.utangTransactionDao().getAllTransactions().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Cart State
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart = _cart.asStateFlow()
    val cartTotal = _cart.map { items -> items.sumOf { it.total } }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // --- UTANG MANAGEMENT ---
    fun modifyUtangBalance(utang: Utang, amount: Double, isPayment: Boolean) = viewModelScope.launch {
        val newAmount = if (isPayment) utang.amount - amount else utang.amount + amount

        val updatedUtang = utang.copy(
            amount = newAmount,
            isPaid = newAmount <= 0,
            datePaid = if (newAmount <= 0) System.currentTimeMillis() else null
        )
        db.utangDao().updateUtang(updatedUtang)

        val transaction = UtangTransaction(
            utangId = utang.id,
            amount = amount,
            type = if (isPayment) "PAYMENT" else "BORROW",
            date = System.currentTimeMillis()
        )
        db.utangTransactionDao().insertTransaction(transaction)
    }

    fun addUtang(u: Utang) = viewModelScope.launch {
        val id = db.utangDao().insertUtang(u)
        db.utangTransactionDao().insertTransaction(UtangTransaction(
            utangId = id.toInt(),
            amount = u.amount,
            type = "BORROW",
            date = u.dateCreated
        ))
    }

    // --- CART ACTIONS ---
    fun addToCart(product: Product) {
        val currentList = _cart.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.product.id == product.id }
        if (existingIndex != -1) {
            val existing = currentList[existingIndex]
            if (existing.quantity < product.quantity) {
                currentList[existingIndex] = existing.copy(quantity = existing.quantity + 1)
            }
        } else {
            currentList.add(CartItem(product, 1))
        }
        _cart.value = currentList
    }

    fun updateCartQuantity(product: Product, newQty: Int) {
        if (newQty <= 0) { removeFromCart(product); return }
        if (newQty > product.quantity) return
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            currentList[index] = currentList[index].copy(quantity = newQty)
            _cart.value = currentList
        }
    }

    fun removeFromCart(product: Product) { _cart.value = _cart.value.filter { it.product.id != product.id } }

    // NEW FUNCTION TO CLEAR CART
    fun clearCart() { _cart.value = emptyList() }

    fun checkoutCart() = viewModelScope.launch {
        val currentCart = _cart.value
        if (currentCart.isEmpty()) return@launch
        currentCart.forEach { item ->
            val sale = Sale(
                productId = item.product.id,
                productName = item.product.name,
                quantitySold = item.quantity,
                sellingPrice = item.product.sellingPrice,
                totalAmount = item.total,
                profit = (item.product.sellingPrice - item.product.costPrice) * item.quantity
            )
            db.saleDao().insertSale(sale)
            db.productDao().decreaseStock(item.product.id, item.quantity)
        }
        _cart.value = emptyList()
    }

    init { populateDemoData() }

    private fun populateDemoData() = viewModelScope.launch {
        if (db.productDao().getAllProducts().first().isEmpty()) {
            val demoProducts = listOf(
                Product(name = "Coke 1L", costPrice = 35.0, sellingPrice = 45.0, quantity = 24, lowStockThreshold = 10),
                Product(name = "Lucky Me Pancit Canton", costPrice = 10.0, sellingPrice = 15.0, quantity = 5, lowStockThreshold = 10),
                Product(name = "Kopiko Coffee", costPrice = 5.0, sellingPrice = 8.0, quantity = 48, lowStockThreshold = 20),
                Product(name = "Surf 10g", costPrice = 4.0, sellingPrice = 6.0, quantity = 3, lowStockThreshold = 10),
                Product(name = "Century Tuna", costPrice = 22.0, sellingPrice = 28.0, quantity = 18),
                Product(name = "Rice 1kg", costPrice = 45.0, sellingPrice = 52.0, quantity = 45),
                Product(name = "Eggs (pc)", costPrice = 5.0, sellingPrice = 7.0, quantity = 60),
                Product(name = "Bread", costPrice = 28.0, sellingPrice = 35.0, quantity = 12),
                Product(name = "Nescafe", costPrice = 3.0, sellingPrice = 5.0, quantity = 30),
                Product(name = "Sprite 1L", costPrice = 35.0, sellingPrice = 45.0, quantity = 10),
                Product(name = "Detergent Bar", costPrice = 25.0, sellingPrice = 35.0, quantity = 8, lowStockThreshold = 10),
                Product(name = "Shampoo Sachet", costPrice = 4.0, sellingPrice = 7.0, quantity = 25)
            )
            demoProducts.forEach { db.productDao().insertProduct(it) }

            val demoUtang = listOf(
                Utang(customerName = "Pedro Garcia", amount = 890.00, notes = "Groceries for the week"),
                Utang(customerName = "Juan Dela Cruz", amount = 350.00, notes = "Cigarettes and drinks"),
                Utang(customerName = "Maria Santos", amount = 125.50, notes = "Rice and eggs"),
                Utang(customerName = "Ana Reyes", amount = 45.00, notes = "Snacks")
            )
            demoUtang.forEach { u ->
                val id = db.utangDao().insertUtang(u)
                db.utangTransactionDao().insertTransaction(UtangTransaction(utangId = id.toInt(), amount = u.amount, type = "BORROW", date = u.dateCreated))
            }

            val products = db.productDao().getAllProducts().first()
            val currentTime = System.currentTimeMillis()
            val dayInMillis = 24 * 60 * 60 * 1000L
            for (i in 0..6) {
                val date = currentTime - (i * dayInMillis)
                repeat(Random.nextInt(3, 8)) {
                    val randomProduct = products.random()
                    val qty = Random.nextInt(1, 5)
                    val sale = Sale(
                        productId = randomProduct.id,
                        productName = randomProduct.name,
                        quantitySold = qty,
                        sellingPrice = randomProduct.sellingPrice,
                        totalAmount = randomProduct.sellingPrice * qty,
                        profit = (randomProduct.sellingPrice - randomProduct.costPrice) * qty,
                        timestamp = date
                    )
                    db.saleDao().insertSale(sale)
                }
            }
        }
    }

    fun login(u: String, p: String): Boolean {
        return when {
            u == "admin" && p == "admin" -> { _userRole.value = UserRole.ADMIN; true }
            u == "admin2" && p == "admin" -> { _userRole.value = UserRole.EMPLOYEE; true }
            else -> false
        }
    }
    fun logout() { _userRole.value = UserRole.NONE }
    fun addProduct(p: Product) = viewModelScope.launch { db.productDao().insertProduct(p) }
    fun deleteProduct(p: Product) = viewModelScope.launch { db.productDao().deleteProduct(p) }
    fun updateProduct(p: Product) = viewModelScope.launch { db.productDao().updateProduct(p) }
}