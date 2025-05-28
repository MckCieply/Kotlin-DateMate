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

/**
 * Fragment for creating and submitting Google Calendar events.
 */
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
        setupListeners()
    }

    /**
     * Sets up input and button click listeners.
     */
    private fun setupListeners() {
        binding.startTimeInput.setOnClickListener {
            showDatePickerDialog(isStartTime = true)
        }

        binding.endTimeInput.setOnClickListener {
            showDatePickerDialog(isStartTime = false)
        }

        binding.submitButton.setOnClickListener {
            handleSubmit()
        }
    }

    /**
     * Handles event submission to Google Calendar.
     */
    private fun handleSubmit() {
        val title = binding.titleInput.text.toString()
        var description = binding.descriptionInput.text.toString()
        val location = binding.locationInput.text.toString()
        val startDateTimeRFC3339 = startDate.toRFC3339() ?: return showInvalidDateToast()
        val endDateTimeRFC3339 = endDate.toRFC3339() ?: return showInvalidDateToast()

        if(description.length == 0){
            val idea = GiftIdeas().getRandomGiftIdea()
            description = "Gift Idea: $idea"
        }
        
        if (!validateInputs(title)) return

        GoogleAPIManager.createCalendarEvent(
            title = title,
            description = description,
            location = location,
            startDateTime = startDateTimeRFC3339,
            endDateTime = endDateTimeRFC3339,
            callback = ::handleEventResult
        )
    }

    /**
     * Validates required input fields.
     */
    private fun validateInputs(title: String): Boolean {
        return if (startDate.isEmpty() || endDate.isEmpty() || title.isEmpty()) {
            showCustomToast("Please enter title and select start & end times")
            false
        } else true
    }

    /**
     * Shows toast for invalid date/time input.
     */
    private fun showInvalidDateToast() {
        showCustomToast("Invalid date/time format")
    }

    /**
     * Opens a DatePicker dialog and then shows a TimePicker.
     */
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

    /**
     * Opens a TimePicker dialog for the selected date.
     */
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

    /**
     * Converts date string to RFC 3339 format for Google Calendar.
     */
    private fun String.toRFC3339(): String? {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-M-d HH:mm", Locale.getDefault())
            val date = inputFormat.parse(this)
            val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            outputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
            date?.let { outputFormat.format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Shows a styled toast with given message.
     */
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

    /**
     * Handles the result of event creation.
     */
    private fun handleEventResult(success: Boolean, response: String?) {
        activity?.runOnUiThread {
            showCustomToast(
                if (success) "Event created successfully!" else "Failed to create event."
            )
            if (!success) {
                Log.e("HomeFragment", "Create event failed: $response")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

