package com.mckcieply.datemate.ui.list

import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.mckcieply.datemate.CalendarEventModel
import com.mckcieply.datemate.GoogleAPIManager
import com.mckcieply.datemate.databinding.FragmentDashboardBinding
import com.mckcieply.datemate.ToastHelper.showCustomToast
import org.json.JSONArray
import org.json.JSONObject

class ListFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.linearLayoutContainer.removeAllViews()

        val accessToken = GoogleAPIManager.getAccessToken()

        if (accessToken != null) {
            GoogleAPIManager.fetchCalendarEvent(requireContext()) { response ->
                try {
                    val events = mutableListOf<CalendarEventModel>()
                    val json = JSONObject(response)
                    val filteredEventsString = json.getString("filteredEvents")
                    val items = JSONArray(filteredEventsString)

                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        val eventId = item.getString("id")
                        val summary = item.optString("summary", "No Title")
                        val start = item.getJSONObject("start")
                        val startTime = start.optString("dateTime", start.optString("date", "Unknown start time"))
                        val location = item.optString("location", null)
                        val description = item.optString("description", null)

                        events.add(CalendarEventModel(eventId, summary, startTime, location, description))
                    }

                    // Update UI
                    activity?.runOnUiThread {
                        binding.linearLayoutContainer.removeAllViews()
                        for (event in events) {
                            val eventLayout = LinearLayout(requireContext()).apply {
                                orientation = LinearLayout.HORIZONTAL
                                setPadding(8, 8, 8, 8)
                            }

                            val formattedText = """
                                <b><big>🔹 ${event.summary}</big></b><br>
                                🕒 ${event.startTime}<br>
                                📝 ${event.description ?: "No ideas"}
                            """.trimIndent()

                            val textView = TextView(requireContext()).apply {
                                text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)
                                textSize = 16f
                                setPadding(16, 16, 16, 16)
                                setTextColor(resources.getColor(android.R.color.black, null))
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                            }

                            val removeButton = TextView(requireContext()).apply {
                                text = "❌"
                                textSize = 18f
                                setPadding(16, 0, 16, 0)
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                setOnClickListener {
                                    GoogleAPIManager.deleteCalendarEvent(event.eventId) { success ->
                                        if (success) {
                                            activity?.runOnUiThread {
                                                binding.linearLayoutContainer.removeView(eventLayout)
                                            }
                                            showCustomToast(requireContext(), "Event deleted successfully")
                                            Log.d("DashboardFragment", "Successfully deleted: ${event.summary}")
                                        } else {
                                            Log.e("DashboardFragment", "Failed to delete: ${event.summary}")
                                        }
                                    }
                                }
                            }

                            eventLayout.addView(textView)
                            eventLayout.addView(removeButton)
                            binding.linearLayoutContainer.addView(eventLayout)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DashboardFragment", "Failed to parse events", e)
                    showError("Failed to load events")
                }
            }
        } else {
            showError("No access token available")
        }
    }

    private fun showError(message: String) {
        showCustomToast(requireContext(), message)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
