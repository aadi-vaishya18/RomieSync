package com.roomiesync.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.roomiesync.app.R
import com.roomiesync.app.data.AppState
import com.roomiesync.app.data.Chore
import com.roomiesync.app.databinding.ItemChoreBinding

class ChoreAdapter(
    private var chores: List<Chore>,
    private val state: AppState,
    private val showDelete: Boolean,
    private val onToggle: (Chore) -> Unit,
    private val onDelete: (Chore) -> Unit
) : RecyclerView.Adapter<ChoreAdapter.VH>() {

    inner class VH(val binding: ItemChoreBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemChoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val chore = chores[position]
        val assignee = state.flatmateById(chore.assignedTo)
        val weightDots = "\u25CF".repeat(chore.weight) + "\u25CB".repeat(3 - chore.weight)

        holder.binding.choreTitle.text = "${chore.title}  $weightDots"
        holder.binding.choreSubtitle.text = "Assigned to ${assignee?.name?.substringBefore(" ") ?: "\u2014"}"

        if (chore.done) {
            holder.binding.choreCheckbox.setBackgroundResource(R.drawable.bg_checkbox_on)
            holder.binding.choreCheckIcon.visibility = android.view.View.VISIBLE
            holder.binding.choreTitle.paintFlags =
                holder.binding.choreTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            holder.binding.choreTitle.alpha = 0.5f
        } else {
            holder.binding.choreCheckbox.setBackgroundResource(R.drawable.bg_checkbox_off)
            holder.binding.choreCheckIcon.visibility = android.view.View.GONE
            holder.binding.choreTitle.paintFlags =
                holder.binding.choreTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.binding.choreTitle.alpha = 1f
        }

        holder.binding.choreCheckbox.setOnClickListener { onToggle(chore) }
        holder.binding.choreDelete.visibility = if (showDelete) android.view.View.VISIBLE else android.view.View.GONE
        holder.binding.choreDelete.setOnClickListener { onDelete(chore) }
    }

    override fun getItemCount(): Int = chores.size

    fun update(newChores: List<Chore>) {
        chores = newChores
        notifyDataSetChanged()
    }
}
