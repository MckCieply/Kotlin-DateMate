package com.mckcieply.datemate.ui.home

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.mckcieply.datemate.databinding.FragmentHomeBinding
import java.util.Calendar

class HomeFragment : Fragment() {

    // ViewBinding reference for fragment layout
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var startDate = ""
    private var endDate = ""

    /**
     * Inflates the layout using view binding.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Sets up click listener to display entered form data.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.startTimeInput.setOnClickListener{
            showDateTimePickerDialog(true)
        }

        binding.endTimeInput.setOnClickListener{
            showDateTimePickerDialog(false)
        }


        binding.submitButton.setOnClickListener {
            val title = binding.titleInput.text.toString()
            val description = binding.descriptionInput.text.toString()
            val location = binding.locationInput.text.toString()
//            val start = binding.startTimeInput.text.toString()
//            val end = binding.endTimeInput.text.toString()

            val summary = """
                Title: $title
                Description: $description
                Location: $location
                From: $startDate
                To: $endDate
            """.trimIndent()

            Toast.makeText(requireContext(), summary, Toast.LENGTH_LONG).show()
        }
    }

    private fun showDateTimePickerDialog(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()

        // DatePickerDialog setup
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            // Set the date based on user input
            val date = "$year-${month + 1}-$dayOfMonth"

            if (isStartTime) {
                startDate = date
                binding.startTimeInput.setText(date)
            } else {
                endDate = date
                binding.endTimeInput.setText(date)
            }
//            showTimePickerDialog(isStartTime, date) // Show TimePicker after date selection
        }

        val datePickerDialog = DatePickerDialog(
            requireContext(), dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}