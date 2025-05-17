package com.example.pocketguard

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.gson.Gson
import com.example.pocketguard.utils.SharedPrefManager
import com.example.pocketguard.utils.NotificationUtils
import com.example.pocketguard.models.Income
import com.example.pocketguard.models.Expense
import com.example.pocketguard.models.Budget
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class IncomeExpenseGraphFragment : Fragment() {
    private val TAG = "IncomeExpenseGraphFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_income_expense_graph, container, false)
        val limitBudget = view.findViewById<EditText>(R.id.budgetLimitInput)
        val setBudgetBtn = view.findViewById<Button>(R.id.setBudgetButton)
        val currencyText = view.findViewById<TextView>(R.id.currencySymbolText)

        val gson = Gson()
        val sharedPrefManager = SharedPrefManager(requireContext())
        
        // Get current user phone and handle null case
        val currentUserPhone = sharedPrefManager.getCurrentUserPhone() ?: run {
            Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
            return view
        }

        // Get currency (now guaranteed to be non-null from SharedPrefManager)
        val currency = sharedPrefManager.getUserCurrency(currentUserPhone)
        currencyText.text = currency

        // Load and display current budget
        val currentBudget = sharedPrefManager.getBudgetForUser(currentUserPhone)
        if (currentBudget != null) {
            val formatter = DecimalFormat("#,##0.00")
            limitBudget.setText(formatter.format(currentBudget))
        }

        // Load income and expense data
        val incomeList = sharedPrefManager.getIncomeList(currentUserPhone)
        val expenseList = sharedPrefManager.getExpenseList(currentUserPhone)

        // Check and show budget reminder
        checkAndShowBudgetReminder(sharedPrefManager, currentUserPhone, currency)

        setBudgetBtn.setOnClickListener {
            val budget = limitBudget.text.toString()
            if (budget.isNotEmpty()) {
                try {
                    val budgetLimit = budget.toDouble()
                    val key = "budget_$currentUserPhone"
                    val budgetObj = Budget(limit = budgetLimit)
                    val budgetJson = gson.toJson(budgetObj)
                    sharedPrefManager.putString(key, budgetJson)
                    Toast.makeText(requireContext(), "Budget limit set successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: NumberFormatException) {
                    Toast.makeText(requireContext(), "Invalid number format!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Please enter a budget limit", Toast.LENGTH_SHORT).show()
            }
        }

        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        
        val monthFormat = SimpleDateFormat("MM", Locale.getDefault())
        val incomeByMonth = FloatArray(12) // Initialize with zeros
        val expenseByMonth = FloatArray(12) // Initialize with zeros

        // Calculate income by month with explicit type and null safety
        incomeList.forEach { income: Income ->
            val dateStr = income.date ?: return@forEach  // Skip if date is null
            val date = parseFlexibleDate(dateStr)
            date?.let {
                val monthIndex = monthFormat.format(date).toInt() - 1
                incomeByMonth[monthIndex] += income.amount.toFloat()
            }
        }

        // Calculate expense by month with explicit type and null safety
        expenseList.forEach { expense: Expense ->
            val dateStr = expense.date ?: return@forEach  // Skip if date is null
            val date = parseFlexibleDate(dateStr)
            date?.let {
                val monthIndex = monthFormat.format(date).toInt() - 1
                expenseByMonth[monthIndex] += expense.amount.toFloat()
            }
        }

        val barChart = view.findViewById<BarChart>(R.id.barChart)
        val spinnerStart = view.findViewById<Spinner>(R.id.spinnerStart)
        val spinnerEnd = view.findViewById<Spinner>(R.id.spinnerEnd)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, months)
        spinnerStart.adapter = adapter
        spinnerEnd.adapter = adapter

        fun updateChart(startIndex: Int, endIndex: Int) {
            val selectedIncome = incomeByMonth.slice(startIndex..endIndex)
            val selectedExpense = expenseByMonth.slice(startIndex..endIndex)
            val selectedMonths = months.slice(startIndex..endIndex)

            val entriesIncome = ArrayList<BarEntry>()
            val entriesExpense = ArrayList<BarEntry>()

            for (i in selectedIncome.indices) {
                entriesIncome.add(BarEntry(i.toFloat(), selectedIncome[i].toFloat()))
                entriesExpense.add(BarEntry(i.toFloat(), selectedExpense[i].toFloat()))
            }

            val incomeSet = BarDataSet(entriesIncome, "Income").apply { color = Color.GREEN }
            val expenseSet = BarDataSet(entriesExpense, "Expense").apply { color = Color.RED }

            incomeSet.setDrawValues(false)
            expenseSet.setDrawValues(true)

            val formatter = object : ValueFormatter() {
                override fun getBarLabel(barEntry: BarEntry?): String {
                    val index = entriesExpense.indexOf(barEntry)
                    return if (index != -1 && selectedIncome.getOrNull(index) != null && selectedIncome[index] != 0f) {
                        val expense = barEntry?.y ?: 0f
                        val income = selectedIncome[index]
                        val percentage = (expense / income) * 100f  // Use 100f instead of 100
                        "${percentage.toInt()}%"
                    } else ""
                }
            }

            expenseSet.valueFormatter = formatter
            expenseSet.valueTextColor = Color.WHITE
            expenseSet.valueTextSize = 12f

            val barData = BarData(incomeSet, expenseSet).apply {
                barWidth = 0.35f
            }

            val groupSpace = 0.2f
            val barSpace = 0.05f

            barChart.data = barData
            barChart.description.isEnabled = false

            barChart.xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(selectedMonths)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setCenterAxisLabels(true)
                axisMinimum = 0f
                axisMaximum = barData.getGroupWidth(groupSpace, barSpace) * selectedMonths.size
                labelCount = selectedMonths.size
                textColor = Color.WHITE
            }

            barChart.axisLeft.apply {
                axisMinimum = 0f
                textColor = Color.WHITE
            }

            barChart.axisRight.isEnabled = false
            barChart.legend.textColor = Color.WHITE

            barChart.groupBars(0f, groupSpace, barSpace)
            barChart.invalidate()
        }

        val onSpinnerChange = {
            val start = spinnerStart.selectedItemPosition
            val end = spinnerEnd.selectedItemPosition
            if (start <= end) {
                updateChart(start, end)
            } else {
                Toast.makeText(requireContext(), "Start month must be before end month!", Toast.LENGTH_SHORT).show()
            }
        }

        spinnerStart.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) = onSpinnerChange()
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerEnd.onItemSelectedListener = spinnerStart.onItemSelectedListener

        spinnerStart.setSelection(0)
        spinnerEnd.setSelection(11)

        updateChart(0, 11)

        return view
    }

    private fun parseFlexibleDate(dateStr: String): Date? {
        val formats = listOf("yyyy-MM-dd", "d/M/yyyy", "dd/MM/yyyy", "d/M/yy", "dd/M/yyyy")
        for (format in formats) {
            try {
                return SimpleDateFormat(format, Locale.getDefault()).apply {
                    isLenient = false
                }.parse(dateStr)
            } catch (_: Exception) {}
        }
        Log.e(TAG, "Unrecognized date format: $dateStr")
        return null
    }

    private fun checkAndShowBudgetReminder(
        sharedPrefManager: SharedPrefManager,
        currentUserPhone: String,
        currency: String  // Now expects non-null String
    ) {
        val budgetLimit = sharedPrefManager.getBudgetForUser(currentUserPhone)
        if (budgetLimit == null) {
            NotificationUtils.showNotification(
                context = requireContext(),
                title = "Set Your Budget",
                message = "📊 Don't forget to set your monthly budget limit to track your expenses effectively!",
                id = 109
            )
        } else {
            showBudgetSummaryNotification(sharedPrefManager, currentUserPhone, currency, budgetLimit)
        }
    }

    private fun showBudgetSummaryNotification(
        sharedPrefManager: SharedPrefManager,
        currentUserPhone: String,
        currency: String,
        budgetLimit: Double
    ) {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)

        val expenses = sharedPrefManager.getExpenseList(currentUserPhone)
        val thisMonthExpenses = expenses.filter { expense: Expense ->
            val dateStr = expense.date ?: return@filter false
            val parts = dateStr.split("/")
            if (parts.size == 3) {
                val month = parts[1].toIntOrNull()
                val year = parts[2].toIntOrNull()
                month == currentMonth && year == currentYear
            } else false
        }

        val totalExpenses = thisMonthExpenses.sumOf { it.amount }
        val remaining = budgetLimit - totalExpenses
        val budgetPercentage = (totalExpenses / budgetLimit) * 100

    }
}























