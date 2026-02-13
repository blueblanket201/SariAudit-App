package com.example.sariaudit.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val costPrice: Double,
    val sellingPrice: Double,
    val quantity: Int,
    val packSize: Int = 1,
    val unit: String = "pcs",
    val category: String = "General",
    val lowStockThreshold: Int = 10,
    val lastRestocked: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val productName: String,
    val quantitySold: Int,
    val sellingPrice: Double,
    val totalAmount: Double,
    val profit: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "utang")
data class Utang(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerName: String,
    val amount: Double, // Current running balance
    val dateCreated: Long = System.currentTimeMillis(),
    val isPaid: Boolean = false,
    val datePaid: Long? = null,
    val notes: String = ""
)

@Entity(tableName = "utang_transactions")
data class UtangTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val utangId: Int,
    val amount: Double,
    val type: String, // "BORROW" or "PAYMENT"
    val date: Long = System.currentTimeMillis()
)