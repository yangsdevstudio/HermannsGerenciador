package com.hermanns.hermannsgerenciador.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermanns.hermannsgerenciador.data.Medication
import com.hermanns.hermannsgerenciador.repo.SheetsApiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ValidadeViewModel(
    private val repository: SheetsApiRepository
) : ViewModel() {

    private val _medications = MutableStateFlow<List<Medication>>(emptyList())
    val medications: StateFlow<List<Medication>> = _medications.asStateFlow()

    private val _labs = MutableStateFlow<List<String>>(listOf("Todos"))
    val labs: StateFlow<List<String>> = _labs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadFromCache()
    }

    fun refreshData(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = repository.fetchAllAndCache(
                spreadsheetId = com.hermanns.hermannsgerenciador.BuildConfig.SPREADSHEET_ID,
                apiKey = com.hermanns.hermannsgerenciador.BuildConfig.SHEETS_API_KEY
            )

            if (result.isSuccess) {
                val meds = result.getOrNull() ?: emptyList()
                _medications.value = meds
                _labs.value = listOf("Todos") + meds.map { it.lab }.distinct().sorted()
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Erro ao atualizar"
                _error.value = msg
            }
            _isLoading.value = false
        }
    }

    private fun loadFromCache() {
        viewModelScope.launch {
            val cached = repository.loadFromCache()
            _medications.value = cached
            _labs.value = listOf("Todos") + cached.map { it.lab }.distinct().sorted()
        }
    }
}