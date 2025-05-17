package com.mckcieply.datemate

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

import com.mckcieply.datemate.databinding.ActivityMainBinding

import android.util.Log
import com.google.android.gms.common.api.Scope

import kotlinx.coroutines.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder


private lateinit var googleSignInClient: GoogleSignInClient


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        val navView: BottomNavigationView = binding.navView
//
//        val navController = findNavController(R.id.nav_host_fragment_activity_main)
//        val appBarConfiguration = AppBarConfiguration(
//            setOf(
//                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
//            )
//        )
//        setupActionBarWithNavController(navController, appBarConfiguration)
//        navView.setupWithNavController(navController)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/calendar.readonly"))
            .requestServerAuthCode(getString(R.string.web_client_id), false)
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Trigger sign-in
        startActivityForResult(googleSignInClient.signInIntent, 100)


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        if (requestCode == 100) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val authCode = account.serverAuthCode

                if (authCode != null) {
                    // Exchange auth code for tokens here:
                    exchangeAuthCodeForTokens(
                        authCode = authCode,
                        clientId = getString(R.string.web_client_id),
                        clientSecret = getString(R.string.web_client_secret)
                    )
                } else {
                    Log.e("Auth", "No auth code received")
                }
            } catch (e: ApiException) {
                Log.w("Auth", "signInResult:failed code=" + e.statusCode)
            }
        }

    }

    fun exchangeAuthCodeForTokens(authCode: String, clientId: String, clientSecret: String, redirectUri: String = "") {
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
                    // TODO: parse response JSON to get access_token, refresh_token, expires_in
                    println("Token response: $response")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                }
            }
        }
    }



}