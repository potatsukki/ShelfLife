package com.example.data

data class AuthUiState(
    val isLoading: Boolean = false,
    val isFirebaseConfigured: Boolean = false,
    val isAuthenticated: Boolean = false,
    val userId: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val errorMessage: String? = null
) {
    val displayLabel: String
        get() = displayName?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "ShelfLife User"

    val initials: String
        get() = displayLabel.split(" ", ".", "_", "-")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "SL" }
}
