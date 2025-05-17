package com.example.pocketguard

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class TransactionFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var fab: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_transaction, container, false)
        val filterButton:ImageButton = view.findViewById(R.id.filter)

        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)
        fab = view.findViewById(R.id.fabAddTransaction)

        val transactionAdapter = TransactionPagerAdapter(requireActivity())
        viewPager.adapter = transactionAdapter


        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "Incomes" else "Expenses"
        }.attach()

        filterButton.setOnClickListener {
            when (viewPager.currentItem) {
                0 -> transactionAdapter.incomeFragment.showFilterDialog()
                1 -> transactionAdapter.expenseFragment.showFilterDialog()
            }
        }

        fab.setOnClickListener{
            val intent =Intent(requireContext(),AddTransaction::class.java)
            startActivity(intent)
        }



        return view
    }
}