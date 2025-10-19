package com.example.gestionnaire_de_depense.auth.domain

import java.time.Duration
import java.time.Instant

// Représente une session d'authentification issue d'un JWT côté client.
data class AuthSession(
    val token: String,
    val subject: String,
    val issuedAt: Instant,
    val expiresAt: Instant
) {
    fun remainingDuration(now: Instant = Instant.now()): Duration {
        return Duration.between(now, expiresAt)
    }

    fun isExpired(now: Instant = Instant.now(), margin: Duration = Duration.ZERO): Boolean {
        val effectiveExpiry = expiresAt.minus(margin)
        return !now.isBefore(effectiveExpiry)
    }
}
