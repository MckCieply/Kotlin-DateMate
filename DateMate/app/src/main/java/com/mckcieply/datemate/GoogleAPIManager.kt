package com.mckcieply.datemate

import android.content.Context
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate

object GoogleAPIManager {

    private var accessToken: String? = null

    fun setAccessToken(token: String) {
        accessToken = token
        // Optional: persist token securely (EncryptedSharedPreferences)
    }

    fun getAccessToken(): String? {
        return accessToken
    }

    fun exchangeAuthCodeForTokens(
        authCode: String,
        clientId: String,
        clientSecret: String,
        onResult: (String?) -> Unit
    ) {
        val requestBody = FormBody.Builder()
            .add("code", authCode)
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("redirect_uri", "") // Empty for installed apps
            .add("grant_type", "authorization_code")
            .build()

        val request = Request.Builder()
            .url("https://oauth2.googleapis.com/token")
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("GoogleAPIManager", "Token exchange failed", e)
                onResult(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e("GoogleAPIManager", "Token exchange error: ${response.code}")
                        onResult(null)
                        return
                    }

                    val responseBody = response.body?.string()
                    val json = JSONObject(responseBody ?: "")
                    val token = json.optString("access_token", null)
                    Log.d("GoogleAPIManager", "Token JSON: $json")
                    onResult(token)
                }
            }
        })
    }

    fun fetchCalendarEvent(context: Context, onResult: (String) -> Unit) {
        val request = Request.Builder()
            .url("https://www.googleapis.com/calendar/v3/calendars/primary/events")
            .addHeader("Authorization", "Bearer ${accessToken}")
            .get()
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("GoogleAPIManager", "Calendar fetch failed", e)
                onResult("Error fetching events")
            }

            override fun onResponse(call: Call, response: Response) {
                val result = response.body?.string() ?: "Empty response"
                val filteredResult = filterDateMateEvents(context, result)
                onResult(filteredResult)
            }
        })
    }
    fun createCalendarEvent(
        title: String,
        description: String?,
        location: String?,
        startDateTime: String,
        endDateTime: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val url = "https://www.googleapis.com/calendar/v3/calendars/primary/events"

        val eventJson = JSONObject().apply {
            put("summary", "DateMate: $title")
            put("description", description ?: "")
            put("location", location ?: "")
            put("start", JSONObject().apply {
                put("dateTime", startDateTime)
                put("timeZone", "UTC")
            })
            put("end", JSONObject().apply {
                put("dateTime", endDateTime)
                put("timeZone", "UTC")
            })
        }

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            eventJson.toString()
        )

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        callback(true, it.body?.string())
                    } else {
                        callback(false, it.body?.string())
                    }
                }
            }
        })
    }

    fun createAllDayCalendarEvent(
        title: String,
        description: String,
        location: String,
        startDate: String,
        endDate: String,
        notifications: Boolean = false,
        callback: (Boolean, String?) -> Unit
    ) {
        val url = "https://www.googleapis.com/calendar/v3/calendars/primary/events"

        val eventJson = JSONObject().apply {
            put("summary", "DateMate: $title")
            put("description", description)
            put("location", location)
            put("start", JSONObject().apply {
                put("date", startDate) // All-day event date format
            })
            put("end", JSONObject().apply {
                put("date", endDate) // Exclusive end date
            })

                put("reminders", JSONObject().apply {
                    put("useDefault", false) // disable default reminders
                    if(notifications) {
                        put("overrides", JSONArray().apply {
                            put(JSONObject().apply {
                                put("method", "popup")
                                put("minutes", 1 * 1440) // Day before
                            })
                            put(JSONObject().apply {
                                put("method", "popup")
                                put("minutes", 6 * 1440) // 7 days before
                            })
                        })
                    }
                })

        }

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            eventJson.toString()
        )

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        callback(true, it.body?.string())
                    } else {
                        callback(false, it.body?.string())
                    }
                }
            }
        })
    }

    private fun filterDateMateEvents(context: Context, jsonResponse: String): String {
        return try {
            val json = JSONObject(jsonResponse)
            val items = json.getJSONArray("items")
            val upcomingEvents = mutableListOf<Pair<LocalDate, JSONObject>>()

            val today = LocalDate.now()

            for (i in 0 until items.length()) {
                val event = items.getJSONObject(i)
                val title = event.optString("summary", "")

                if (title.startsWith("DateMate:")) {
                    val startObj = event.optJSONObject("start")
                    val dateStr = startObj?.optString("date") ?: startObj?.optString("dateTime")?.substring(0, 10)

                    try {
                        val eventDate = LocalDate.parse(dateStr)
                        if (!eventDate.isBefore(today)) {
                            notifyIfEventIsTomorrow(context, dateStr.toString(), title)
                            upcomingEvents.add(eventDate to event)
                        }
                    } catch (e: Exception) {
                        Log.e("GoogleAPIManager", "Invalid event date: $dateStr", e)
                    }
                }
            }

            // Sort by event date
            val sortedEvents = upcomingEvents.sortedBy { it.first }.map { it.second }

            val resultJson = JSONObject().apply {
                put("filteredEvents", JSONArray().apply {
                    sortedEvents.forEach { put(it) }
                })
            }

            resultJson.toString()
        } catch (e: Exception) {
            Log.e("GoogleAPIManager", "Error filtering events", e)
            "{\"error\": \"Failed to parse or filter events\"}"
        }
    }

    fun deleteCalendarEvent(eventId: String, callback: (Boolean) -> Unit) {
        val accessToken = getAccessToken()
        if (accessToken == null) {
            callback(false)
            return
        }

        val url = "https://www.googleapis.com/calendar/v3/calendars/primary/events/$eventId"

        val request = Request.Builder()
            .url(url)
            .delete()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("GoogleAPIManager", "Failed to delete event", e)
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    callback(response.isSuccessful)
                }
            }
        })
    }

    fun notifyIfEventIsTomorrow(context: Context, dateString: String, title: String) {
        try {
            val eventDate = LocalDate.parse(dateString)
            val tomorrow = LocalDate.now().plusDays(1)

            if (eventDate == tomorrow) {
                val helper = NotificationHelper(context)
                helper.showNotification("Upcoming DateMate: ${title.removePrefix("DateMate:").trim()}", "Have you forgotten?")
            }
        } catch (e: Exception) {
            Log.e("DateMate", "Invalid date: $dateString, error: $e")
        }
    }

}
