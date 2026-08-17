package com.neet.app.data

import com.neet.app.data.auth.SecureTokenStore
import com.neet.app.data.model.AuthResponse
import com.neet.app.data.model.LoginRequest
import com.neet.app.data.model.SignupRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

sealed interface AuthResult {
    data class Success(val userId: String) : AuthResult
    data class Failure(val message: String) : AuthResult
}

class AuthRepository(
    private val authApiService: AuthApiService,
    private val tokenStore: SecureTokenStore,
    private val clearLocalData: suspend () -> Unit = {},
) {
    val isLoggedIn: Flow<Boolean> = tokenStore.tokenFlow().map { it != null }
    val accountEmail: Flow<String?> = tokenStore.emailFlow()

    suspend fun signup(email: String, password: String): AuthResult =
        runAuthCall(email) { authApiService.signup(SignupRequest(email, password)) }

    suspend fun login(email: String, password: String): AuthResult =
        runAuthCall(email) { authApiService.login(LoginRequest(email, password)) }

    suspend fun logout() {
        tokenStore.clear()
    }

    /** Permanently removes the authenticated account and its cloud-synced learning data. */
    suspend fun deleteAccount(): AuthResult = try {
        authApiService.deleteAccount()
        tokenStore.clear()
        withContext(Dispatchers.IO) { clearLocalData() }
        AuthResult.Success("")
    } catch (e: IOException) {
        AuthResult.Failure("Couldn't reach the server. Check your connection and try again.")
    } catch (e: retrofit2.HttpException) {
        AuthResult.Failure(
            if (e.code() == 401) "Your session has expired. Please sign in again."
            else "Couldn't delete your account. Please try again.",
        )
    }

    private suspend inline fun runAuthCall(email: String, call: () -> AuthResponse): AuthResult {
        return try {
            val response = call()
            tokenStore.saveToken(response.token)
            tokenStore.saveEmail(email)
            AuthResult.Success(response.userId)
        } catch (e: IOException) {
            AuthResult.Failure("Couldn't reach the server. Check your connection and try again.")
        } catch (e: retrofit2.HttpException) {
            val message = when (e.code()) {
                409 -> "An account with this email already exists."
                401 -> "Invalid email or password."
                else -> "Something went wrong. Please try again."
            }
            AuthResult.Failure(message)
        }
    }
}
