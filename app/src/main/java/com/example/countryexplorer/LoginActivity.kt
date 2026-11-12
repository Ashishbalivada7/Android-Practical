package com.example.countryexplorer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnSignUp: Button
    private lateinit var btnGoogleLogin: Button
    private lateinit var btnGuestLogin: Button
    private lateinit var progressBar: ProgressBar

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val idToken = account.idToken
                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken)
                } else {
                    showLoading(false)
                    Toast.makeText(this, "Google Sign-In failed: ID token is null", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                showLoading(false)
                val detailMsg = when (e.statusCode) {
                    10 -> "Developer error (Code 10). Ensure SHA-1 fingerprint is added in Firebase Console and google-services.json is updated."
                    12500 -> "Sign-in failed (Code 12500). Ensure Google Sign-In is enabled in Firebase Console."
                    12501 -> "Sign-in cancelled."
                    else -> e.localizedMessage ?: "Status Code: ${e.statusCode}"
                }
                Toast.makeText(this, "Google Sign-In Error: $detailMsg", Toast.LENGTH_LONG).show()
            }
        } else {
            showLoading(false)
            Toast.makeText(this, "Google Sign-In cancelled or failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            navigateToMain()
            return
        }

        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnSignUp = findViewById(R.id.btnSignUp)
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin)
        btnGuestLogin = findViewById(R.id.btnGuestLogin)
        progressBar = findViewById(R.id.progressBarLogin)

        setupGoogleSignIn()

        btnLogin.setOnClickListener { loginUser() }
        btnSignUp.setOnClickListener { signUpUser() }
        btnGoogleLogin.setOnClickListener { signInWithGoogle() }
        btnGuestLogin.setOnClickListener { guestLogin() }
    }

    private fun setupGoogleSignIn() {
        val webClientId = getWebClientId()
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()

        if (webClientId.isNotEmpty()) {
            gsoBuilder.requestIdToken(webClientId)
        }

        googleSignInClient = GoogleSignIn.getClient(this, gsoBuilder.build())
    }

    private fun getWebClientId(): String {
        return try {
            val resId = resources.getIdentifier("default_web_client_id", "string", packageName)
            if (resId != 0) getString(resId) else ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun signInWithGoogle() {
        val webClientId = getWebClientId()
        if (webClientId.isEmpty()) {
            Toast.makeText(
                this,
                "Google Web Client ID missing in google-services.json. Please add SHA-1 in Firebase Console & re-download google-services.json.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        showLoading(true)
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showLoading(true)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, "Google Sign-In Successful!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    val errorMsg = task.exception?.localizedMessage ?: "Unknown authentication error"
                    Toast.makeText(this, "Authentication failed: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    val errorMsg = task.exception?.localizedMessage ?: "Unknown authentication error"
                    Toast.makeText(this, "Authentication failed: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun signUpUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    val errorMsg = task.exception?.localizedMessage ?: "Unknown error"
                    Toast.makeText(this, "Sign up failed: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun guestLogin() {
        showLoading(true)
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, "Logged in as Guest", Toast.LENGTH_SHORT).show()
                    navigateToMain(isGuest = true)
                } else {
                    Toast.makeText(this, "Continuing as Guest", Toast.LENGTH_SHORT).show()
                    navigateToMain(isGuest = true)
                }
            }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !isLoading
        btnSignUp.isEnabled = !isLoading
        btnGoogleLogin.isEnabled = !isLoading
        btnGuestLogin.isEnabled = !isLoading
    }

    private fun navigateToMain(isGuest: Boolean = false) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("is_guest", isGuest)
        }
        startActivity(intent)
        finish()
    }
}
