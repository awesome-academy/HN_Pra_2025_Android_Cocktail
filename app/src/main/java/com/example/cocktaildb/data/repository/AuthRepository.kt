package com.example.cocktaildb.data.repository

import android.util.Log
import com.example.cocktaildb.data.model.User
import com.example.cocktaildb.data.model.LoginMethod
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun loginWithEmail(email: String, password: String, onComplete: (FirebaseUser?, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onComplete(auth.currentUser, null)
            } else {
                onComplete(null, task.exception?.message)
            }
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
        auth.signOut()
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}
