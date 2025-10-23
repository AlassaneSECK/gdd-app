package com.example.gestionnaire_de_depense.di

import android.content.Context
import com.example.gestionnaire_de_depense.auth.data.AuthNetworkModule
import com.example.gestionnaire_de_depense.auth.data.AuthRepositoryImpl
import com.example.gestionnaire_de_depense.auth.data.authDataStore
import com.example.gestionnaire_de_depense.auth.domain.AuthRepository
import com.example.gestionnaire_de_depense.auth.presentation.AuthViewModel
import com.example.gestionnaire_de_depense.budget.data.BudgetRepositoryImpl
import com.example.gestionnaire_de_depense.budget.domain.BudgetRepository
import com.example.gestionnaire_de_depense.budget.network.BudgetNetworkModule
import com.example.gestionnaire_de_depense.budget.presentation.BudgetViewModel

class AppContainer(context: Context) {

    private val applicationContext = context.applicationContext

    private val authApi = AuthNetworkModule.createAuthApi()

    private val authDataStore = applicationContext.authDataStore

    val authRepository: AuthRepository = AuthRepositoryImpl(
        api = authApi,
        dataStore = authDataStore
    )

    private val budgetApi = BudgetNetworkModule.createBudgetApi()

    val budgetRepository: BudgetRepository = BudgetRepositoryImpl(
        api = budgetApi,
        authRepository = authRepository
    )

    val budgetViewModelFactory = BudgetViewModel.provideFactory(budgetRepository)

    val authViewModelFactory = AuthViewModel.provideFactory(authRepository)
}
