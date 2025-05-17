package com.example.pocketguard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.pocketguard.databinding.FragmentHomeBinding
import com.example.pocketguard.utils.NotificationUtils
import com.example.pocketguard.utils.SharedPrefManager
import com.example.pocketguard.models.User
import com.example.pocketguard.models.Income
import com.example.pocketguard.models.Expense
import com.example.pocketguard.models.Budget
import com.google.gson.Gson
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {
    private val TAG = "HomeFragment"
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted")
            updateUIAndShowNotification()
        } else {
            Log.d(TAG, "Notification permission denied")
            Toast.makeText(context, "Notification permission is required for budget updates", Toast.LENGTH_LONG).show()
        }
    }

    private val pickBackupFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { restoreFromBackup(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView called")
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupBackupRestoreButtons()
        checkNotificationPermission()
        return binding.root
    }

    private fun setupBackupRestoreButtons() {
        binding.btnBackup.setOnClickListener {
            createBackup()
        }

        binding.btnRestore.setOnClickListener {
            pickBackupFile.launch("application/json")
        }
    }

    private fun createBackup() {
        try {
            val sharedPrefManager = SharedPrefManager(requireContext())
            val currentUserPhone = sharedPrefManager.getCurrentUserPhone() ?: return

            // Create backup data
            val backupData = BackupData(
                user = sharedPrefManager.findUserByPhone(currentUserPhone),
                incomeList = sharedPrefManager.getIncomeList(currentUserPhone),
                expenseList = sharedPrefManager.getExpenseList(currentUserPhone),
                budget = sharedPrefManager.getBudgetForUser(currentUserPhone),
                currency = sharedPrefManager.getUserCurrency(currentUserPhone),
                categories = sharedPrefManager.getCategories(),
                settings = BackupSettings(
                    notificationsEnabled = sharedPrefManager.getNotificationEnabled(currentUserPhone),
                    darkModeEnabled = sharedPrefManager.getDarkModeEnabled(currentUserPhone)
                )
            )

            // Convert to JSON
            val json = Gson().toJson(backupData)

            // Create backup file
            val backupDir = File(requireContext().getExternalFilesDir(null), "Backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "pocketguard_backup_$timestamp.json")
            backupFile.writeText(json)

            // Share the backup file
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                backupFile
            )

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "application/json"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share Backup File"))

            Toast.makeText(requireContext(), "Backup created successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating backup", e)
            Toast.makeText(requireContext(), "Failed to create backup", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restoreFromBackup(uri: Uri) {
        try {
            val json = requireContext().contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                ?: throw Exception("Could not read backup file")

            val backupData = Gson().fromJson(json, BackupData::class.java)
            val sharedPrefManager = SharedPrefManager(requireContext())
            val currentUserPhone = sharedPrefManager.getCurrentUserPhone() ?: return

            // Restore data
            backupData.user?.let { sharedPrefManager.saveUser(it) }
            sharedPrefManager.saveIncomeList(currentUserPhone, backupData.incomeList)
            sharedPrefManager.saveExpenseList(currentUserPhone, backupData.expenseList)
            backupData.budget?.let { sharedPrefManager.setBudgetForUser(currentUserPhone, it) }
            sharedPrefManager.setUserCurrency(currentUserPhone, backupData.currency)
            sharedPrefManager.saveCategories(backupData.categories)
            sharedPrefManager.setNotificationEnabled(currentUserPhone, backupData.settings.notificationsEnabled)
            sharedPrefManager.setDarkModeEnabled(currentUserPhone, backupData.settings.darkModeEnabled)

            Toast.makeText(requireContext(), "Backup restored successfully", Toast.LENGTH_SHORT).show()
            updateUIAndShowNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup", e)
            Toast.makeText(requireContext(), "Failed to restore backup", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "Notification permission already granted")
                    updateUIAndShowNotification()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Toast.makeText(
                        context,
                        "Notification permission is required for budget updates",
                        Toast.LENGTH_LONG
                    ).show()
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            updateUIAndShowNotification()
        }
    }

    private fun updateUIAndShowNotification() {
        try {
            val sharedPrefManager = SharedPrefManager(requireContext())
            val currentUserPhone = sharedPrefManager.getCurrentUserPhone()
            
            if (currentUserPhone == null) {
                Log.e(TAG, "Current user phone is null")
                Toast.makeText(context, "Please log in first", Toast.LENGTH_SHORT).show()
                return
            }

            val calculator = TransactionClaculations(sharedPrefManager)

            val resIncome = calculator.getTotalIncome(currentUserPhone)
            val resExpense = calculator.getTotalExpense(currentUserPhone)
            val resBalance = calculator.getBalance(currentUserPhone)
            val currency = sharedPrefManager.getUserCurrency(currentUserPhone) ?: "$"

            val symbols = DecimalFormatSymbols().apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val formatter = DecimalFormat("#,##0.00", symbols)

            binding.apply {
                balance.text = "${currency} ${formatter.format(resBalance)}"
                income.text = "${currency} ${formatter.format(resIncome)}"
                expense.text = "${currency} ${formatter.format(resExpense)}"
            }

            // Show notifications only if we have a valid user
            showBudgetNotification(sharedPrefManager, currentUserPhone, currency)
            showDailyReminder(sharedPrefManager, currentUserPhone)
            checkAndShowMonthlyBudgetReminder(sharedPrefManager, currentUserPhone)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating UI", e)
            Toast.makeText(context, "Error updating dashboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDailyReminder(sharedPrefManager: SharedPrefManager, currentUserPhone: String) {
        try {
            val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            val lastReminderDay = sharedPrefManager.getInt("last_reminder_day_$currentUserPhone", -1)

            if (lastReminderDay != today) {
                NotificationUtils.showNotification(
                    context = requireContext(),
                    title = "Daily Transaction Reminder",
                    message = "📝 Don't forget to record your transactions for today!",
                    id = 107
                )
                sharedPrefManager.putInt("last_reminder_day_$currentUserPhone", today)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing daily reminder", e)
        }
    }

    private fun checkAndShowMonthlyBudgetReminder(sharedPrefManager: SharedPrefManager, currentUserPhone: String) {
        try {
            val calendar = Calendar.getInstance()
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
            val currentMonth = calendar.get(Calendar.MONTH)
            val lastBudgetReminderMonth = sharedPrefManager.getInt("last_budget_reminder_month_$currentUserPhone", -1)

            if (currentDay == 1 && currentMonth != lastBudgetReminderMonth) {
                val budgetLimit = sharedPrefManager.getBudgetForUser(currentUserPhone)
                if (budgetLimit == null) {
                    NotificationUtils.showNotification(
                        context = requireContext(),
                        title = "Monthly Budget Reminder",
                        message = "📊 It's the beginning of a new month! Don't forget to set your monthly budget limit.",
                        id = 108
                    )
                    sharedPrefManager.putInt("last_budget_reminder_month_$currentUserPhone", currentMonth)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing monthly budget reminder", e)
        }
    }

    private fun showBudgetNotification(sharedPrefManager: SharedPrefManager, currentUserPhone: String, currency: String) {
        try {
            val budgetLimit = sharedPrefManager.getBudgetForUser(currentUserPhone)?.toDouble()
            if (budgetLimit == null) {
                Log.d(TAG, "Budget limit not set for user")
                return
            }

            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH) + 1
            val currentYear = calendar.get(Calendar.YEAR)

            val expenses = sharedPrefManager.getExpenseList(currentUserPhone) ?: emptyList()
            val thisMonthExpenses = expenses.filter { expense: Expense ->
                val dateStr = expense.date ?: return@filter false
                val parts = dateStr.split("/")
                if (parts.size == 3) {
                    val month = parts[1].toIntOrNull()
                    val year = parts[2].toIntOrNull()
                    month == currentMonth && year == currentYear
                } else false
            }

            val totalExpenses: Double = thisMonthExpenses.sumOf { expense: Expense -> expense.amount }
            val remaining: Double = budgetLimit - totalExpenses
            val budgetPercentage: Double = (totalExpenses / budgetLimit) * 100.0

            val message = StringBuilder().apply {
                append("💰 Budget: $currency ${String.format("%.2f", budgetLimit)}\n")
                append("💸 Spent: $currency ${String.format("%.2f", totalExpenses)}\n")
                append("✅ Remaining: $currency ${String.format("%.2f", remaining)}\n")
                append("📊 Used: ${String.format("%.1f", budgetPercentage)}%")
            }.toString()

            Log.d(TAG, "Showing budget notification: $message")

            NotificationUtils.showNotification(
                context = requireContext(),
                title = "Monthly Budget Summary",
                message = message,
                id = 106
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error showing budget notification", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class BackupData(
    val user: User?,
    val incomeList: List<Income>,
    val expenseList: List<Expense>,
    val budget: Double?,
    val currency: String,
    val categories: List<String>,
    val settings: BackupSettings
)

data class BackupSettings(
    val notificationsEnabled: Boolean,
    val darkModeEnabled: Boolean
)






