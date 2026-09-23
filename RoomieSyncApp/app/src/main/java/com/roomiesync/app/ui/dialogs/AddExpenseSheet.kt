package com.roomiesync.app.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.roomiesync.app.MainActivity
import com.roomiesync.app.R
import com.roomiesync.app.data.AppRepository
import com.roomiesync.app.data.Expense
import com.roomiesync.app.databinding.SheetAddExpenseBinding

class AddExpenseSheet : BottomSheetDialogFragment() {

    private var _binding: SheetAddExpenseBinding? = null
    private val binding get() = _binding!!

    private val categories = listOf("Groceries", "Utilities", "Rent", "Wifi", "Household", "Other")
    private val selectedParticipants = mutableSetOf<String>()

    override fun getTheme(): Int = R.style.Theme_RoomieSync_BottomSheet

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = SheetAddExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as MainActivity
        val state = activity.state

        binding.expCategorySpinner.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, categories
        )

        val flatmateNames = state.flatmates.map { it.name }
        binding.expPaidBySpinner.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, flatmateNames
        )
        val currentIndex = state.flatmates.indexOfFirst { it.id == state.currentUserId }
        if (currentIndex >= 0) binding.expPaidBySpinner.setSelection(currentIndex)

        selectedParticipants.addAll(state.flatmates.map { it.id })
        binding.expParticipantsChipGroup.removeAllViews()
        state.flatmates.forEach { f ->
            val chip = Chip(requireContext())
            chip.text = f.name.substringBefore(" ")
            chip.isCheckable = true
            chip.isChecked = true
            chip.chipBackgroundColor = androidx.core.content.ContextCompat.getColorStateList(
                requireContext(), R.color.chip_bg_selector
            )
            chip.setTextColor(
                androidx.core.content.ContextCompat.getColorStateList(requireContext(), R.color.chip_text_selector)
            )
            chip.chipStrokeWidth = 0f
            chip.setOnClickListener {
                if (chip.isChecked) {
                    selectedParticipants.add(f.id)
                } else {
                    if (selectedParticipants.size > 1) {
                        selectedParticipants.remove(f.id)
                    } else {
                        // Never allow zero participants — snap back to checked.
                        chip.isChecked = true
                    }
                }
            }
            binding.expParticipantsChipGroup.addView(chip)
        }

        binding.expSaveBtn.setOnClickListener {
            val title = binding.expTitleInput.text.toString().trim()
            val amountText = binding.expAmountInput.text.toString().trim()
            val amount = amountText.toDoubleOrNull()

            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Give the expense a title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (amount == null || amount <= 0) {
                Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category = categories[binding.expCategorySpinner.selectedItemPosition]
            val paidBy = state.flatmates[binding.expPaidBySpinner.selectedItemPosition].id

            state.expenses.add(
                Expense(
                    id = AppRepository.newId(),
                    title = title,
                    amount = amount,
                    category = category,
                    paidBy = paidBy,
                    participants = selectedParticipants.toMutableList(),
                    date = AppRepository.today()
                )
            )
            activity.saveAndRefresh()
            Toast.makeText(requireContext(), "Expense added", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
