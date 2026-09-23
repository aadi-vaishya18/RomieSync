package com.roomiesync.app.ui.dialogs

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.roomiesync.app.MainActivity
import com.roomiesync.app.R
import com.roomiesync.app.data.AppRepository
import com.roomiesync.app.data.Flatmate
import com.roomiesync.app.databinding.SheetAddFlatmateBinding

class AddFlatmateSheet : BottomSheetDialogFragment() {

    private var _binding: SheetAddFlatmateBinding? = null
    private val binding get() = _binding!!

    private val palette = listOf("#3A5A78", "#E08E45", "#2E7D5B", "#8A5FB8", "#C25B7C")
    private var selectedColor = palette[0]

    override fun getTheme(): Int = R.style.Theme_RoomieSync_BottomSheet

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = SheetAddFlatmateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as MainActivity
        val state = activity.state

        selectedColor = palette[state.flatmates.size % palette.size]

        val swatchViews = mutableListOf<View>()
        binding.fmColorRow.removeAllViews()
        palette.forEach { hex ->
            val size = (32 * resources.displayMetrics.density).toInt()
            val margin = (6 * resources.displayMetrics.density).toInt()
            val swatch = View(requireContext())
            val params = ViewGroup.MarginLayoutParams(size, size)
            params.setMargins(0, 0, margin, 0)
            swatch.layoutParams = params

            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.OVAL
            drawable.setColor(Color.parseColor(hex))
            if (hex == selectedColor) {
                drawable.setStroke((2 * resources.displayMetrics.density).toInt(), Color.parseColor("#24303A"))
            }
            swatch.background = drawable

            swatch.setOnClickListener {
                selectedColor = hex
                swatchViews.forEachIndexed { i, v ->
                    val d = GradientDrawable()
                    d.shape = GradientDrawable.OVAL
                    d.setColor(Color.parseColor(palette[i]))
                    if (palette[i] == selectedColor) {
                        d.setStroke((2 * resources.displayMetrics.density).toInt(), Color.parseColor("#24303A"))
                    }
                    v.background = d
                }
            }

            swatchViews.add(swatch)
            binding.fmColorRow.addView(swatch)
        }

        binding.fmSaveBtn.setOnClickListener {
            val name = binding.fmNameInput.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Enter a name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            state.flatmates.add(Flatmate(id = AppRepository.newId(), name = name, color = selectedColor))
            activity.saveAndRefresh()
            Toast.makeText(requireContext(), "Flatmate added", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
