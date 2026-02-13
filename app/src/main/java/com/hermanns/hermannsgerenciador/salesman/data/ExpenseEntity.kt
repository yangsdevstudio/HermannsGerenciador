package com.hermanns.hermannsgerenciador.salesman.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expenseType: String,
    val paymentType: String,
    val issuer: String,
    val issueDate: String,
    val description: String,
    val totalAmount: Double,
    val documentNumber: String,
    val documentPhotoPath: String?,
    val km: String?,
    val salesmanId: String,
    val timestamp: Long,
    val isExported: Boolean = false
)
