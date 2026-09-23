package com.roomiesync.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.roomiesync.app.MainActivity
import com.roomiesync.app.Refreshable
import com.roomiesync.app.data.AppRepository
import com.roomiesync.app.data.DebtTransaction
import com.roomiesync.app.data.Payment
import com.roomiesync.app.databinding.FragmentSettleBinding

class SettleUpFragment : Fragment(), Refreshable {

    private var _binding: FragmentSettleBinding? = null
    private val binding get() = _binding!!

    private lateinit var myBalancesAdapter: BalanceActionableAdapter
    private lateinit var otherBalancesAdapter: BalanceAdapter
    private lateinit var paymentLogAdapter: BalanceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as MainActivity
        val state = activity.state

        myBalancesAdapter = BalanceActionableAdapter(
            rows = emptyList(),
            currentUserId = state.currentUserId,
            onMarkPaid = { tx ->
                state.payments.add(
                    Payment(
                        id = AppRepository.newId(),
                        from = tx.from,
                        to = tx.to,
                        amount = tx.amount,
                        date = AppRepository.today()
                    )
                )
                activity.saveAndRefresh()
            }
        )
        binding.myBalancesList.layoutManager = LinearLayoutManager(requireContext())
        binding.myBalancesList.adapter = myBalancesAdapter

        otherBalancesAdapter = BalanceAdapter(emptyList())
        binding.otherBalancesList.layoutManager = LinearLayoutManager(requireContext())
        binding.otherBalancesList.adapter = otherBalancesAdapter

        paymentLogAdapter = BalanceAdapter(emptyList())
        binding.paymentLogList.layoutManager = LinearLayoutManager(requireContext())
        binding.paymentLogList.adapter = paymentLogAdapter

        refresh()
    }

    override fun refresh() {
        if (_binding == null) return
        val state = (requireActivity() as MainActivity).state
        val net = AppRepository.computeNet(state)
        val allTx = AppRepository.simplifyDebts(net)

        val mine: List<DebtTransaction> = allTx.filter { it.from == state.currentUserId || it.to == state.currentUserId }
        val others: List<DebtTransaction> = allTx.filter { it.from != state.currentUserId && it.to != state.currentUserId }

        myBalancesAdapter.update(mine.map { tx ->
            val otherId = if (tx.from == state.currentUserId) tx.to else tx.from
            tx to state.flatmateById(otherId)
        })
        binding.myBalancesEmpty.visibility = if (mine.isEmpty()) View.VISIBLE else View.GONE
        binding.myBalancesList.visibility = if (mine.isEmpty()) View.GONE else View.VISIBLE

        if (others.isEmpty()) {
            binding.otherBalancesLabel.visibility = View.GONE
            binding.otherBalancesList.visibility = View.GONE
        } else {
            binding.otherBalancesLabel.visibility = View.VISIBLE
            binding.otherBalancesList.visibility = View.VISIBLE
            otherBalancesAdapter.update(others.map { tx ->
                val from = state.flatmateById(tx.from)
                val to = state.flatmateById(tx.to)
                BalanceRow(
                    avatarName = null,
                    avatarColor = null,
                    title = "${from?.name?.substringBefore(" ") ?: "?"} owes ${to?.name?.substringBefore(" ") ?: "?"}",
                    subtitle = "",
                    amountText = "\u20b9${Math.round(tx.amount)}",
                    amountColorHex = "#24303A"
                )
            })
        }

        val payments = state.payments.sortedByDescending { it.date }.take(8)
        paymentLogAdapter.update(payments.map { p ->
            val from = state.flatmateById(p.from)
            val to = state.flatmateById(p.to)
            BalanceRow(
                avatarName = null,
                avatarColor = null,
                title = "${from?.name?.substringBefore(" ") ?: "?"} paid ${to?.name?.substringBefore(" ") ?: "?"}",
                subtitle = AppRepository.formatDate(p.date),
                amountText = "\u20b9${Math.round(p.amount)}",
                amountColorHex = "#24303A"
            )
        })
        binding.paymentLogEmpty.visibility = if (payments.isEmpty()) View.VISIBLE else View.GONE
        binding.paymentLogList.visibility = if (payments.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
