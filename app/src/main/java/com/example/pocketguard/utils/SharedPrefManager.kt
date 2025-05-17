package com.example.pocketguard.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.pocketguard.models.User
import com.example.pocketguard.models.Budget
import com.example.pocketguard.models.Income
import com.example.pocketguard.models.Expense

class SharedPrefManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("PocketGuardPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // OTP
    
    fun storeOTP(phone: String, otp: String) {
        val otpData = OTPData(otp, System.currentTimeMillis(), 0)
        val json = gson.toJson(otpData)
        sharedPreferences.edit().putString("otp_$phone", json).apply()
    }

    fun getOTP(phone: String): OTPData? {
        val json = sharedPreferences.getString("otp_$phone", null)
        return json?.let {
            try {
                gson.fromJson(it, OTPData::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun isOTPValid(phone: String): Boolean {
        val otpData = getOTP(phone) ?: return false
        return (System.currentTimeMillis() - otpData.timestamp) < 300000
    }

    fun incrementOTPAttempts(phone: String) {
        val otpData = getOTP(phone) ?: return
        val updated = otpData.copy(attempts = otpData.attempts + 1)
        sharedPreferences.edit().putString("otp_$phone", gson.toJson(updated)).apply()
    }

    fun getOTPAttempts(phone: String): Int = getOTP(phone)?.attempts ?: 0

    fun clearOTP(phone: String) {
        sharedPreferences.edit().remove("otp_$phone").apply()
    }

    // Authentication related methods
    fun isUserRegisteredByPhone(phone: String): Boolean = getUsers().any { it.phone == phone }

    fun findUserByPhone(phone: String): User? = getUsers().find { it.phone == phone }

    fun createSession(phone: String) = setCurrentUserPhone(phone)

    fun saveUser(user: User) {
        val users = getUsers().toMutableList()
        val index = users.indexOfFirst { it.phone == user.phone }
        if (index != -1) users[index] = user else users.add(user)
        saveUsers(users)
    }

    fun saveUserWithResult(user: User): Boolean {
        return try {
            saveUser(user)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun updatePassword(phone: String, newPassword: String): Boolean {
        val user = findUserByPhone(phone) ?: return false
        return saveUserWithResult(user.copy(password = newPassword))
    }

    fun isSaved(phone: String): Boolean {
        val userExists = isUserRegisteredByPhone(phone)
        val hasIncome = getIncomeList(phone).isNotEmpty()
        val hasExpense = getExpenseList(phone).isNotEmpty()
        val hasBudget = getBudgetForUser(phone) != null
        val hasCurrency = getUserCurrency(phone) != "$"
        val hasNotifications = getNotificationEnabled(phone)
        val hasDarkMode = getDarkModeEnabled(phone)
        return userExists && (hasIncome || hasExpense || hasBudget || hasCurrency || hasNotifications || hasDarkMode)
    }

    fun getUsers(): List<User> {
        val json = sharedPreferences.getString("users", null)
        return json?.let {
            val type = object : TypeToken<List<User>>() {}.type
            gson.fromJson(it, type)
        } ?: emptyList()
    }

    private fun saveUsers(users: List<User>) {
        sharedPreferences.edit().putString("users", gson.toJson(users)).apply()
    }

    // User session methods
    fun getCurrentUserPhone(): String? {
        return sharedPreferences.getString("current_user_phone", null)
    }

    fun setCurrentUserPhone(phone: String) {
        sharedPreferences.edit().putString("current_user_phone", phone).apply()
    }

    // Currency preferences
    fun getUserCurrency(userPhone: String): String {
        // Always return a non-null String by providing a default value
        return sharedPreferences.getString("currency_$userPhone", "$") ?: "$"
    }

    fun setUserCurrency(userPhone: String, currency: String) {
        sharedPreferences.edit().putString("currency_$userPhone", currency).apply()
    }

    // Budget methods
    fun getBudgetForUser(userPhone: String): Double? {
        val budgetJson = getString("budget_$userPhone", null)
        return try {
            gson.fromJson(budgetJson, Budget::class.java)?.limit
        } catch (e: Exception) {
            null
        }
    }

    fun setBudgetForUser(userPhone: String, budget: Double) {
        val budgetObj = Budget(limit = budget)
        val budgetJson = gson.toJson(budgetObj)
        sharedPreferences.edit().putString("budget_$userPhone", budgetJson).apply()
    }

    // Transactions
    fun getIncomeList(userPhone: String): List<Income> {
        val json = sharedPreferences.getString("income_$userPhone", null)
        return json?.let {
            val type = object : TypeToken<List<Income>>() {}.type
            gson.fromJson(it, type)
        } ?: emptyList()
    }

    fun getExpenseList(userPhone: String): List<Expense> {
        val json = getString("expense_$userPhone", "[]")
        return try {
            gson.fromJson(json, object : TypeToken<List<Expense>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveIncomeList(userPhone: String, incomeList: List<Income>) {
        sharedPreferences.edit().putString("income_$userPhone", gson.toJson(incomeList)).apply()
    }

    fun saveExpenseList(userPhone: String, expenseList: List<Expense>) {
        sharedPreferences.edit().putString("expense_$userPhone", gson.toJson(expenseList)).apply()
    }

    // Category methods
    fun getCategories(): List<String> {
        val json = sharedPreferences.getString("categories", null)
        return json?.let {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, type)
        } ?: listOf("Food", "Transport", "Insurance", "Water", "Electricity", "Telecommunication", "Health", "Other")
    }

    fun saveCategories(categories: List<String>) {
        sharedPreferences.edit().putString("categories", gson.toJson(categories)).apply()
    }

    // Settings
    fun getNotificationEnabled(userPhone: String): Boolean =
        sharedPreferences.getBoolean("notifications_$userPhone", true)

    fun setNotificationEnabled(userPhone: String, enabled: Boolean) {
        sharedPreferences.edit().putBoolean("notifications_$userPhone", enabled).apply()
    }

    fun getDarkModeEnabled(userPhone: String): Boolean =
        sharedPreferences.getBoolean("dark_mode_$userPhone", false)

    fun setDarkModeEnabled(userPhone: String, enabled: Boolean) {
        sharedPreferences.edit().putBoolean("dark_mode_$userPhone", enabled).apply()
    }

    // Generic utils
    fun getString(key: String, defaultValue: String?): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue ?: ""
    }

    fun putString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int): Int = sharedPreferences.getInt(key, defaultValue)

    fun putInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    fun logout() {
        sharedPreferences.edit().apply {
            remove("current_user_phone")
            // Clear all user-specific data
            val currentUserPhone = getCurrentUserPhone()
            if (currentUserPhone != null) {
                remove("income_$currentUserPhone")
                remove("expense_$currentUserPhone")
                remove("budget_$currentUserPhone")
                remove("currency_$currentUserPhone")
                remove("notifications_enabled_$currentUserPhone")
                remove("dark_mode_enabled_$currentUserPhone")
                remove("last_reminder_day_$currentUserPhone")
                remove("last_budget_reminder_month_$currentUserPhone")
            }
            apply()
        }
    }

    fun saveUserProfile(user: User) {
        val users = getUsers().toMutableList()
        val index = users.indexOfFirst { it.phone == user.phone }
        if (index != -1) {
            users[index] = user
        }
        saveUsers(users)
        setUserCurrency(user.phone, user.currency)
    }
}

data class OTPData(
    val otp: String,
    val timestamp: Long,
    val attempts: Int
)
