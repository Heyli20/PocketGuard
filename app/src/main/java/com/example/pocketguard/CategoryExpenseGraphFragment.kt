package com.example.pocketguard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.example.pocketguard.utils.SharedPrefManager
import com.example.pocketguard.models.Expense
import java.text.NumberFormat
import java.util.Locale

class CategoryExpenseGraphFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_category_expense_graph, container, false)
        val pieChart = view.findViewById<PieChart>(R.id.pieChart)
        val categoryList = view.findViewById<LinearLayout>(R.id.categoryList)

        val sharedPrefManager = SharedPrefManager(requireContext())
        val phone = sharedPrefManager.getCurrentUserPhone()
        val expenses: List<Expense> = sharedPrefManager.getExpenseList(phone.toString()) ?: emptyList()
        val currency = sharedPrefManager.getUserCurrency(phone.toString())

        // Group by category and sum amounts with explicit types
        val grouped = expenses
            .groupingBy { it.category }
            .fold(0.0) { acc, expense -> acc + expense.amount }

        if (grouped.isEmpty()) {
            val noData = TextView(context).apply {
                text = "No expense data available"
                textSize = 16f
                setTextColor(Color.DKGRAY)
            }
            categoryList.addView(noData)
            pieChart.clear()
            return view
        }

        // Predefined colors for each category
        val categoryColors = mapOf(
            "Food" to Color.parseColor("#FF6F61"),
            "Transport" to Color.parseColor("#6B5B95"),
            "Utilities" to Color.parseColor("#88B04B"),
            "Health" to Color.parseColor("#FFA500"),
            "Other" to Color.parseColor("#400063"),
            "Water" to Color.parseColor("#008080"),
            "Electricity" to Color.parseColor("#DC143C"),
            "Telecommunication" to Color.parseColor("#808000")
        )

        val entries = mutableListOf<PieEntry>()
        val pieColors = mutableListOf<Int>()

        grouped.forEach { (category, total) ->
            entries.add(PieEntry(total.toFloat(), category))
            pieColors.add(categoryColors[category] ?: Color.GRAY) // fallback color
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = pieColors
            valueTextColor = Color.WHITE
            valueTextSize = 14f
            setDrawValues(true)
            valueFormatter = PercentFormatter(pieChart)
        }

        pieChart.setUsePercentValues(true)
        pieChart.data = PieData(dataSet)

        val centerText = SpannableString("Expenses")
        centerText.setSpan(StyleSpan(Typeface.BOLD), 0, centerText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        pieChart.centerText = centerText
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setCenterTextColor(Color.BLACK)
        pieChart.setCenterTextSize(16f)
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = false
        pieChart.setDrawEntryLabels(false) // hide text on chart
        pieChart.invalidate()

        val numberFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))

        entries.forEachIndexed { index, entry ->
            val colorBox = View(context).apply {
                setBackgroundColor(pieColors[index])
                layoutParams = LinearLayout.LayoutParams(24, 24).apply {
                    marginEnd = 16
                }
            }

            val formattedAmount = numberFormat.format(entry.value.toInt())

            val labelText = TextView(context).apply {
                text = "${entry.label}: $currency $formattedAmount"
                textSize = 16f
                setTextColor(Color.WHITE)
            }

            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
                gravity = Gravity.CENTER_VERTICAL
                addView(colorBox)
                addView(labelText)
            }

            categoryList.addView(row)
        }

        return view
    }
}




