package com.example.sariaudit.data

// --- STORE MODELS ---

data class Store(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val accessCode: String = "", // 6-digit code
    val members: Map<String, String> = emptyMap() // Map<UserId, Role>
)

data class StoreMember(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "ASSISTANT" // "OWNER", "ADMIN", "ASSISTANT"
)

// --- BUSINESS MODELS (Adapted for Firebase) ---

data class Product(
    val id: String = "",
    val name: String = "",
    val costPrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val quantity: Int = 0,
    val packSize: Int = 1,
    val unit: String = "pcs",
    val category: String = "General",
    val lowStockThreshold: Int = 10,
    val lastRestocked: Long = System.currentTimeMillis()
)

data class Sale(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val quantitySold: Int = 0,
    val sellingPrice: Double = 0.0,
    val totalAmount: Double = 0.0,
    val profit: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val cashierName: String = "" // Useful to track who sold it
)

data class Utang(
    val id: String = "",
    val customerName: String = "",
    val amount: Double = 0.0,
    val dateCreated: Long = System.currentTimeMillis(),
    val isPaid: Boolean = false,
    val datePaid: Long? = null,
    val notes: String = ""
)

data class UtangTransaction(
    val id: String = "",
    val utangId: String = "",
    val notes: String = "",
    val amount: Double = 0.0,
    val type: String = "", // "BORROW" or "PAYMENT"
    val date: Long = System.currentTimeMillis(),
    val processedBy: String = ""
)