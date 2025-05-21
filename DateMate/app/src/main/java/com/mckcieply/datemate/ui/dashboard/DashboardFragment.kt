package com.mckcieply.datemate.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.mckcieply.datemate.GoogleAPIManager
import com.mckcieply.datemate.MainActivity
import com.mckcieply.datemate.databinding.FragmentDashboardBinding
import org.json.JSONObject

/**
 * Simple Fragment displaying Google Calendar events as a list of TextViews added dynamically.
 */
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

        // Clear any existing views
        binding.linearLayoutContainer.removeAllViews()

        // Get the access token from MainActivity
        val accessToken = (activity as MainActivity).accessToken

        if (accessToken != null) {
            GoogleAPIManager.fetchCalendarEvent(accessToken) { response ->
                try {
                    val json = JSONObject(response)
                    val items = json.getJSONArray("items")

                    // Update UI on main thread
                    activity?.runOnUiThread {
                        binding.linearLayoutContainer.removeAllViews() // Clear again before adding
                        for (i in 0 until items.length()) {
                            val event = items.getJSONObject(i)
                            val summary = event.optString("summary", "No Title")

                            val textView = TextView(requireContext()).apply {
                                text = summary
                                textSize = 18f
                                setPadding(16, 16, 16, 16)
                            }
                            binding.linearLayoutContainer.addView(textView)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DashboardFragment", "Failed to parse events", e)
                    activity?.runOnUiThread {
                        binding.linearLayoutContainer.removeAllViews()
                        val errorView = TextView(requireContext()).apply {
                            text = "Failed to load events"
                            textSize = 18f
                            setPadding(16, 16, 16, 16)
                        }
                        binding.linearLayoutContainer.addView(errorView)
                    }
                }
            }
        } else {
            // No token: show message
            binding.linearLayoutContainer.removeAllViews()
            val noTokenView = TextView(requireContext()).apply {
                text = "No access token available"
                textSize = 18f
                setPadding(16, 16, 16, 16)
            }
            binding.linearLayoutContainer.addView(noTokenView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
