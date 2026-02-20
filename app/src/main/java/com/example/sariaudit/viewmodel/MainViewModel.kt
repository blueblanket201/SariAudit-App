package com.example.sariaudit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sariaudit.data.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Note: UserRole is now in UserRole.kt to prevent redeclaration errors

data class CartItem(val product: Product, val quantity: Int) {
    val total: Double get() = product.sellingPrice * quantity
}

// Data class for member list
data class MemberDetails(val uid: String, val name: String, val email: String, val role: String)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = Firebase.database.reference

    // --- AUTH & STORE STATE ---
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser = _currentUser.asStateFlow()

    private val _currentStore = MutableStateFlow<Store?>(null)
    val currentStore = _currentStore.asStateFlow()

    private val _userRole = MutableStateFlow(UserRole.NONE)
    val userRole = _userRole.asStateFlow()

    private val _userStores = MutableStateFlow<List<Store>>(emptyList())
    val userStores = _userStores.asStateFlow()

    // Member List State
    private val _currentMembers = MutableStateFlow<List<MemberDetails>>(emptyList())
    val currentMembers = _currentMembers.asStateFlow()

    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg = _errorMsg.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _registrationSuccess = MutableStateFlow(false)
    val registrationSuccess = _registrationSuccess.asStateFlow()

    // --- BUSINESS DATA FLOWS ---
    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    val allProducts = _allProducts.asStateFlow()

    val lowStockProducts = _allProducts.map { list ->
        list.filter { it.quantity <= it.lowStockThreshold }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _allSales = MutableStateFlow<List<Sale>>(emptyList())
    val allSales = _allSales.asStateFlow()

    private val _allUtang = MutableStateFlow<List<Utang>>(emptyList())
    val allUtang = _allUtang.asStateFlow()

    private val _allUtangTransactions = MutableStateFlow<List<UtangTransaction>>(emptyList())
    val allUtangTransactions = _allUtangTransactions.asStateFlow()

    // Cart State
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart = _cart.asStateFlow()
    val cartTotal = _cart.map { items -> items.sumOf { it.total } }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // Listeners
    private var productListener: ValueEventListener? = null
    private var saleListener: ValueEventListener? = null
    private var utangListener: ValueEventListener? = null
    private var transListener: ValueEventListener? = null

    init {
        try { Firebase.database.setPersistenceEnabled(true) } catch (e: Exception) { /* Already enabled */ }

        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            if (user != null) {
                // REMOVED AUTO-SYNC. Now we only fetch stores.
                // Google users are synced manually via finalizeGoogleLogin() in LoginScreen.
                fetchUserStores()
            } else {
                detachListeners()
                _currentStore.value = null
                _userRole.value = UserRole.NONE
            }
        }
    }

    // --- GOOGLE SIGN-IN HANDLER ---
    // Called manually from LoginScreen upon successful Google Sign-In
    fun finalizeGoogleLogin() {
        val user = auth.currentUser ?: return
        val userRef = db.child("users").child(user.uid)

        userRef.get().addOnSuccessListener { snapshot ->
            // Only save if user doesn't exist yet
            if (!snapshot.exists()) {
                val parts = (user.displayName ?: "User").split(" ")
                val firstName = parts.firstOrNull() ?: "User"
                val lastName = if (parts.size > 1) parts.lastOrNull() ?: "" else ""

                val userMap = mapOf(
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "email" to (user.email ?: "No Email")
                )
                userRef.setValue(userMap)
            }
        }
    }

    // --- STORE MANAGEMENT ---

    fun createStore(storeName: String) {
        val user = auth.currentUser ?: return
        if (storeName.isBlank()) { _errorMsg.value = "Store name required"; return }

        _loading.value = true
        val storeId = db.child("stores").push().key ?: return
        val code = (100000..999999).random().toString()

        val newStore = Store(
            id = storeId,
            name = storeName,
            ownerId = user.uid,
            accessCode = code,
            members = mapOf(user.uid to "OWNER")
        )

        val updates = hashMapOf<String, Any>(
            "/stores/$storeId" to newStore,
            "/users/${user.uid}/joinedStores/$storeId" to true
        )

        db.updateChildren(updates).addOnCompleteListener { task ->
            _loading.value = false
            if (task.isSuccessful) {
                selectStore(newStore)
                fetchUserStores()
            } else {
                _errorMsg.value = task.exception?.message
            }
        }
    }

    fun deleteStore(onSuccess: () -> Unit) {
        val store = _currentStore.value ?: return

        if (_userRole.value != UserRole.OWNER) {
            _errorMsg.value = "Only the Owner can delete this store."
            return
        }

        _loading.value = true

        // 1. Clean up references for all members
        val memberIds = store.members.keys
        memberIds.forEach { memberId ->
            db.child("users").child(memberId).child("joinedStores").child(store.id).removeValue()
        }

        // 2. Delete store data
        db.child("stores").child(store.id).removeValue().addOnCompleteListener { task ->
            _loading.value = false
            if (task.isSuccessful) {
                _currentStore.value = null
                fetchUserStores()
                onSuccess()
            } else {
                _errorMsg.value = task.exception?.message
            }
        }
    }

    fun joinStore(accessCode: String) {
        val user = auth.currentUser ?: return
        if (accessCode.length != 6) { _errorMsg.value = "Invalid code"; return }

        _loading.value = true
        db.child("stores").orderByChild("accessCode").equalTo(accessCode)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val storeSnapshot = snapshot.children.first()
                        val storeId = storeSnapshot.key ?: return
                        val storeName = storeSnapshot.child("name").getValue(String::class.java) ?: ""

                        val updates = hashMapOf<String, Any>(
                            "/stores/$storeId/members/${user.uid}" to "ASSISTANT",
                            "/users/${user.uid}/joinedStores/$storeId" to true
                        )

                        db.updateChildren(updates).addOnCompleteListener { task ->
                            _loading.value = false
                            if (task.isSuccessful) {
                                fetchUserStores()
                                val joinedStore = Store(id = storeId, name = storeName, accessCode = accessCode)
                                selectStore(joinedStore)
                            } else {
                                _errorMsg.value = "Failed to join."
                            }
                        }
                    } else {
                        _loading.value = false
                        _errorMsg.value = "Store not found."
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    _loading.value = false
                    _errorMsg.value = error.message
                }
            })
    }

    private fun fetchUserStores() {
        val userId = auth.currentUser?.uid ?: return
        db.child("users").child(userId).child("joinedStores")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userStoresSnapshot: DataSnapshot) {
                    val storeIds = userStoresSnapshot.children.mapNotNull { it.key }
                    if (storeIds.isEmpty()) {
                        _userStores.value = emptyList()
                        return
                    }

                    val stores = mutableListOf<Store>()
                    var fetchedCount = 0
                    storeIds.forEach { sid ->
                        db.child("stores").child(sid).get().addOnSuccessListener { shot ->
                            val s = shot.getValue(Store::class.java)
                            if (s != null) stores.add(s)
                            fetchedCount++
                            if (fetchedCount == storeIds.size) {
                                _userStores.value = stores
                                if (_currentStore.value == null && stores.isNotEmpty()) {
                                    selectStore(stores.first())
                                }
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun selectStore(store: Store) {
        val userId = auth.currentUser?.uid ?: return
        detachListeners()
        _currentStore.value = store

        val roleStr = store.members[userId] ?: "NONE"
        _userRole.value = when (roleStr) {
            "OWNER" -> UserRole.OWNER
            "ADMIN" -> UserRole.ADMIN
            "ASSISTANT" -> UserRole.ASSISTANT
            else -> UserRole.NONE
        }

        attachListeners(store.id)
    }

    // --- TEAM MANAGEMENT ---

    fun fetchStoreMembers() {
        val store = _currentStore.value ?: return
        val memberIds = store.members.keys
        val fetchedMembers = mutableListOf<MemberDetails>()
        var completed = 0

        if (memberIds.isEmpty()) {
            _currentMembers.value = emptyList()
            return
        }

        memberIds.forEach { uid ->
            db.child("users").child(uid).get().addOnSuccessListener { snapshot ->
                val firstName = snapshot.child("firstName").getValue(String::class.java) ?: "User"
                val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                val email = snapshot.child("email").getValue(String::class.java) ?: "No Email"
                val role = store.members[uid] ?: "ASSISTANT"

                fetchedMembers.add(MemberDetails(uid, "$firstName $lastName", email, role))
                completed++

                if (completed == memberIds.size) {
                    _currentMembers.value = fetchedMembers
                }
            }.addOnFailureListener {
                fetchedMembers.add(MemberDetails(uid, "Unknown User", "N/A", store.members[uid] ?: "ASSISTANT"))
                completed++
                if (completed == memberIds.size) _currentMembers.value = fetchedMembers
            }
        }
    }

    fun updateMemberRole(uid: String, newRole: String) {
        val storeId = _currentStore.value?.id ?: return
        db.child("stores").child(storeId).child("members").child(uid).setValue(newRole)
            .addOnSuccessListener {
                db.child("stores").child(storeId).get().addOnSuccessListener { snapshot ->
                    val updatedStore = snapshot.getValue(Store::class.java)
                    if (updatedStore != null) {
                        _currentStore.value = updatedStore
                        fetchStoreMembers()
                    }
                }
            }
    }

    fun removeMember(uid: String) {
        val storeId = _currentStore.value?.id ?: return
        db.child("stores").child(storeId).child("members").child(uid).removeValue()
        db.child("users").child(uid).child("joinedStores").child(storeId).removeValue()
            .addOnSuccessListener {
                db.child("stores").child(storeId).get().addOnSuccessListener { snapshot ->
                    val updatedStore = snapshot.getValue(Store::class.java)
                    if (updatedStore != null) {
                        _currentStore.value = updatedStore
                        fetchStoreMembers()
                    }
                }
            }
    }

    // --- REALTIME DATA LISTENERS ---

    private fun attachListeners(storeId: String) {
        val storeRef = db.child("stores").child(storeId)

        productListener = storeRef.child("products").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Product::class.java) }
                _allProducts.value = list.sortedBy { it.name }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        saleListener = storeRef.child("sales").orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Sale::class.java) }
                _allSales.value = list.reversed()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        utangListener = storeRef.child("utang").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Utang::class.java) }
                _allUtang.value = list.sortedByDescending { it.dateCreated }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        transListener = storeRef.child("utang_transactions").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(UtangTransaction::class.java) }
                _allUtangTransactions.value = list.sortedByDescending { it.date }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun detachListeners() {
        productListener?.let { db.removeEventListener(it) }
        saleListener?.let { db.removeEventListener(it) }
        utangListener?.let { db.removeEventListener(it) }
        transListener?.let { db.removeEventListener(it) }
    }

    // --- AUTH ACTIONS ---

    fun loginWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) { _errorMsg.value = "Enter email and password"; return }
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { _errorMsg.value = null }
            .addOnFailureListener { e -> _errorMsg.value = e.message }
    }

    fun registerWithEmail(email: String, pass: String, firstName: String, lastName: String) {
        if (email.isBlank() || pass.isBlank()) { _errorMsg.value = "Missing fields"; return }

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val user = result.user ?: return@addOnSuccessListener
                val fullName = "$firstName $lastName"

                val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(fullName).build()
                user.updateProfile(profileUpdates)

                val userMap = mapOf(
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "email" to email
                )
                db.child("users").child(user.uid).setValue(userMap)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            auth.signOut()
                            _registrationSuccess.value = true
                            _errorMsg.value = null
                        } else {
                            _errorMsg.value = "Registered, but failed to save details."
                        }
                    }
            }
            .addOnFailureListener { e -> _errorMsg.value = e.message }
    }

    fun resetRegistrationState() { _registrationSuccess.value = false }
    fun clearError() { _errorMsg.value = null }
    fun logout() {
        auth.signOut()
        _currentStore.value = null
    }

    // --- BUSINESS ACTIONS ---

    // 🔴 UPDATED: Added the notes parameter with a default empty string
    fun modifyUtangBalance(utang: Utang, amount: Double, isPayment: Boolean, notes: String = "") {
        val storeId = _currentStore.value?.id ?: return
        val newAmount = if (isPayment) utang.amount - amount else utang.amount + amount

        val updatedUtang = utang.copy(
            amount = newAmount,
            isPaid = newAmount <= 0,
            datePaid = if (newAmount <= 0) System.currentTimeMillis() else null
        )

        val transId = db.child("stores").child(storeId).child("utang_transactions").push().key ?: return

        // 🔴 UPDATED: Passing notes into UtangTransaction
        val trans = UtangTransaction(
            id = transId,
            utangId = utang.id,
            amount = amount,
            type = if (isPayment) "PAYMENT" else "BORROW",
            processedBy = auth.currentUser?.displayName ?: "Unknown",
            notes = notes
        )

        val updates = hashMapOf<String, Any>(
            "stores/$storeId/utang/${utang.id}" to updatedUtang,
            "stores/$storeId/utang_transactions/$transId" to trans
        )
        db.updateChildren(updates)
    }

    fun addUtang(u: Utang) {
        val storeId = _currentStore.value?.id ?: return
        val key = db.child("stores").child(storeId).child("utang").push().key ?: return
        val newUtang = u.copy(id = key)
        db.child("stores").child(storeId).child("utang").child(key).setValue(newUtang)
    }

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
    fun clearCart() { _cart.value = emptyList() }

    fun checkoutCart() {
        val storeId = _currentStore.value?.id ?: return
        val currentCart = _cart.value
        if (currentCart.isEmpty()) return

        val updates = hashMapOf<String, Any>()

        currentCart.forEach { item ->
            val saleId = db.child("stores").child(storeId).child("sales").push().key ?: return@forEach
            val sale = Sale(
                id = saleId,
                productId = item.product.id,
                productName = item.product.name,
                quantitySold = item.quantity,
                sellingPrice = item.product.sellingPrice,
                totalAmount = item.total,
                profit = (item.product.sellingPrice - item.product.costPrice) * item.quantity,
                cashierName = auth.currentUser?.displayName ?: "Unknown"
            )
            updates["stores/$storeId/sales/$saleId"] = sale
            val newStock = item.product.quantity - item.quantity
            updates["stores/$storeId/products/${item.product.id}/quantity"] = newStock
        }

        db.updateChildren(updates).addOnSuccessListener {
            _cart.value = emptyList()
        }
    }

    fun addProduct(p: Product) {
        if (_userRole.value == UserRole.ASSISTANT) return
        val storeId = _currentStore.value?.id ?: return
        val key = db.child("stores").child(storeId).child("products").push().key ?: return
        val newProduct = p.copy(id = key)
        db.child("stores").child(storeId).child("products").child(key).setValue(newProduct)
    }

    fun deleteProduct(p: Product) {
        if (_userRole.value == UserRole.ASSISTANT) return
        val storeId = _currentStore.value?.id ?: return
        db.child("stores").child(storeId).child("products").child(p.id).removeValue()
    }

    fun updateProduct(p: Product) {
        if (_userRole.value == UserRole.ASSISTANT) return
        val storeId = _currentStore.value?.id ?: return
        db.child("stores").child(storeId).child("products").child(p.id).setValue(p)
    }

    fun populateDemoData() {
        if (_userRole.value == UserRole.ASSISTANT) return
        val storeId = _currentStore.value?.id ?: return
        val demoProducts = listOf(
            Product(name = "Coke 1L", costPrice = 35.0, sellingPrice = 45.0, quantity = 24),
            Product(name = "Pancit Canton", costPrice = 10.0, sellingPrice = 15.0, quantity = 50),
            Product(name = "Kopiko Brown", costPrice = 5.0, sellingPrice = 8.0, quantity = 100)
        )
        demoProducts.forEach { addProduct(it) }
    }
}