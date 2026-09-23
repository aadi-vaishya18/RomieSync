package com.roomiesync.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.roomiesync.app.MainActivity
import com.roomiesync.app.Refreshable
import com.roomiesync.app.databinding.FragmentExpensesBinding

class ExpensesFragment : Fragment(), Refreshable {

    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!

    private lateinit var expensesAdapter: ExpenseAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as MainActivity
        val state = activity.state

        expensesAdapter = ExpenseAdapter(
            expenses = emptyList(),
            state = state,
            onDelete = { expense ->
                state.expenses.removeAll { it.id == expense.id }
                activity.saveAndRefresh()
            }
        )
        binding.expensesList.layoutManager = LinearLayoutManager(requireContext())
        binding.expensesList.adapter = expensesAdapter

        refresh()
    }

    override fun refresh() {
        if (_binding == null) return
        val state = (requireActivity() as MainActivity).state
        val expenses = state.expenses.sortedByDescending { it.date }
        expensesAdapter.update(expenses)
        binding.expensesEmpty.visibility = if (expenses.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
