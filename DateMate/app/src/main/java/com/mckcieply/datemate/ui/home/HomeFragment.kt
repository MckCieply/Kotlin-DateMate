package com.mckcieply.datemate.ui.home

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.mckcieply.datemate.GoogleAPIManager
import com.mckcieply.datemate.MainActivity
import com.mckcieply.datemate.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var startDate = ""
    private var endDate = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.startTimeInput.setOnClickListener {
            showDatePickerDialog(true)
        }

        binding.endTimeInput.setOnClickListener {
            showDatePickerDialog(false)
        }

        binding.submitButton.setOnClickListener {
            val title = binding.titleInput.text.toString()
            val description = binding.descriptionInput.text.toString()
            val location = binding.locationInput.text.toString()

            if (startDate.isEmpty() || endDate.isEmpty() || title.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter title and select start & end times", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val accessToken = (activity as? MainActivity)?.accessToken
            if (accessToken == null) {
                Toast.makeText(requireContext(), "No access token available. Please sign in.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Convert date strings to RFC3339 format for Google Calendar
            val startDateTimeRFC3339 = convertToRFC3339(startDate)
            val endDateTimeRFC3339 = convertToRFC3339(endDate)

            if (startDateTimeRFC3339 == null || endDateTimeRFC3339 == null) {
                Toast.makeText(requireContext(), "Invalid date/time format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Call the create event function
            GoogleAPIManager.createCalendarEvent(
                accessToken = accessToken,
                title = "DateMate: $title",
                description = description,
                location = location,
                startDateTime = startDateTimeRFC3339,
                endDateTime = endDateTimeRFC3339
            ) { success, response ->
                activity?.runOnUiThread {
                    if (success) {
                        showCustomToast("Event created successfully!")
                    } else {
                        showCustomToast("Failed to create event.")
                        Log.e("HomeFragment", "Create event failed: $response")
                    }
                }
            }
        }
    }

    private fun showDatePickerDialog(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            val date = "$year-${month + 1}-$dayOfMonth"
            showTimePickerDialog(isStartTime, date)
        }

        DatePickerDialog(
            requireContext(),
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePickerDialog(isStartTime: Boolean, date: String) {
        val calendar = Calendar.getInstance()
        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            val time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
            val fullDateTime = "$date $time"

            if (isStartTime) {
                startDate = fullDateTime
                binding.startTimeInput.setText(fullDateTime)
            } else {
                endDate = fullDateTime
                binding.endTimeInput.setText(fullDateTime)
            }
        }

        TimePickerDialog(
            requireContext(),
            timeSetListener,
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun convertToRFC3339(dateTime: String): String? {
        // Input example: "2025-5-21 14:30"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-M-d HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateTime)
            val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            outputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
            date?.let { outputFormat.format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun showCustomToast(message: String) {
        val toast = Toast(requireContext())
        toast.duration = Toast.LENGTH_LONG

        val toastText = TextView(requireContext()).apply {
            text = message
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.BLACK)
            setTextColor(Color.WHITE)
        }

        toast.view = toastText
        toast.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
