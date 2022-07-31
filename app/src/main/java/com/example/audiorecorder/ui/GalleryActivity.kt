package com.example.audiorecorder.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.audiorecorder.R
import com.example.audiorecorder.adapter.AudioAdapter
import com.example.audiorecorder.adapter.OnItemClickListener
import com.example.audiorecorder.databinding.ActivityGalleryBinding
import com.example.audiorecorder.db.AppDatabase
import com.example.audiorecorder.db.AudioRecord
import com.example.audiorecorder.utils.Constants
import com.example.audiorecorder.utils.Constants.BUNDLE_AUDIO_RECORD_ID
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class GalleryActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var binding: ActivityGalleryBinding
    private val audioAdapter by lazy { AudioAdapter(this) }
    private val appDB: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, Constants.APP_DATABASE)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
    }

    /* deletedRecording and oldRecordingList are used when the user uses the UNDO option after
       deleting a row in the recycler view */
    private lateinit var deletedRecording: AudioRecord
    private lateinit var oldRecordingList: List<AudioRecord>
    private lateinit var deletedCachedRecording: File

    /* This variable is responsible for the check box inside the recycle view */
    private var allChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkItem()

        /* --- Enable Toolbar --- */
        setSupportActionBar(binding.mtToolbarGallery)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.mtToolbarGallery.setNavigationOnClickListener {
            onBackPressed()
        }

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

        /* Hides keyboard on clicking on the tlRecordings (CollapsingToolbarLayout) layout */
        binding.tlRecordings.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }

        /* Enables swiping and deletes a recording on swipe */
        val itemTouchHelper = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                deleteRecording(audioAdapter.differ.currentList[viewHolder.adapterPosition])
            }
        }

        /* Enables swiping */
        val swipeHelper = ItemTouchHelper(itemTouchHelper)
        swipeHelper.attachToRecyclerView(binding.rvGallary)


        binding.apply {
            binding.btnClose.setOnClickListener {
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.setDisplayShowHomeEnabled(true)
                binding.editBar.visibility = View.GONE
                audioAdapter.differ.currentList.map {
                    it.isChecked = false
                }
                audioAdapter.setEditMode(false)
                setupRecyclerView()
            }
        }

        binding.btnSelectAll.setOnClickListener {
            allChecked = !allChecked
            audioAdapter.differ.currentList.map {
                it.isChecked = allChecked
            }
            setupRecyclerView()
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

    /* Opens AudioPlayerActivity or if EditMode is on it will either check or uncheck the curr row */
    override fun onItemClickListener(position: Int) {
        var audioRecord = audioAdapter.differ.currentList[position]

        if (audioAdapter.isEditMode()) {
            audioAdapter.differ.currentList[position].isChecked =
                !audioAdapter.differ.currentList[position].isChecked
            setupRecyclerView()
        } else {
            val intent = Intent(this, AudioPlayerActivity::class.java)
            intent.putExtra(BUNDLE_AUDIO_RECORD_ID, audioRecord.id)
            startActivity(intent)
        }
    }

    /*  */
    override fun onItemLongClickListener(position: Int) {
        audioAdapter.setEditMode(true)
        audioAdapter.differ.currentList[position].isChecked =
            !audioAdapter.differ.currentList[position].isChecked

        if (audioAdapter.isEditMode() && binding.editBar.visibility == View.GONE) {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            supportActionBar?.setDisplayShowHomeEnabled(false)

            binding.editBar.visibility = View.VISIBLE
        }
        setupRecyclerView()
    }

    private fun deleteRecording(record: AudioRecord) {
        deletedRecording = record
        oldRecordingList = audioAdapter.differ.currentList
        appDB.audioRecordDao().delete(record)
        val filename = deletedRecording.filename
        val dirPath = "${externalCacheDir?.absolutePath}/"
        val filePath = "$dirPath$filename.mp3"
        val file = File(filePath)

        //Creates a new list of audio recordings excluding the deleted recording
        audioAdapter.differ.submitList(appDB.audioRecordDao().getAll().filter { it.id != record.id })
        showSnackbar()

        if (file.exists()) {
            File("$dirPath$filename.mp3").delete()
        }
    }

    // Snackbar is shown temporarily after a row is deleted and provides an UNDO option
    private fun showSnackbar(){
        val snackbar = Snackbar.make(binding.rootView,
            getString(R.string.deleted_transaction), Snackbar.LENGTH_LONG)
        snackbar.setAction(getString(R.string.undo)) {
            //undoDelete()
        }
            .setActionTextColor(ContextCompat.getColor(this, R.color.red))
            .setTextColor(ContextCompat.getColor(this, R.color.white))
            .show()
    }

    /* Undoes delete by inserting the deleted recording back and giving a the old list
    *  to the adapter */
    private fun undoDelete(){
        appDB.audioRecordDao().insert(deletedRecording)
        audioAdapter.differ.submitList(oldRecordingList)
        runOnUiThread {
            setupRecyclerView()
        }
    }
}













