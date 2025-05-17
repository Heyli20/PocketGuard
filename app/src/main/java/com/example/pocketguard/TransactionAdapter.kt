package com.example.pocketguard

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketguard.models.Expense
import com.example.pocketguard.models.Income
import java.text.DecimalFormat

class TransactionAdapter<T>(
    private val context: Context,
    private var list: List<T>,
    private val currency: String,
    private val onItemClick: (T) -> Unit
) : RecyclerView.Adapter<TransactionAdapter<T>.TransactionViewHolder>() {

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        val title: TextView = itemView.findViewById(R.id.tvTitle)
        val amount: TextView = itemView.findViewById(R.id.tvAmount)
        val date: TextView = itemView.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val item = list[position]
        val formatter = DecimalFormat("#,##0.00")

        if (item is Income) {
            holder.title.text = item.title
            holder.amount.text = "$currency ${formatter.format(item.amount)}"
            holder.date.text = item.date
            holder.ivIcon.setImageResource(R.drawable.salary)
        } else if (item is Expense) {
            holder.title.text = item.title
            holder.amount.text = "$currency ${formatter.format(item.amount)}"
            holder.date.text = item.date

            val iconRes = when (item.category) {
                "Food" -> R.drawable.food
                "Transport" -> R.drawable.transport
                "Insurance" -> R.drawable.insurance
                "Water" -> R.drawable.drop
                "Electricity" -> R.drawable.lightning
                "Telecommunication" -> R.drawable.telecommunication
                "Health" -> R.drawable.healthcare
                else -> R.drawable.more
            }
            holder.ivIcon.setImageResource(iconRes)
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    fun updateList(newList: List<T>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun getItemCount() = list.size
}

