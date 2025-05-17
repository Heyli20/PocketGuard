package com.example.pocketguard

import com.example.pocketguard.models.Expense
import com.example.pocketguard.models.Income
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.pocketguard.utils.SharedPrefManager
import android.util.Log

class TransactionClaculations(private val sharedPrefManager: SharedPrefManager) {

    fun getTotalIncome(phone: String): Double {
        val key = "income_$phone"
        return try {
            val json = sharedPrefManager.getString(key, null)
            if (json.isNullOrEmpty()) return 0.0

            val typeToken = object : TypeToken<List<Income>>() {}.type
            val incomeList: List<Income> = Gson().fromJson(json, typeToken) ?: emptyList()
            incomeList.sumOf { it.amount }
        } catch (e: Exception) {
            Log.e("TransactionCalculations", "Error calculating total income", e)
            0.0
        }
    }

    fun getTotalExpense(phone: String): Double {
        val key = "expense_$phone"
        return try {
            val json = sharedPrefManager.getString(key, null)
            if (json.isNullOrEmpty()) return 0.0

            val typeToken = object : TypeToken<List<Expense>>() {}.type
            val expenseList: List<Expense> = Gson().fromJson(json, typeToken) ?: emptyList()
            expenseList.sumOf { it.amount }
        } catch (e: Exception) {
            Log.e("TransactionCalculations", "Error calculating total expense", e)
            0.0
        }
    }

    fun getBalance(phone: String): Double {
        val totIncome = getTotalIncome(phone)
        val totExpense = getTotalExpense(phone)
        return totIncome - totExpense
    }
}

