package com.example.moneytracker

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.*

class FirestoreManager {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "FirestoreManager"
        private const val DEFAULT_CATEGORIES_COLLECTION = "default_categories"
        private const val USER_CATEGORIES_COLLECTION = "user_categories"
        private const val EXPENSES_COLLECTION = "expenses"
    }

    suspend fun getCategoriesByUser(userId: String): List<Category> {
        return try {
            val defaultCategories = getDefaultCategories()

            val userCategories = getUserCustomCategories(userId)

            (defaultCategories + userCategories)
                .sortedBy { it.createdAt.seconds }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error obteniendo categorías", e)
            emptyList()
        }
    }

    private suspend fun getDefaultCategories(): List<Category> {
        return try {
            val snapshot = firestore.collection(DEFAULT_CATEGORIES_COLLECTION)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Category.fromMap(data).copy(
                    id = doc.id,
                    isDefault = true  // Marcar como default
                )
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error obteniendo categorías default", e)
            emptyList()
        }
    }

    private suspend fun getUserCustomCategories(userId: String): List<Category> {
        return try {
            val snapshot = firestore.collection(USER_CATEGORIES_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Category.fromMap(data).copy(
                    id = doc.id,
                    isDefault = false
                )
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error obteniendo categorías del usuario", e)
            emptyList()
        }
    }

    suspend fun insertCategory(category: Category): Pair<Boolean, String> {
        return try {
            if (category.isDefault) {
                return Pair(false, "No se pueden modificar categorías por defecto")
            }

            val docRef = firestore.collection(USER_CATEGORIES_COLLECTION).document()
            val categoryWithId = category.copy(
                id = docRef.id,
                isDefault = false
            )

            docRef.set(categoryWithId.toMap()).await()
            Pair(true, docRef.id)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error insertando categoría", e)
            Pair(false, e.message ?: "Error desconocido")
        }
    }

    suspend fun updateCategory(category: Category): Boolean {
        return try {
            if (category.isDefault) {
                android.util.Log.e(TAG, "Intento de actualizar categoría default")
                return false
            }

            firestore.collection(USER_CATEGORIES_COLLECTION)
                .document(category.id)
                .update(category.toMap())
                .await()

            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error actualizando categoría", e)
            false
        }
    }

    suspend fun deleteCategory(categoryId: String): Boolean {
        return try {
            val category = getCategoryById(categoryId)
            if (category?.isDefault == true) {
                android.util.Log.e(TAG, "Intento de eliminar categoría default")
                return false
            }

            firestore.collection(USER_CATEGORIES_COLLECTION)
                .document(categoryId)
                .delete()
                .await()

            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error eliminando categoría", e)
            false
        }
    }

    suspend fun getCategoryById(categoryId: String): Category? {
        return try {
            var document = firestore.collection(USER_CATEGORIES_COLLECTION)
                .document(categoryId)
                .get()
                .await()

            if (document.exists()) {
                return Category.fromMap(document.data ?: emptyMap()).copy(
                    id = document.id,
                    isDefault = false
                )
            }

            document = firestore.collection(DEFAULT_CATEGORIES_COLLECTION)
                .document(categoryId)
                .get()
                .await()

            if (document.exists()) {
                Category.fromMap(document.data ?: emptyMap()).copy(
                    id = document.id,
                    isDefault = true
                )
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error obteniendo categoría por ID", e)
            null
        }
    }

    suspend fun insertExpense(expense: Expense): Pair<Boolean, String> {
        return try {
            val docRef = firestore.collection(EXPENSES_COLLECTION).document()
            val expenseWithId = expense.copy(id = docRef.id)

            docRef.set(expenseWithId.toMap()).await()
            Pair(true, docRef.id)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error insertando gasto", e)
            Pair(false, e.message ?: "Error desconocido")
        }
    }

    suspend fun getExpensesByUser(userId: String): List<Expense> {
        return try {
            val snapshot = firestore.collection(EXPENSES_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val expenses = snapshot.documents.mapNotNull { doc ->
                Expense.fromMap(doc.data ?: emptyMap())
            }

            expenses.sortedByDescending { it.date + it.time }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error", e)
            emptyList()
        }
    }

    suspend fun getExpenseById(expenseId: String): Expense? {
        return try {
            val document = firestore.collection(EXPENSES_COLLECTION)
                .document(expenseId)
                .get()
                .await()

            if (document.exists()) {
                Expense.fromMap(document.data ?: emptyMap())
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error obteniendo gasto por ID", e)
            null
        }
    }

    suspend fun updateExpense(expense: Expense): Boolean {
        return try {
            firestore.collection(EXPENSES_COLLECTION)
                .document(expense.id)
                .update(expense.toMap())
                .await()

            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error actualizando gasto", e)
            false
        }
    }

    suspend fun deleteExpense(expenseId: String): Boolean {
        return try {
            firestore.collection(EXPENSES_COLLECTION)
                .document(expenseId)
                .delete()
                .await()

            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error eliminando gasto", e)
            false
        }
    }

    suspend fun getExpensesByCategory(userId: String, categoryName: String): List<Expense> {
        return try {
            val snapshot = firestore.collection(EXPENSES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("category", categoryName)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                Expense.fromMap(doc.data ?: emptyMap())
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error obteniendo gastos por categoría", e)
            emptyList()
        }
    }

    suspend fun getCategoryStats(userId: String): Map<String, Double> {
        return try {
            val expenses = getExpensesByUser(userId)

            expenses.groupBy { it.category }
                .mapValues { (_, expenseList) ->
                    expenseList.sumOf { it.amount }
                }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error obteniendo estadísticas", e)
            emptyMap()
        }
    }

    suspend fun getCurrentMonthExpenses(userId: String): List<Expense> {
        return try {
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH) + 1
            val currentYear = calendar.get(Calendar.YEAR)

            val expenses = getExpensesByUser(userId)

            expenses.filter { expense ->
                val parts = expense.date.split("/")
                if (parts.size == 3) {
                    val expenseMonth = parts[1].toIntOrNull()
                    val expenseYear = parts[2].toIntOrNull()
                    expenseMonth == currentMonth && expenseYear == currentYear
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error obteniendo gastos del mes", e)
            emptyList()
        }
    }

    suspend fun getTotalMonthlyExpenses(userId: String): Double {
        return try {
            val monthExpenses = getCurrentMonthExpenses(userId)
            monthExpenses.sumOf { it.amount }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error obteniendo total mensual", e)
            0.0
        }
    }
}