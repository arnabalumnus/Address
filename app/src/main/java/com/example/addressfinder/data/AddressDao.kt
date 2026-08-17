package com.example.addressfinder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AddressDao {

    @Insert
    suspend fun insert(address: AddressEntity): Long

    @Query("SELECT * FROM saved_addresses ORDER BY timestamp DESC")
    fun getAllOrderedByTimestampDesc(): Flow<List<AddressEntity>>
}
