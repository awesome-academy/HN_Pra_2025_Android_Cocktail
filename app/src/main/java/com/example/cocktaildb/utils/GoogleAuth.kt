package com.example.cocktaildb.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.cocktaildb.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class GoogleAuth(private val context: Context) {

    companion object {
        const val RC_SIGN_IN = 9001
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun handleSignInResult(
        data: Intent?,
        onSuccess: (GoogleSignInAccount) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account, onSuccess, onFailure)
        } catch (e: Exception) {
            onFailure(e.localizedMessage ?: context.getString(R.string.msg_google_signin_failed))
        }
    }

    private fun firebaseAuthWithGoogle(
        account: GoogleSignInAccount,
        onSuccess: (GoogleSignInAccount) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(context as Activity) { task ->
            if (task.isSuccessful) {
                onSuccess(account)
            } else {
                onFailure(task.exception?.message ?: context.getString(R.string.msg_firebase_auth_failed))
            }
        }
    }

    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut()
    }
}
