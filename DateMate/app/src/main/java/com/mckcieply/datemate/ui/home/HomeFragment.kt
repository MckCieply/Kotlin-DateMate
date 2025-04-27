package com.mckcieply.datemate.ui.home

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
            showDatePickerDialog(true)
        }

        binding.endTimeInput.setOnClickListener{
            showDatePickerDialog(false)
        }


        binding.submitButton.setOnClickListener {
            val title = binding.titleInput.text.toString()
            val description = binding.descriptionInput.text.toString()
            val location = binding.locationInput.text.toString()

            val summary = """
                Title: $title
                Description: $description
                Location: $location
                From: $startDate
                To: $endDate
            """.trimIndent()

            showCustomToast(summary)
        }
    }

    private fun showDatePickerDialog(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()

        // DatePickerDialog setup
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            // Set the date based on user input
            val date = "$year-${month + 1}-$dayOfMonth"

            showTimePickerDialog(isStartTime, date)
        }

        val datePickerDialog = DatePickerDialog(
            requireContext(), dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showTimePickerDialog(isStartTime: Boolean, date: String) {
        val calendar = Calendar.getInstance()

        // TimePickerDialog setup
        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            val time = "$hourOfDay:$minute"
            val fullDateTime = "$date $time"

            if (isStartTime) {
                startDate = fullDateTime
                binding.startTimeInput.setText(fullDateTime)
            } else {
                endDate = fullDateTime
                binding.endTimeInput.setText(fullDateTime)
            }
        }

        val timePickerDialog = TimePickerDialog(
            requireContext(), timeSetListener,
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }

    private fun showCustomToast(message: String) {
        // Create the Toast
        val toast = Toast(requireContext())
        toast.duration = Toast.LENGTH_LONG

        // Create a TextView and set it as the toast view
        val toastText = TextView(requireContext())
        toastText.text = message
        toastText.setPadding(16, 16, 16, 16)  // Add padding for better readability
        toastText.setBackgroundColor(Color.BLACK)  // Set background color to black
        toastText.setTextColor(Color.WHITE)  // Set text color to white

        // Ensure that LayoutParams is set
        val params = toastText.layoutParams ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        // Optional: Set maximum width for the TextView
        val maxWidth = 800  // You can adjust this value depending on your screen size
        params.width = maxWidth
        toastText.layoutParams = params

        toast.view = toastText  // Set the custom TextView as the toast view
        toast.show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}