package com.mckcieply.datemate

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mckcieply.datemate.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    var accessToken: String? = null
    private lateinit var binding: ActivityMainBinding
    private lateinit var googleSignInClient: GoogleSignInClient

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleSignInResult(task)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Navigation setup
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_home, R.id.navigation_dashboard)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/calendar.events"))
            .requestServerAuthCode(getString(R.string.web_client_id), true)
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Launch sign-in flow
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val authCode = account.serverAuthCode
            Log.d("MainActivity", "Auth Code: $authCode")

            if (authCode != null) {
                GoogleAPIManager.exchangeAuthCodeForTokens(
                    authCode = authCode,
                    clientId = getString(R.string.web_client_id),
                    clientSecret = getString(R.string.web_client_secret)
                ) { token ->
                    if (token != null) {
                        accessToken = token
                        GoogleAPIManager.setAccessToken(token)
                        Log.d("MainActivity", "Access token received: $token")
                        GoogleAPIManager.fetchCalendarEvent() { result ->
                            Log.d("CalendarEvents", result)
                        }
                    } else {
                        Log.e("MainActivity", "Failed to retrieve access token")
                    }
                }
            } else {
                Log.e("MainActivity", "No auth code received")
            }
        } catch (e: ApiException) {
            Log.e("MainActivity", "Sign-in failed", e)
        }
    }
}
