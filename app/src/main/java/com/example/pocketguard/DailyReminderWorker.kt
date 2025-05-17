package com.example.pocketguard

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pocketguard.models.Expense
import com.example.pocketguard.models.Income
import com.example.pocketguard.utils.NotificationUtils
import com.example.pocketguard.utils.SharedPrefManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DailyReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val sharedPreferences = SharedPrefManager(context)
            val currentUserPhone = sharedPreferences.getCurrentUserPhone()
            
            if (currentUserPhone == null) {
                return Result.failure()
            }

            val gson = Gson()
            
            // Get today's transactions
            val today = java.time.LocalDate.now().toString()
            
            // Get income list
            val incomeList: MutableList<Income> = sharedPreferences.getString("income_$currentUserPhone", null)?.let {
                gson.fromJson(it, object: TypeToken<MutableList<Income>>() {}.type)
            } ?: mutableListOf()
            
            // Get expense list
            val expenseList: MutableList<Expense> = sharedPreferences.getString("expense_$currentUserPhone", null)?.let {
                gson.fromJson(it, object: TypeToken<MutableList<Expense>>() {}.type)
            } ?: mutableListOf()

            // Filter today's transactions
            val todayIncomes = incomeList.filter { it.date == today }
            val todayExpenses = expenseList.filter { it.date == today }

            // Calculate totals
            val totalIncome = todayIncomes.sumOf { it.amount }
            val totalExpense = todayExpenses.sumOf { it.amount }

            // Create notification message
            val message = buildString {
                append("Today's Summary:\n")
                append("Income: $${String.format("%.2f", totalIncome)}\n")
                append("Expenses: $${String.format("%.2f", totalExpense)}\n")
                append("Balance: $${String.format("%.2f", totalIncome - totalExpense)}")
            }

            // Show notification
            NotificationUtils.showNotification(
                context,
                "Daily Transaction Summary",
                message,
                3
            )

            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }
}
