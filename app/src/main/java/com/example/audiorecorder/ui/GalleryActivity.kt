package com.example.audiorecorder.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.audiorecorder.adapter.AudioAdapter
import com.example.audiorecorder.adapter.OnItemClickListener
import com.example.audiorecorder.databinding.ActivityGalleryBinding
import com.example.audiorecorder.databinding.ActivityMainBinding
import com.example.audiorecorder.db.AppDatabase
import com.example.audiorecorder.utils.Constants
import com.example.audiorecorder.utils.Constants.BUNDLE_AUDIO_RECORD_ID
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GalleryActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var binding: ActivityGalleryBinding
    private val audioAdapter by lazy { AudioAdapter(this) }
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

        /* Filtering the recycle view for a specific file */
        binding.etSearchInput.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                var query = s.toString()
                searchDataBase(query)
            }

            override fun afterTextChanged(s: Editable?) {
            }

        })

        binding.tlRecordings.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }

    }

    private fun searchDataBase(query: String) {
        GlobalScope.launch {
            if (appDB.audioRecordDao().getAll().isNotEmpty()) {
                audioAdapter.differ.submitList(appDB.audioRecordDao().
                searchDataBase("%$query%")) //%...% -> look for filenames that contain "query"

                runOnUiThread {
                    setupRecyclerView()
                }
            }
        }

    }

    /* Send data from database to recycler view */
    private fun checkItem() {
        GlobalScope.launch {
            binding.apply {
                if (appDB.audioRecordDao().getAll().isNotEmpty()) {
                    audioAdapter.differ.submitList(appDB.audioRecordDao().getAll())
                    setupRecyclerView()
                }
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

    /* Opens AudioPlayerActivity */
    override fun onItemClickListener(position: Int) {
        var audioRecord = audioAdapter.differ.currentList[position]
        val intent = Intent(this, AudioPlayerActivity::class.java)
        intent.putExtra(BUNDLE_AUDIO_RECORD_ID, audioRecord.id)
        startActivity(intent)
    }

    /* */
    override fun onItemLongClickListener(position: Int) {
        Toast.makeText(this@GalleryActivity, "Long click", Toast.LENGTH_SHORT).show()
    }

}