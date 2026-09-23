package com.roomiesync.app.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.roomiesync.app.MainActivity
import com.roomiesync.app.R
import com.roomiesync.app.data.AppRepository
import com.roomiesync.app.data.Chore
import com.roomiesync.app.databinding.SheetAddChoreBinding

class AddChoreSheet : BottomSheetDialogFragment() {

    private var _binding: SheetAddChoreBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.Theme_RoomieSync_BottomSheet

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = SheetAddChoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as MainActivity
        val state = activity.state

        val flatmateNames = state.flatmates.map { it.name }
        binding.choreAssigneeSpinner.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, flatmateNames
        )
        val currentIndex = state.flatmates.indexOfFirst { it.id == state.currentUserId }
        if (currentIndex >= 0) binding.choreAssigneeSpinner.setSelection(currentIndex)

        binding.choreSaveBtn.setOnClickListener {
            val title = binding.choreTitleInput.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Give the chore a name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val assignee = state.flatmates[binding.choreAssigneeSpinner.selectedItemPosition].id
            val weight = when (binding.choreWeightGroup.checkedRadioButtonId) {
                binding.weightNormal.id -> 2
                binding.weightHeavy.id -> 3
                else -> 1
            }

            state.chores.add(
                Chore(
                    id = AppRepository.newId(),
                    title = title,
                    assignedTo = assignee,
                    weight = weight,
                    done = false,
                    createdAt = AppRepository.today()
                )
            )
            activity.saveAndRefresh()
            Toast.makeText(requireContext(), "Chore added", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
