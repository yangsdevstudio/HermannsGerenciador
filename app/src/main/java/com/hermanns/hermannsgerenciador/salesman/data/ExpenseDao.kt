package com.hermanns.hermannsgerenciador.salesman.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE isExported = 0")
    fun getUnexportedExpenses(): Flow<List<ExpenseEntity>>

    @Query("UPDATE expenses SET isExported = 1 WHERE isExported = 0")
    suspend fun markAllAsExported()

    @Query("DELETE FROM expenses WHERE isExported = 0")
    suspend fun deleteUnexportedExpenses()
}
