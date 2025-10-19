package com.example.gestionnaire_de_depense.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// Écran d'accueil minimaliste pour confirmer la navigation après l'authentification.
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    sessionEmail: String? = null,
    sessionExpiresAt: Instant? = null,
    onLogout: (() -> Unit)? = null
) {
    val formatter = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM) }
    val zoneId = remember { ZoneId.systemDefault() }
    val expirationText = remember(sessionExpiresAt, formatter, zoneId) {
        sessionExpiresAt?.let { formatter.withLocale(Locale.getDefault()).withZone(zoneId).format(it) }
    }

    var remainingText by remember(sessionExpiresAt) { mutableStateOf<String?>(null) }

    LaunchedEffect(sessionExpiresAt) {
        val target = sessionExpiresAt
        if (target == null) {
            remainingText = null
        } else {
            while (true) {
                val remaining = Duration.between(Instant.now(), target)
                val normalized = if (remaining.isNegative) Duration.ZERO else remaining
                remainingText = formatRemaining(normalized)

                if (normalized.isZero) {
                    break
                }
                delay(1_000)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bienvenue !",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Votre authentification est réussie.",
            style = MaterialTheme.typography.bodyLarge
        )
        sessionEmail?.let { email ->
            Text(
                text = "Connecté en tant que $email",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        expirationText?.let { formatted ->
            Text(
                text = "Expiration du jeton : $formatted",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        remainingText?.let { remaining ->
            Text(
                text = "Temps restant avant expiration : $remaining",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        onLogout?.let { logout ->
            Button(onClick = logout) {
                Text("Se déconnecter")
            }
        }
    }
}

private fun formatRemaining(duration: Duration): String {
    if (duration.isZero) {
        return "expiré"
    }
    val totalSeconds = duration.seconds
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 -> String.format(Locale.getDefault(), "%dh %02dm %02ds", hours, minutes, seconds)
        minutes > 0 -> String.format(Locale.getDefault(), "%dm %02ds", minutes, seconds)
        else -> String.format(Locale.getDefault(), "%ds", seconds)
    }
}
