package com.example.gestionnaire_de_depense.auth.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private const val AUTH_DATASTORE_NAME = "auth_preferences"

// Extension de contexte utilisée pour récupérer le DataStore dédié aux jetons d'authentification.
val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(
    name = AUTH_DATASTORE_NAME
)
