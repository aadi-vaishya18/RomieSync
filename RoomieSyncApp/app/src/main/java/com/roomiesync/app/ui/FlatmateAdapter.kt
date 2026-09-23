package com.roomiesync.app.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.roomiesync.app.data.Flatmate
import com.roomiesync.app.databinding.ItemFlatmateBinding

class FlatmateAdapter(
    private var flatmates: List<Flatmate>,
    private var allowDelete: Boolean,
    private val onDelete: (Flatmate) -> Unit
) : RecyclerView.Adapter<FlatmateAdapter.VH>() {

    inner class VH(val binding: ItemFlatmateBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemFlatmateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val flatmate = flatmates[position]
        holder.binding.flatmateName.text = flatmate.name
        holder.binding.flatmateAvatar.text = Flatmate.initials(flatmate.name)
        holder.binding.flatmateAvatar.background.mutate().setTint(Color.parseColor(flatmate.color))
        holder.binding.flatmateDelete.visibility =
            if (allowDelete) android.view.View.VISIBLE else android.view.View.INVISIBLE
        holder.binding.flatmateDelete.setOnClickListener { onDelete(flatmate) }
    }

    override fun getItemCount(): Int = flatmates.size

    fun update(newFlatmates: List<Flatmate>, newAllowDelete: Boolean? = null) {
        flatmates = newFlatmates
        if (newAllowDelete != null) allowDelete = newAllowDelete
        notifyDataSetChanged()
    }
}
