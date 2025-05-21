package com.mckcieply.datemate

import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder


object GoogleAPIManager {
    private val client = OkHttpClient()
    fun exchangeAuthCodeForTokens(
        authCode: String,
        clientId: String,
        clientSecret: String,
        redirectUri: String = "",
        onResult: (accessToken: String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://oauth2.googleapis.com/token")
                val postData = mapOf(
                    "code" to authCode,
                    "client_id" to clientId,
                    "client_secret" to clientSecret,
                    "redirect_uri" to redirectUri,
                    "grant_type" to "authorization_code"
                ).map { (k, v) -> "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}" }
                    .joinToString("&")

                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.outputStream.use { it.write(postData.toByteArray()) }

                val response = conn.inputStream.bufferedReader().use { it.readText() }

                withContext(Dispatchers.Main) {
                    try {
                        val json = JSONObject(response)
                        val accessToken = json.getString("access_token")
                        val refreshToken = json.optString("refresh_token", "")
                        val expiresIn = json.getInt("expires_in")

                        Log.d("GoogleAuth", "✅ Access Token: $accessToken")
                        Log.d("GoogleAuth", "🔁 Refresh Token: $refreshToken")
                        Log.d("GoogleAuth", "⏳ Expires in: $expiresIn seconds")

                        onResult(accessToken)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onResult(null)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    onResult(null)
                }
            }
        }
    }

    fun fetchCalendarEvent(accessToken: String, onResult: (String) -> Unit) {

        val url = "https://www.googleapis.com/calendar/v3/calendars/primary/events" +
                "?orderBy=startTime" +
                "&singleEvents=true" +
                "&timeMin=${java.time.ZonedDateTime.now().toInstant()}" +
                "&maxResults=20"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        Log.d("CalendarAPI", "Starting request to fetch events")

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string() ?: "Empty response"
                    onResult(body)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("CalendarAPI", "Error fetching events", e)
                onResult("Error: ${e.message}")
            }
        })
    }

    fun createCalendarEvent(
        accessToken: String,
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

        client.newCall(request).enqueue(object : Callback {
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

