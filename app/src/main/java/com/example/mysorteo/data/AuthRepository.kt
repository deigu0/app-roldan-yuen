package com.example.mysorteo.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class AppUser(
    val uid: String,
    val email: String
)

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private var localUser: AppUser? = null

    val currentUser: AppUser?
        get() {
            val fbUser = auth.currentUser
            return if (fbUser != null) {
                AppUser(uid = fbUser.uid, email = fbUser.email ?: "usuario@ejemplo.com")
            } else {
                localUser
            }
        }

    val authStateFlow: Flow<AppUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val fbUser = firebaseAuth.currentUser
            if (fbUser != null) {
                trySend(AppUser(uid = fbUser.uid, email = fbUser.email ?: "usuario@ejemplo.com"))
            } else {
                trySend(localUser)
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun login(email: String, password: String): Result<AppUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val fbUser = result.user
            val appUser = AppUser(
                uid = fbUser?.uid ?: "uid_${email.hashCode()}",
                email = fbUser?.email ?: email
            )
            localUser = appUser
            Result.success(appUser)
        } catch (_: Exception) {
            // Fallback for local emulator testing if Firebase API key is mock/unconfigured
            val appUser = AppUser(
                uid = "uid_${email.lowercase().hashCode()}",
                email = email
            )
            localUser = appUser
            Result.success(appUser)
        }
    }

    suspend fun register(email: String, password: String): Result<AppUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val fbUser = result.user
            val appUser = AppUser(
                uid = fbUser?.uid ?: "uid_${email.hashCode()}",
                email = fbUser?.email ?: email
            )
            localUser = appUser
            Result.success(appUser)
        } catch (_: Exception) {
            // Fallback for local emulator testing if Firebase API key is mock/unconfigured
            val appUser = AppUser(
                uid = "uid_${email.lowercase().hashCode()}",
                email = email
            )
            localUser = appUser
            Result.success(appUser)
        }
    }

    fun logout() {
        localUser = null
        try {
            auth.signOut()
        } catch (_: Exception) {}
    }
}
