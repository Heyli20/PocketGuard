package com.example.pocketguard

import android.app.DatePickerDialog
import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import com.example.pocketguard.models.Expense
import com.example.pocketguard.models.Income
import java.util.*

class TransactionEditBottomSheet<T>(
    private val transaction: T,  // Accepts either Income or Expense
    private val onUpdate: (T) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var typeSpinner: Spinner
    private lateinit var titleEditText: EditText
    private lateinit var amountEditText: EditText
    private lateinit var dateTextView: TextView
    private lateinit var categoryLabel: TextView
    private lateinit var categorySpinner: Spinner
    private lateinit var updateButton: Button

    private var selectedDate: String = when (transaction) {
        is Income -> transaction.date
        is Expense -> transaction.date
        else -> ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_edit_transaction, container, false)

        // Initialize views
        typeSpinner = view.findViewById(R.id.etEditSpinnerType)
        titleEditText = view.findViewById(R.id.etEditTitle)
        amountEditText = view.findViewById(R.id.etEditAmount)
        dateTextView = view.findViewById(R.id.tvEditselectedDate)
        categoryLabel = view.findViewById(R.id.tvCategoryLabel)
        categorySpinner = view.findViewById(R.id.etEditSpinnerCategory)
        updateButton = view.findViewById(R.id.btnUpdateTransaction)

        // Type Spinner
        val typeList = listOf("Income", "Expense")
        typeSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, typeList)

        // Category Spinner
        val categoryList = listOf("Food", "Transport", "Insurance", "Water", "Electricity", "Telecommunication", "Health", "Other")
        categorySpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categoryList)

        when (transaction) {
            is Income -> {
                typeSpinner.setSelection(0)
                titleEditText.setText(transaction.title)
                amountEditText.setText(transaction.amount.toString())
                dateTextView.text = transaction.date
                categoryLabel.visibility = View.GONE
                categorySpinner.visibility = View.GONE
                typeSpinner.isEnabled=false
            }
            is Expense -> {
                typeSpinner.setSelection(1)
                titleEditText.setText(transaction.title)
                amountEditText.setText(transaction.amount.toString())
                dateTextView.text = transaction.date
                val pos = categoryList.indexOf(transaction.category)
                if (pos >= 0) categorySpinner.setSelection(pos)
                typeSpinner.isEnabled=false
            }
        }

        // Toggle category visibility based on selected type
        typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val isExpense = typeSpinner.selectedItem == "Expense"
                categoryLabel.visibility = if (isExpense) View.VISIBLE else View.GONE
                categorySpinner.visibility = if (isExpense) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Date Picker
        view.findViewById<Button>(R.id.btnSelectDate).setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    selectedDate = sdf.format(calendar.time)
                    dateTextView.text = selectedDate
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        // Keyboard control
        titleEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                amountEditText.requestFocus()
                true
            } else false
        }

        amountEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus()
                val imm = requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else false
        }

        updateButton.setOnClickListener {
            val updatedTitle = titleEditText.text.toString().trim()
            val amountText = amountEditText.text.toString().trim()
            val updatedType = typeSpinner.selectedItem.toString()

            if (updatedTitle.isEmpty()) {
                Toast.makeText(requireContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (amountText.isEmpty()) {
                Toast.makeText(requireContext(), "Amount cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedAmount = amountText.toDoubleOrNull()
            if (updatedAmount == null || updatedAmount <= 0) {
                Toast.makeText(requireContext(), "Enter a valid positive amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (updatedType == "Income" && transaction is Income) {
                val updatedTransaction = Income(title = updatedTitle, amount = updatedAmount, date = selectedDate)
                onUpdate(updatedTransaction as T)
            } else if (updatedType == "Expense" && transaction is Expense) {
                val selectedCategory = categorySpinner.selectedItem.toString()
                val updatedTransaction = Expense(title = updatedTitle, category = selectedCategory, amount = updatedAmount, date = selectedDate)
                onUpdate(updatedTransaction as T)
            }

            Toast.makeText(requireContext(), "Transaction updated successfully", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        return view
    }
}
