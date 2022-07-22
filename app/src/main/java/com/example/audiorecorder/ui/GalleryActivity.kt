package com.example.audiorecorder.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.audiorecorder.adapter.AudioAdapter
import com.example.audiorecorder.databinding.ActivityGalleryBinding
import com.example.audiorecorder.databinding.ActivityMainBinding
import com.example.audiorecorder.db.AppDatabase
import com.example.audiorecorder.utils.Constants

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private val audioAdapter by lazy { AudioAdapter() }
    private val appDB: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, Constants.APP_DATABASE)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkItem()
    }

    /* */
    private fun checkItem() {
        binding.apply {
            if (appDB.audioRecordDao().getAll().isNotEmpty()) {
                audioAdapter.differ.submitList(appDB.audioRecordDao().getAll())
                setupRecyclerView()
                //updateDashBoard()
            }
        }
    }

    /* */
    private fun setupRecyclerView() {
        binding.rvGallary.apply {
            layoutManager = LinearLayoutManager(this@GalleryActivity)
            adapter = audioAdapter
        }
    }
}