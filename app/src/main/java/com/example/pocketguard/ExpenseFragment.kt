package com.example.pocketguard

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketguard.models.Expense
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.pocketguard.utils.SharedPrefManager
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar

class ExpenseFragment : Fragment() {
    private val TAG = "ExpenseFragment"
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter<Expense>
    private lateinit var expenseList: List<Expense>

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted")
        } else {
            Log.d(TAG, "Notification permission denied")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_expense, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewExpense)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        checkNotificationPermission()
        loadExpenseData()

        return view
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "Notification permission already granted")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showPermissionRationaleDialog()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Notification Permission Required")
            .setMessage("This app needs notification permission to alert you when you exceed your budget limit.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun loadExpenseData() {
        Log.d(TAG, "loadExpenseData called")
        val sharedPrefManager = SharedPrefManager(requireContext())
        val gson = Gson()

        val currentUserPhone = sharedPrefManager.getCurrentUserPhone() ?: return
        val key = "expense_$currentUserPhone"

        val existingExpenses = sharedPrefManager.getString(key, null)
        val type = object : TypeToken<List<Expense>>() {}.type

        expenseList = if (!existingExpenses.isNullOrEmpty()) {
            gson.fromJson(existingExpenses, type)
        } else {
            emptyList()
        }

        val phone = sharedPrefManager.getCurrentUserPhone()
        val currency = sharedPrefManager.getUserCurrency("$phone")

        adapter = TransactionAdapter(requireContext(), expenseList, currency) { transaction ->
            showEditBottomSheet(transaction)
        }
        recyclerView.adapter = adapter

    }


    fun showFilterDialog() {
        val categories = arrayOf("All", "Food", "Transport", "Health", "Water", "Electricity", "Telecommunication", "Other")

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Filter by Category")
        builder.setItems(categories) { _, which ->
            val selectedCategory = categories[which]
            filterExpenses(selectedCategory)
        }
        builder.show()
    }

    private fun filterExpenses(category: String) {
        val filteredList = if (category == "All") {
            expenseList
        } else {
            expenseList.filter { it.category == category }
        }

        adapter.updateList(filteredList)
    }

    override fun onResume() {
        super.onResume()
        loadExpenseData()
    }

    private fun showEditBottomSheet(transactionToEdit: Expense) {
        val actionSheet = TransactionBottomSheet(
            onEditClicked = {
                val sheet = TransactionEditBottomSheet(transactionToEdit) { updatedTransaction ->
                    val mutableList = expenseList.toMutableList()
                    val index = mutableList.indexOfFirst {
                        it.title == transactionToEdit.title &&
                                it.amount == transactionToEdit.amount &&
                                it.date == transactionToEdit.date
                    }

                    if (index != -1) {
                        mutableList[index] = updatedTransaction
                        expenseList = mutableList
                        adapter.updateList(expenseList)

                        val gson = Gson()
                        val sharedPrefManager = SharedPrefManager(requireContext())
                        val currentUserPhone = sharedPrefManager.getCurrentUserPhone()
                        val key = "expense_$currentUserPhone"
                        val updatedJson = gson.toJson(expenseList)
                        sharedPrefManager.putString(key, updatedJson)
                    }
                }
                sheet.show(parentFragmentManager, "editSheet")
            },

            onDeleteClicked = {
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Expense")
                    .setMessage("Are you sure you want to delete this expense?")
                    .setPositiveButton("Delete") { _, _ ->
                        val mutableList = expenseList.toMutableList()
                        val index = mutableList.indexOfFirst {
                            it.title == transactionToEdit.title &&
                                    it.amount == transactionToEdit.amount &&
                                    it.date == transactionToEdit.date
                        }

                        if (index != -1) {
                            mutableList.removeAt(index)
                            expenseList = mutableList
                            adapter.updateList(expenseList)

                            val gson = Gson()
                            val sharedPrefManager = SharedPrefManager(requireContext())
                            val currentUserPhone = sharedPrefManager.getCurrentUserPhone()
                            val key = "expense_$currentUserPhone"
                            val updatedJson = gson.toJson(expenseList)
                            sharedPrefManager.putString(key, updatedJson)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        actionSheet.show(parentFragmentManager, "actionSheet")
    }
}
