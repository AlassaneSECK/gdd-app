package com.example.gestionnaire_de_depense.budget.domain

open class BudgetException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

class BudgetUnauthorizedException(cause: Throwable? = null) : BudgetException(cause = cause)

class BudgetValidationException(message: String? = null) : BudgetException(message)

class BudgetNotFoundException : BudgetException()

class BudgetNetworkException(cause: Throwable) : BudgetException(cause = cause)

class BudgetUnexpectedResponseException(message: String? = null, cause: Throwable? = null) :
    BudgetException(message, cause)

class BudgetAuthenticationRequiredException : BudgetException()
