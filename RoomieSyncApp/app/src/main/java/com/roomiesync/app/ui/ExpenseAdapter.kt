package com.roomiesync.app.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.roomiesync.app.data.AppRepository
import com.roomiesync.app.data.AppState
import com.roomiesync.app.data.Expense
import com.roomiesync.app.data.Flatmate
import com.roomiesync.app.databinding.ItemExpenseBinding

class ExpenseAdapter(
    private var expenses: List<Expense>,
    private val state: AppState,
    private val onDelete: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.VH>() {

    inner class VH(val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val expense = expenses[position]
        val payer = state.flatmateById(expense.paidBy)

        holder.binding.expenseTitle.text = expense.title
        holder.binding.expenseSubtitle.text =
            "Paid by ${payer?.name?.substringBefore(" ") ?: "\u2014"} \u00b7 split ${expense.participants.size} ways \u00b7 ${AppRepository.formatDate(expense.date)}"
        holder.binding.expenseAmount.text = "\u20b9${Math.round(expense.amount)}"
        holder.binding.expenseAvatar.text = payer?.let { Flatmate.initials(it.name) } ?: "?"
        holder.binding.expenseAvatar.background.mutate().setTint(
            Color.parseColor(payer?.color ?: "#999999")
        )

        holder.binding.expenseDelete.setOnClickListener { onDelete(expense) }
    }

    override fun getItemCount(): Int = expenses.size

    fun update(newExpenses: List<Expense>) {
        expenses = newExpenses
        notifyDataSetChanged()
    }
}
