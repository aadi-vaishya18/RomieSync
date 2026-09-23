package com.roomiesync.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.roomiesync.app.MainActivity
import com.roomiesync.app.R
import com.roomiesync.app.Refreshable
import com.roomiesync.app.data.AppRepository
import com.roomiesync.app.databinding.FragmentHomeBinding

class HomeFragment : Fragment(), Refreshable {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var choresAdapter: ChoreAdapter
    private lateinit var expensesAdapter: ExpenseAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as MainActivity
        val state = activity.state

        choresAdapter = ChoreAdapter(
            chores = emptyList(),
            state = state,
            showDelete = false,
            onToggle = { chore ->
                chore.done = !chore.done
                activity.saveAndRefresh()
            },
            onDelete = { }
        )
        binding.homeChoresList.layoutManager = LinearLayoutManager(requireContext())
        binding.homeChoresList.adapter = choresAdapter

        expensesAdapter = ExpenseAdapter(
            expenses = emptyList(),
            state = state,
            onDelete = { }
        )
        binding.homeExpensesList.layoutManager = LinearLayoutManager(requireContext())
        binding.homeExpensesList.adapter = expensesAdapter

        binding.settleUpLink.setOnClickListener { activity.openSettleUp() }

        refresh()
    }

    override fun refresh() {
        if (_binding == null) return
        val activity = requireActivity() as MainActivity
        val state = activity.state
        val net = AppRepository.computeNet(state)
        val mine = net[state.currentUserId] ?: 0.0

        when {
            mine > 0.5 -> {
                binding.balanceLabel.text = "You are owed"
                binding.balanceAmountHome.text = "\u20b9${Math.round(mine)}"
                binding.balanceAmountHome.setTextColor(ContextCompat.getColor(requireContext(), R.color.moss))
            }
            mine < -0.5 -> {
                binding.balanceLabel.text = "You owe overall"
                binding.balanceAmountHome.text = "\u20b9${Math.round(-mine)}"
                binding.balanceAmountHome.setTextColor(ContextCompat.getColor(requireContext(), R.color.clay))
            }
            else -> {
                binding.balanceLabel.text = "You're all settled up"
                binding.balanceAmountHome.text = "\u2713"
                binding.balanceAmountHome.setTextColor(ContextCompat.getColor(requireContext(), R.color.ink))
            }
        }

        val chores = state.chores.sortedBy { if (it.done) 1 else 0 }.take(4)
        choresAdapter.update(chores)
        binding.homeChoresEmpty.visibility = if (chores.isEmpty()) View.VISIBLE else View.GONE

        val expenses = state.expenses.sortedByDescending { it.date }.take(3)
        expensesAdapter.update(expenses)
        binding.homeExpensesEmpty.visibility = if (expenses.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
