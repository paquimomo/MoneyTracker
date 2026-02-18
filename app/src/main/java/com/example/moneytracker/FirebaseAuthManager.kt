package com.example.moneytracker

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "FirebaseAuthManager"
        private const val USERS_COLLECTION = "users"
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    suspend fun registerUser(user: User): Pair<Boolean, String> {
        return try {
            if (isUsernameExists(user.username)) {
                return Pair(false, "El nombre de usuario ya existe")
            }

            val authResult = auth.createUserWithEmailAndPassword(user.email, user.password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                val newUser = user.copy(
                    id = firebaseUser.uid,
                    password = ""
                )

                firestore.collection(USERS_COLLECTION)
                    .document(firebaseUser.uid)
                    .set(newUser.toMap())
                    .await()
                createDefaultCategories(firebaseUser.uid)

                Pair(true, "Usuario registrado exitosamente")
            } else {
                Pair(false, "Error al crear usuario")
            }
        } catch (e: Exception) {
            Pair(false, e.message ?: "Error desconocido al registrar")
        }
    }

    suspend fun loginUser(email: String, password: String): Pair<User?, String> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                val document = firestore.collection(USERS_COLLECTION)
                    .document(firebaseUser.uid)
                    .get()
                    .await()

                if (document.exists()) {
                    val user = User.fromMap(document.data ?: emptyMap())
                    Pair(user, "Login exitoso")
                } else {
                    Pair(null, "Usuario no encontrado en la base de datos")
                }
            } else {
                Pair(null, "Error de autenticación")
            }
        } catch (e: Exception) {
            Pair(null, e.message ?: "Error al iniciar sesión")
        }
    }

    fun logout() {
        auth.signOut()
    }

    private suspend fun isUsernameExists(username: String): Boolean {
        return try {
            val query = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("username", username)
                .get()
                .await()

            !query.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    suspend fun isEmailExists(email: String): Boolean {
        return try {
            val methods = auth.fetchSignInMethodsForEmail(email).await()
            methods.signInMethods?.isNotEmpty() ?: false
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun createDefaultCategories(userId: String) {
        try {
            val defaultCategories = Category.getDefaultCategories(userId)
            val batch = firestore.batch()

            defaultCategories.forEach { category ->
                val docRef = firestore.collection("categories").document(category.id)
                batch.set(docRef, category.toMap())
            }

            batch.commit().await()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error creando categorías por defecto", e)
        }
    }

    suspend fun getCurrentUserData(): User? {
        return try {
            val userId = getCurrentUserId() ?: return null

            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                User.fromMap(document.data ?: emptyMap())
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseAuthManager", "Error obteniendo usuario", e)
            null
        }
    }
}