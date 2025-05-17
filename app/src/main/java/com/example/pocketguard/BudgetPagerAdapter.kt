package com.example.pocketguard

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class BudgetPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> IncomeExpenseGraphFragment()
            1 -> CategoryExpenseGraphFragment()
            else -> throw IllegalStateException("Invalid position")
        }
    }
}
