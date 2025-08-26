package com.example.cocktaildb.data.repository

import android.content.Context
import android.util.Log
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.User
import com.example.cocktaildb.data.model.LoginMethod
import com.example.cocktaildb.utils.GoogleAuth
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

@Suppress("DEPRECATION")
class AuthRepository(private val context: Context? = null) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val googleAuth: GoogleAuth? = context?.let { GoogleAuth(it) }

    fun loginWithEmail(email: String, password: String, onComplete: (FirebaseUser?, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onComplete(auth.currentUser, null)
            } else {
                onComplete(null, task.exception?.message)
            }
        }
    }
    fun authenticateFirebaseWithGoogle(
        account: com.google.android.gms.auth.api.signin.GoogleSignInAccount,
        onSuccess: (com.google.android.gms.auth.api.signin.GoogleSignInAccount) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val googleAuth = this.googleAuth
        if (googleAuth != null) {
            val email = account.email
            if (email != null) {
                checkLoginMethodFromFirestore(email) { loginMethod, error ->
                    if (loginMethod == LoginMethod.EMAIL_PASSWORD) {
                        if (context != null) {
                            onFailure(context.getString(R.string.msg_email_exists_with_password))
                        }
                        return@checkLoginMethodFromFirestore
                    }
                    googleAuth.authenticateWithFirebase(account, onSuccess, onFailure)
                }
            } else {
                onFailure("Google account has no email")
            }
        } else {
            onFailure("GoogleAuth not initialized")
        }
    }

    fun signUpWithEmail(name: String, email: String, password: String, onComplete: (FirebaseUser?, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val firebaseUser = auth.currentUser
                saveUserToFirestore(firebaseUser, name, LoginMethod.EMAIL_PASSWORD) { success, error ->
                    if (success) {
                        onComplete(firebaseUser, null)
                    } else {
                        onComplete(null, "Lưu thông tin user thất bại: $error")
                    }
                }
            } else {
                onComplete(null, task.exception?.message)
            }
        }
    }

    fun saveUserToFirestore(
        firebaseUser: FirebaseUser?,
        name: String,
        loginMethod: LoginMethod = LoginMethod.EMAIL_PASSWORD,
        profileImage: String? = null,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val uid = firebaseUser?.uid
        if (uid == null) {
            onComplete?.invoke(false, "User ID is null")
            return
        }

        val email = firebaseUser.email ?: ""
        Log.d("AuthRepository", "Creating user with uid: $uid, name: $name, email: $email, loginMethod: ${loginMethod.value}")
        val user = User(
            uid = uid,
            name = name,
            email = email,
            loginMethod = loginMethod,
            profileImage = profileImage,
            createdAt = System.currentTimeMillis()
        )

        Log.d("AuthRepository", "Saving to Firestore: $user")
        firestore.collection("users").document(uid).set(user)
            .addOnSuccessListener {
                Log.d("AuthRepository", "User saved successfully to Firestore")
                onComplete?.invoke(true, null)
            }
            .addOnFailureListener { exception ->
                Log.e("AuthRepository", "Failed to save user to Firestore", exception)
                onComplete?.invoke(false, exception.message)
            }
    }

    fun checkLoginMethodFromFirestore(
        email: String,
        onComplete: (LoginMethod?, String?) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val usersCollection = db.collection("users")

        usersCollection
            .whereEqualTo("email", email.trim())
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        Log.d("AuthRepo", "Found user with email=$email, loginMethod=${user.loginMethod}")
                        onComplete(user.loginMethod, null)
                    } else {
                        Log.w("AuthRepo", "User object is null for email=$email")
                        onComplete(null, "User data not found")
                    }
                } else {
                    Log.d("AuthRepo", "No user found for email=$email")
                    onComplete(null, null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("AuthRepo", "Error checking login method for email=$email", exception)
                onComplete(null, exception.message ?: "Unknown error")
            }
    }



    fun sendPasswordResetEmail(email: String, onComplete: (Boolean, String?) -> Unit) {
        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onComplete(true, null)
            } else {
                onComplete(false, task.exception?.message)
            }
        }
    }

    fun signOut() {
        if (googleAuth != null) {
            googleAuth.revokeAccess()
        } else {
            auth.signOut()
        }
    }

    fun signOutGoogleOnly() {
        googleAuth?.signOutGoogleOnly()
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}
