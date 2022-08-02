package com.example.audiorecorder.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.room.Room
import com.example.audiorecorder.databinding.ActivityDetailsBinding
import com.example.audiorecorder.db.AppDatabase
import com.example.audiorecorder.utils.Constants
import java.text.DateFormat

class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding
    private val appDB: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, Constants.APP_DATABASE)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
    }

    /* Audio record variables */
    private var id = 0
    private var  filename = ""
    private var timestamp = 0L
    private var duration = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /* --- Enable Toolbar --- */
        setSupportActionBar(binding.mtToolbarDetails)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.mtToolbarDetails.setNavigationOnClickListener {
            onBackPressed()
        }

        /* This intent is sent from btnInfo inside ActivityGallery */
        intent.extras?.let {
            id = it.getInt(Constants.BUNDLE_AUDIO_RECORD_ID)
        }

        /* Extracting data from database */
        filename = appDB.audioRecordDao().getAll().find { it.id == id }!!.filename
        timestamp = appDB.audioRecordDao().getAll().find { it.id == id }!!.timestamp
        duration = appDB.audioRecordDao().getAll().find { it.id == id }!!.duration

        /* Setting date format */
        val date = DateFormat.getDateTimeInstance().format(timestamp)

        binding.apply {
            tvFile.text = filename
            tvCreated.text = date
            tvDuration.text = duration
        }
    }
}

























