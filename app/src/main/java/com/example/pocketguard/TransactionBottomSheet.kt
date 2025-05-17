package com.example.pocketguard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TransactionBottomSheet(
    private val onEditClicked: () -> Unit,
    private val onDeleteClicked: () -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.transaction_bottom_sheet, container, false)

        view.findViewById<TextView>(R.id.tvEdit).setOnClickListener {
            onEditClicked()
            dismiss()
        }

        view.findViewById<TextView>(R.id.tvDelete).setOnClickListener {
            onDeleteClicked()
            dismiss()
        }

        return view
    }
}
