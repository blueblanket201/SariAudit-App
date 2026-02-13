package com.example.sariaudit.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE quantity <= lowStockThreshold")
    fun getLowStockProducts(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Update
    suspend fun updateProduct(product: Product)

    @Query("UPDATE products SET quantity = quantity - :qty WHERE id = :id")
    suspend fun decreaseStock(id: Int, qty: Int)
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Insert
    suspend fun insertSale(sale: Sale)
}

@Dao
interface UtangDao {
    @Query("SELECT * FROM utang ORDER BY dateCreated DESC")
    fun getAllUtang(): Flow<List<Utang>>

    @Insert
    suspend fun insertUtang(utang: Utang) : Long

    @Update
    suspend fun updateUtang(utang: Utang)

    @Delete
    suspend fun deleteUtang(utang: Utang)
}

@Dao
interface UtangTransactionDao {
    @Query("SELECT * FROM utang_transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<UtangTransaction>>

    @Insert
    suspend fun insertTransaction(transaction: UtangTransaction)
}