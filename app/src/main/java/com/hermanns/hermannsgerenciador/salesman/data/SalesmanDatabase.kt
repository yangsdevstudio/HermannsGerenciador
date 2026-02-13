package com.hermanns.hermannsgerenciador.salesman.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LocationEntity::class, ExpenseEntity::class], version = 22, exportSchema = false)
abstract class SalesmanDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: SalesmanDatabase? = null

        fun getDatabase(context: Context): SalesmanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SalesmanDatabase::class.java,
                    "location_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
