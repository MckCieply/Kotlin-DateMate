package com.mckcieply.datemate

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

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

    fun fetchCalendarEvent( onResult: (String) -> Unit) {
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
                Log.d("GoogleAPIManager", "Calendar API response: $result")
                onResult(result)
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
        startDate: String, // Format: "yyyy-MM-dd"
        endDate: String,   // Format: "yyyy-MM-dd" (exclusive end date)
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


}
