package com.mckcieply.datemate.ui.home

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mckcieply.datemate.GoogleAPIManager
import com.mckcieply.datemate.databinding.FragmentHomeBinding
import com.mckcieply.datemate.ToastHelper.showCustomToast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var selectedDate = ""

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

    private fun setupListeners() {
        binding.startTimeInput.setOnClickListener {
            showDatePickerDialog()
        }

        binding.submitButton.setOnClickListener {
            handleSubmit()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            selectedDate = format.format(calendar.time)
            binding.startTimeInput.setText(selectedDate)
        }

        DatePickerDialog(
            requireContext(),
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun handleSubmit() {
        val title = binding.titleInput.text.toString()
        var description = binding.descriptionInput.text.toString()
        var notifications = binding.notificationsSwitch.isChecked
        val location = ""

        if (description.isEmpty()) {
            val idea = GiftIdeas().getRandomGiftIdea()
            description = "Gift Idea: $idea"
        }

        if (!validateInputs(title)) return

        val endDate = selectedDate.addOneDay()

        GoogleAPIManager.createAllDayCalendarEvent(
            title = title,
            description = description,
            location = location,
            startDate = selectedDate,
            endDate = endDate,
            notifications = notifications,
            callback = ::handleEventResult
        )
    }

    private fun validateInputs(title: String): Boolean {
        return if (selectedDate.isEmpty() || title.isEmpty()) {
            showCustomToast(requireContext(), "Please enter a title and select a date")
            false
        } else true
    }

    private fun handleEventResult(success: Boolean, response: String?) {
        activity?.runOnUiThread {
            showCustomToast(requireContext(),
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

    private fun String.addOneDay(): String {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = format.parse(this)
            val calendar = Calendar.getInstance().apply {
                time = date!!
                add(Calendar.DATE, 1)
            }
            format.format(calendar.time)
        } catch (e: Exception) {
            this
        }
    }
}
