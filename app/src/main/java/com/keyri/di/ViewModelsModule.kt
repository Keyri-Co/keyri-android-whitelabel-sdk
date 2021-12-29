package com.keyri.di

import com.keyri.accounts.AccountsVM
import com.keyri.accounts.NewAccountVM
import com.keyri.auth.AuthVM
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module that provides ViewModels.
 */
val viewModelsModule = module {
    viewModel { AccountsVM(get(), get()) }
    viewModel { NewAccountVM(get(), get()) }
    viewModel { AuthVM(get(), get()) }
}
