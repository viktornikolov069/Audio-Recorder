package com.example.audiorecorder.db

import androidx.room.*

@Dao
interface AudioRecordDao {
    @Query("SELECT * FROM audioRecords")
    fun getAll(): List<AudioRecord>

    @Query("SELECT * FROM audioRecords WHERE filename LIKE :query")
    fun searchDataBase(query: String): List<AudioRecord>

    @Insert
    fun insert(vararg audioRecord: AudioRecord)

    @Delete
    fun delete(audioRecord: AudioRecord)

    @Delete
    fun delete(audioRecords: Array<AudioRecord>)

    @Update
    fun update(audioRecord: AudioRecord)

}






























