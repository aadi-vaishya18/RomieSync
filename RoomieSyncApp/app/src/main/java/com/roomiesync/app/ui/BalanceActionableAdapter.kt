package com.roomiesync.app.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.roomiesync.app.data.DebtTransaction
import com.roomiesync.app.data.Flatmate
import com.roomiesync.app.databinding.ItemBalanceActionableBinding

class BalanceActionableAdapter(
    private var rows: List<Pair<DebtTransaction, Flatmate?>>,
    private val currentUserId: String,
    private val onMarkPaid: (DebtTransaction) -> Unit
) : RecyclerView.Adapter<BalanceActionableAdapter.VH>() {

    inner class VH(val binding: ItemBalanceActionableBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemBalanceActionableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (tx, other) = rows[position]
        val youOwe = tx.from == currentUserId
        val otherFirstName = other?.name?.substringBefore(" ") ?: "\u2014"

        holder.binding.balTitle.text = if (youOwe) "You owe $otherFirstName" else "$otherFirstName owes you"
        holder.binding.balSubtitle.text = if (youOwe) "Settle up to clear this" else "Waiting for repayment"
        holder.binding.balAmount.text = "\u20b9${Math.round(tx.amount)}"
        holder.binding.balAmount.setTextColor(
            Color.parseColor(if (youOwe) "#B8442F" else "#2E7D5B")
        )
        holder.binding.balAvatar.text = other?.let { Flatmate.initials(it.name) } ?: "?"
        holder.binding.balAvatar.background.mutate().setTint(
            Color.parseColor(other?.color ?: "#999999")
        )

        holder.binding.balMarkPaidBtn.setOnClickListener { onMarkPaid(tx) }
    }

    override fun getItemCount(): Int = rows.size

    fun update(newRows: List<Pair<DebtTransaction, Flatmate?>>) {
        rows = newRows
        notifyDataSetChanged()
    }
}
