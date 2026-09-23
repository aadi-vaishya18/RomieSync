package com.roomiesync.app.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.roomiesync.app.data.Flatmate
import com.roomiesync.app.databinding.ItemBalanceBinding

/** A generic read-only row: avatar + title + subtitle + amount. Used for "other flat balances" and the payment log. */
data class BalanceRow(
    val avatarName: String?,
    val avatarColor: String?,
    val title: String,
    val subtitle: String,
    val amountText: String,
    val amountColorHex: String
)

class BalanceAdapter(private var rows: List<BalanceRow>) : RecyclerView.Adapter<BalanceAdapter.VH>() {

    inner class VH(val binding: ItemBalanceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemBalanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = rows[position]
        holder.binding.balanceTitle.text = row.title
        holder.binding.balanceSubtitle.text = row.subtitle
        holder.binding.balanceAmount.text = row.amountText
        holder.binding.balanceAmount.setTextColor(Color.parseColor(row.amountColorHex))

        if (row.avatarName != null) {
            holder.binding.balanceAvatar.visibility = android.view.View.VISIBLE
            holder.binding.balanceAvatar.text = Flatmate.initials(row.avatarName)
            holder.binding.balanceAvatar.background.mutate().setTint(
                Color.parseColor(row.avatarColor ?: "#999999")
            )
        } else {
            holder.binding.balanceAvatar.visibility = android.view.View.GONE
        }
    }

    override fun getItemCount(): Int = rows.size

    fun update(newRows: List<BalanceRow>) {
        rows = newRows
        notifyDataSetChanged()
    }
}
