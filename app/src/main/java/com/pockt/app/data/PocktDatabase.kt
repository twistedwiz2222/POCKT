package com.pockt.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PocktDao {
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM transactions WHERE fingerprint = :fingerprint")
    suspend fun fingerprintCount(fingerprint: String): Int

    @Query("SELECT * FROM preferences WHERE `key` = :key LIMIT 1")
    suspend fun preference(key: String): PreferenceEntity?

    @Query("SELECT * FROM preferences")
    fun observePreferences(): Flow<List<PreferenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreference(preference: PreferenceEntity)

    @Query("DELETE FROM transactions") suspend fun deleteAllTransactions()
    @Query("DELETE FROM preferences") suspend fun deleteAllPreferences()
}

@Database(entities = [TransactionEntity::class, PreferenceEntity::class], version = 1, exportSchema = false)
abstract class PocktDatabase : RoomDatabase() {
    abstract fun dao(): PocktDao

    companion object {
        @Volatile private var instance: PocktDatabase? = null
        fun get(context: Context): PocktDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, PocktDatabase::class.java, "pockt.db")
                .fallbackToDestructiveMigration(false)
                .build()
                .also { instance = it }
        }
    }
}
