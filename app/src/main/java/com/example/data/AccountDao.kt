package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM daledou_accounts ORDER BY id DESC")
    fun getAllAccountsFlow(): Flow<List<DaledouAccount>>

    @Query("SELECT * FROM daledou_accounts")
    suspend fun getAllAccounts(): List<DaledouAccount>

    @Query("SELECT * FROM daledou_accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Int): DaledouAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: DaledouAccount)

    @Update
    suspend fun updateAccount(account: DaledouAccount)

    @Delete
    suspend fun deleteAccount(account: DaledouAccount)
}
