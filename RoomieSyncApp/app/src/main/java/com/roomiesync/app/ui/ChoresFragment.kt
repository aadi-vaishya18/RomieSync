package com.roomiesync.app.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.roomiesync.app.MainActivity
import com.roomiesync.app.Refreshable
import com.roomiesync.app.databinding.FragmentChoresBinding
import com.roomiesync.app.databinding.ItemWorkloadBarBinding

class ChoresFragment : Fragment(), Refreshable {

    private var _binding: FragmentChoresBinding? = null
    private val binding get() = _binding!!

    private lateinit var choresAdapter: ChoreAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChoresBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as MainActivity
        val state = activity.state

        choresAdapter = ChoreAdapter(
            chores = emptyList(),
            state = state,
            showDelete = true,
            onToggle = { chore ->
                chore.done = !chore.done
                activity.saveAndRefresh()
            },
            onDelete = { chore ->
                state.chores.removeAll { it.id == chore.id }
                activity.saveAndRefresh()
            }
        )
        binding.choresList.layoutManager = LinearLayoutManager(requireContext())
        binding.choresList.adapter = choresAdapter

        refresh()
    }

    override fun refresh() {
        if (_binding == null) return
        val activity = requireActivity() as MainActivity
        val state = activity.state

        // --- Workload bars, one per flatmate ---
        binding.workloadContainer.removeAllViews()
        val totals = state.flatmates.associate { f ->
            val total = state.chores.filter { it.assignedTo == f.id }.sumOf { it.weight }
            val done = state.chores.filter { it.assignedTo == f.id && it.done }.sumOf { it.weight }
            f.id to Pair(total, done)
        }
        val maxTotal = (totals.values.maxOfOrNull { it.first } ?: 0).coerceAtLeast(1)

        state.flatmates.forEach { f ->
            val (total, done) = totals[f.id] ?: Pair(0, 0)
            val rowBinding = ItemWorkloadBarBinding.inflate(
                LayoutInflater.from(requireContext()), binding.workloadContainer, false
            )
            rowBinding.workloadName.text = f.name.substringBefore(" ")
            rowBinding.workloadCount.text = "$done/$total wt done"

            val pct = if (maxTotal > 0) (total.toFloat() / maxTotal.toFloat()) else 0f
            (rowBinding.workloadFill.layoutParams as android.widget.LinearLayout.LayoutParams).weight =
                pct.coerceAtLeast(0.02f)
            (rowBinding.workloadSpacer.layoutParams as android.widget.LinearLayout.LayoutParams).weight =
                (1f - pct).coerceAtLeast(0f)
            rowBinding.workloadFill.setBackgroundColor(Color.parseColor(f.color))

            binding.workloadContainer.addView(rowBinding.root)
        }

        // --- Full chore list ---
        val chores = state.chores.sortedBy { if (it.done) 1 else 0 }
        choresAdapter.update(chores)
        binding.choresEmpty.visibility = if (chores.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
