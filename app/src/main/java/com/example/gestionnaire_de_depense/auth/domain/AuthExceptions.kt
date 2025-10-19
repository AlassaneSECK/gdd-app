package com.example.gestionnaire_de_depense.auth.domain

sealed class AuthException(message: String, cause: Throwable? = null) : Exception(message, cause)

class InvalidCredentialsException : AuthException("Identifiants invalides.")

class EmailAlreadyUsedException : AuthException("Adresse e-mail déjà utilisée.")

class InvalidAuthRequestException : AuthException("Adresse e-mail ou mot de passe invalide.")

class InvalidAuthTokenException(cause: Throwable? = null) :
    AuthException("Jeton d'authentification invalide.", cause)

class AuthNetworkException(cause: Throwable? = null) :
    AuthException("Service d'authentification indisponible pour le moment.", cause)
