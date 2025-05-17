package com.example.pocketguard

import android.app.DatePickerDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.pocketguard.utils.NotificationUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.pocketguard.models.Expense
import com.example.pocketguard.models.Income
import com.example.pocketguard.utils.SharedPrefManager
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

class AddTransaction : AppCompatActivity() {
    private val TAG = "AddTransaction"
    private lateinit var spinnerType: Spinner
    private lateinit var spinnerCategory: Spinner
    private lateinit var tvCategoryLabel: TextView
    private lateinit var etTitle: EditText
    private lateinit var etAmount: EditText
    private lateinit var btnSelectDate: Button
    private lateinit var tvSelectedDate: TextView
    private lateinit var btnSubmit: Button

    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)
        enableEdgeToEdge()

        val btnBack: ImageView = findViewById(R.id.back)
        btnBack.setOnClickListener { finish() }

        // Bind views
        spinnerType = findViewById(R.id.spinnerType)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        tvCategoryLabel = findViewById(R.id.tvCategoryLabel)
        etTitle = findViewById(R.id.etTitle)
        etAmount = findViewById(R.id.etAmount)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        btnSubmit = findViewById(R.id.btnSubmit)

        etTitle.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                etAmount.requestFocus()
                true
            } else false
        }

        etAmount.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else false
        }

        // Spinners
        val types = listOf("Income", "Expense")
        spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)

        val categories = listOf("Food", "Transport", "Insurance", "Water", "Electricity", "Telecommunication", "Health", "Other")
        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapter: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val isExpense = types[position] == "Expense"
                spinnerCategory.visibility = if (isExpense) View.VISIBLE else View.GONE
                tvCategoryLabel.visibility = if (isExpense) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // Date picker
        btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, y, m, d ->
                selectedDate = "$d/${m + 1}/$y"
                tvSelectedDate.text = selectedDate
            }, year, month, day)

            datePicker.show()
        }

        // Submit
        btnSubmit.setOnClickListener {
            val type = spinnerType.selectedItem.toString()
            val title = etTitle.text.toString().trim()
            val amountText = etAmount.text.toString()
            val amount = amountText.toDoubleOrNull()
            val category = if (type == "Expense") spinnerCategory.selectedItem.toString() else ""

            if (title.isEmpty() || amount == null || selectedDate.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sharedPreferences = SharedPrefManager(this)
            val currentUserPhone = sharedPreferences.getCurrentUserPhone()
            val gson = Gson()

            if (currentUserPhone == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(type == "Income"){
                val key = "income_$currentUserPhone"
                val newIncome = Income(title = title, amount = amount, date = selectedDate)
                val type = object: TypeToken<MutableList<Income>>() {}.type
                val existingList: MutableList<Income> = sharedPreferences.getString(key, null)?.let {
                    gson.fromJson(it, type)
                } ?: mutableListOf()

                existingList.add(newIncome)
                sharedPreferences.putString(key, gson.toJson(existingList))

                Toast.makeText(this, "Income Added", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                val key = "expense_$currentUserPhone"
                val newExpense = Expense(title = title, category = category, amount = amount, date = selectedDate)
                val type = object: TypeToken<MutableList<Expense>>() {}.type
                val existingList: MutableList<Expense> = sharedPreferences.getString(key, null)?.let {
                    gson.fromJson(it, type)
                } ?: mutableListOf()

                existingList.add(newExpense)

                // Get income and expense lists
                val incomeList: MutableList<Income> = sharedPreferences.getString("income_$currentUserPhone", null)?.let {
                    gson.fromJson(it, object: TypeToken<MutableList<Income>>() {}.type)
                } ?: mutableListOf()
                
                val expenseList: MutableList<Expense> = sharedPreferences.getString("expense_$currentUserPhone", null)?.let {
                    gson.fromJson(it, object: TypeToken<MutableList<Expense>>() {}.type)
                } ?: mutableListOf()

                val totalIncome = incomeList.sumOf { it.amount }

                // Get current month and year
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH) + 1
                val currentYear = calendar.get(Calendar.YEAR)

                // Filter expenses for current month
                val currentMonthExpenses = expenseList.filter { expense ->
                    val dateParts = expense.date.split("/")
                    if (dateParts.size == 3) {
                        val month = dateParts[1].toIntOrNull()
                        val year = dateParts[2].toIntOrNull()
                        month == currentMonth && year == currentYear
                    } else false
                }

                // Calculate total expense including the new expense if it's for current month
                var totalExpense = currentMonthExpenses.sumOf { it.amount }
                
                // Check if new expense is for current month
                val newExpenseDateParts = selectedDate.split("/")
                if (newExpenseDateParts.size == 3) {
                    val newExpenseMonth = newExpenseDateParts[1].toIntOrNull()
                    val newExpenseYear = newExpenseDateParts[2].toIntOrNull()
                    if (newExpenseMonth == currentMonth && newExpenseYear == currentYear) {
                        totalExpense += amount
                    }
                }

                // Get budget limit from SharedPreferences
                val budgetLimit = sharedPreferences.getBudgetForUser(currentUserPhone) ?: totalIncome * 0.9


                when {
                    totalExpense > budgetLimit -> {
                        Log.d(TAG, "Budget exceeded notification triggered")
                        NotificationUtils.showNotification(
                            this,
                            "Budget Exceeded",
                            "You've exceeded your budget limit this month.",
                            1
                        )
                    }
                    totalExpense >= budgetLimit * 0.9 -> {
                        Log.d(TAG, "Budget warning notification triggered")
                        NotificationUtils.showNotification(
                            this,
                            "Warning",
                            "You're nearing your monthly budget limit.",
                            2
                        )
                    }
                }
                sharedPreferences.putString(key, gson.toJson(existingList))

                Toast.makeText(this, "Expense Added", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}


