package com.mckcieply.datemate.ui.dashboard

import android.os.Bundle
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
import org.json.JSONArray
import org.json.JSONObject

class DashboardFragment : Fragment() {

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
            GoogleAPIManager.fetchCalendarEvent { response ->
                try {
                    val events = mutableListOf<CalendarEventModel>()
                    val json = JSONObject(response)
                    val filteredEventsString = json.getString("filteredEvents")
                    val items = JSONArray(filteredEventsString)

                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)

                        val summary = item.optString("summary", "No Title")
                        val start = item.getJSONObject("start")
                        val startTime = start.optString("dateTime", start.optString("date", "Unknown start time"))
                        val location = item.optString("location", null)
                        val description = item.optString("description", null)

                        events.add(CalendarEventModel(summary, startTime, location, description))
                    }

                    // Update UI
                    activity?.runOnUiThread {
                        binding.linearLayoutContainer.removeAllViews()
                        for (event in events) {
                            val eventLayout = LinearLayout(requireContext()).apply {
                                orientation = LinearLayout.HORIZONTAL
                                setPadding(8, 8, 8, 8)
                            }
                            val textView = TextView(requireContext()).apply {
                                text = """
                            🔹 ${event.summary}
                            🕒 ${event.startTime}
                            📝 ${event.description ?: "No ideas"}
                        """.trimIndent()
                                textSize = 16f
                                setPadding(16, 16, 16, 16)
                            }

                            val removeButton = TextView(requireContext()).apply {
                                text = "❌"
                                textSize = 18f
                                setPadding(16, 0, 0, 0)
                                setOnClickListener {
                                    binding.linearLayoutContainer.removeView(eventLayout)
                                    // TODO: Optionally remove from source (e.g., API, DB)
                                    Log.d("DashboardFragment", "Removed event: ${event.summary}")
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

        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
