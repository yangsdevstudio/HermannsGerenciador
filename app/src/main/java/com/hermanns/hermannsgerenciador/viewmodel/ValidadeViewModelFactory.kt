package com.hermanns.hermannsgerenciador.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hermanns.hermannsgerenciador.repo.SheetsApiRepository

class ValidadeViewModelFactory(
    private val repository: SheetsApiRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ValidadeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ValidadeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}