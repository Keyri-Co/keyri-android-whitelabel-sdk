package com.keyri.di

import com.keyri.accounts.AccountsVM
import com.keyri.accounts.NewAccountVM
import com.keyri.auth.AuthVM
import com.keyri.auth_with_scanner.AuthWithScannerVM
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module that provides ViewModels.
 */
val viewModelsModule = module {
    viewModel { AccountsVM(get(), get()) }
    viewModel { NewAccountVM(get(), get()) }
    viewModel { AuthVM(get(), get()) }
    viewModel { AuthWithScannerVM() }
}
