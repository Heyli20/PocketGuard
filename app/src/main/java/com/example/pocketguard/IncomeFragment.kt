package com.example.pocketguard

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketguard.models.Income
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.pocketguard.utils.SharedPrefManager

class IncomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter<Income>
    private lateinit var incomeList: List<Income>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_income, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewIncome)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val sharedPrefManager = SharedPrefManager(requireContext())
        val phone = sharedPrefManager.getCurrentUserPhone()
        val currency = sharedPrefManager.getUserCurrency("$phone")


        // Initialize adapter with an empty list initially
        adapter = TransactionAdapter(requireContext(), emptyList(),currency) { transaction ->
            showEditBottomSheet(transaction)
        }
        recyclerView.adapter = adapter

        return view
    }

    override fun onResume() {
        super.onResume()
        loadIncomeData()
    }

    private fun loadIncomeData() {
        val sharedPrefManager = SharedPrefManager(requireContext())
        val gson = Gson()
        val phone = sharedPrefManager.getCurrentUserPhone()
        val key = "income_$phone"
        val json = sharedPrefManager.getString(key, null)
        val type = object : TypeToken<List<Income>>() {}.type

        // Safe JSON deserialization with a fallback to an empty list in case of error
        val allTransactions: List<Income> = if (json != null) {
            try {
                gson.fromJson(json, type)
            } catch (e: Exception) {
                Log.e("IncomeFragment", "Error loading income data", e)
                emptyList()
            }
        } else {
            emptyList()
        }

        incomeList = allTransactions
        adapter.updateList(incomeList)
    }

    private fun showEditBottomSheet(transactionToEdit: Income) {
        val sharedPrefManager = SharedPrefManager(requireContext())

        val sheet = TransactionEditBottomSheet(transactionToEdit) { updatedTransaction ->
            Log.d("IncomeFragment", "Updated: $updatedTransaction")

            val mutableList = incomeList.toMutableList()
            val index = mutableList.indexOfFirst {
                it.title == transactionToEdit.title &&
                        it.amount == transactionToEdit.amount &&
                        it.date == transactionToEdit.date
            }

            if (index != -1) {
                mutableList[index] = updatedTransaction
                incomeList = mutableList
                adapter.updateList(incomeList)

                // Save updated list to SharedPreferences
                val gson = Gson()
                val phone = sharedPrefManager.getCurrentUserPhone()
                val key = "income_$phone"
                val updatedJson = gson.toJson(incomeList)
                sharedPrefManager.putString(key, updatedJson)
            }
        }
        sheet.show(parentFragmentManager, "editSheet")
    }

    fun showFilterDialog() {
        // For now, you just show the "Income" category.
        val categories = arrayOf("Income")  // Add more categories as necessary

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Filter by Category")
        builder.setItems(categories) { _, which ->
            val selectedCategory = categories[which]
            filterIncome(selectedCategory)
        }
        builder.show()
    }

    private fun filterIncome(category: String) {
        // Right now, just update the list without filtering
        // Add logic to filter the list based on category in the future
        adapter.updateList(incomeList)
    }
}
