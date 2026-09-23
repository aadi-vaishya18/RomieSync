package com.roomiesync.app.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.roomiesync.app.MainActivity
import com.roomiesync.app.R
import com.roomiesync.app.Refreshable
import com.roomiesync.app.databinding.FragmentProfileBinding
import com.roomiesync.app.ui.dialogs.AddFlatmateSheet

class ProfileFragment : Fragment(), Refreshable {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var flatmatesAdapter: FlatmateAdapter
    private var suppressNameWatcher = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as MainActivity
        val state = activity.state

        flatmatesAdapter = FlatmateAdapter(
            flatmates = emptyList(),
            allowDelete = state.flatmates.size > 1,
            onDelete = { flatmate ->
                if (state.flatmates.size > 1) {
                    state.flatmates.removeAll { it.id == flatmate.id }
                    if (state.currentUserId == flatmate.id) {
                        state.currentUserId = state.flatmates.first().id
                    }
                    activity.saveAndRefresh()
                }
            }
        )
        binding.flatmatesList.layoutManager = LinearLayoutManager(requireContext())
        binding.flatmatesList.adapter = flatmatesAdapter

        binding.flatNameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (suppressNameWatcher) return
                val name = s?.toString()?.trim().orEmpty()
                if (name.isNotEmpty()) {
                    state.flatName = name
                    activity.saveAndRefresh()
                }
            }
        })

        binding.addFlatmateBtn.setOnClickListener {
            AddFlatmateSheet().show(activity.supportFragmentManager, "add_flatmate")
        }

        binding.resetDataBtn.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Reset demo data?")
                .setMessage("This clears everything in this app and restores the original demo data.")
                .setPositiveButton("Reset") { _, _ -> activity.resetDemoData() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        refresh()
    }

    override fun refresh() {
        if (_binding == null) return
        val activity = requireActivity() as MainActivity
        val state = activity.state

        suppressNameWatcher = true
        if (binding.flatNameInput.text.toString() != state.flatName) {
            binding.flatNameInput.setText(state.flatName)
        }
        suppressNameWatcher = false

        // "You are" chip group
        binding.whoamiChipGroup.removeAllViews()
        state.flatmates.forEach { f ->
            val chip = Chip(requireContext())
            chip.text = f.name.substringBefore(" ")
            chip.isCheckable = true
            chip.isChecked = f.id == state.currentUserId
            chip.chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.chip_bg_selector)
            chip.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_selector))
            chip.chipStrokeWidth = 0f
            chip.setOnClickListener {
                state.currentUserId = f.id
                activity.saveAndRefresh()
            }
            binding.whoamiChipGroup.addView(chip)
        }

        flatmatesAdapter.update(state.flatmates, newAllowDelete = state.flatmates.size > 1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
