package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daledou_accounts")
data class DaledouAccount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val qq: String,
    val description: String,
    val cookieString: String,
    val isActive: Boolean = true
)
