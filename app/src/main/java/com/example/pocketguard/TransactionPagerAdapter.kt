package com.example.pocketguard

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class TransactionPagerAdapter(fragment: FragmentActivity) : FragmentStateAdapter(fragment) {
    val incomeFragment = IncomeFragment()
    val expenseFragment = ExpenseFragment()
    override fun getItemCount() = 2
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> incomeFragment
            else -> expenseFragment
        }
    }
}
