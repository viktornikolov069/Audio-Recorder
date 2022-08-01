package com.example.audiorecorder.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
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
import com.example.audiorecorder.utils.ListPositioner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class GalleryActivity : AppCompatActivity(), OnItemClickListener, ListPositioner {

    private lateinit var binding: ActivityGalleryBinding
    private val audioAdapter by lazy { AudioAdapter(this) }
    private val appDB: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, Constants.APP_DATABASE)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
    }

    /* Recycler View position */
    private var rvPosition = 0

    /* Bottom Sheet Rename variables */
    private lateinit var bottomSheetBehaviour: BottomSheetBehavior<LinearLayout>

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

        /* --- Initialise BottomSheetBehavior and Setup --- */

        bottomSheetBehaviour = BottomSheetBehavior.from(binding.bsRename.bottomSheetRename)
        bottomSheetBehaviour.peekHeight = 0 // This hides the bottom sheet
        bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED

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
                binding.mtToolbarGallery.visibility = View.VISIBLE
                saveListPosition()
                setupRecyclerView()
                loadListPosition()
            }

            btnSelectAll.setOnClickListener {
                allChecked = !allChecked
                audioAdapter.differ.currentList.map {
                    it.isChecked = allChecked
                }
                setupRecyclerView()
            }

            btnRename.isClickable = false
            btnRename.setOnClickListener {
                bottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
                viewBottomSheetBackGround.visibility = View.VISIBLE
                tiLayout.visibility = View.GONE
                val checked = audioAdapter.differ.currentList.filter { it.isChecked }
                bsRename.etFilenameInput.setText(checked[0].filename)
            }

            /* --- BottomSheetSave Buttons --- */
            bsRename.btnCancel.setOnClickListener {
                dismiss()
            }

            bsRename.btnOk.setOnClickListener {
                dismiss()
                val dirPath = "${externalCacheDir?.absolutePath}/"
                val checked = audioAdapter.differ.currentList.first { it.isChecked }
                val oldFilename = checked.filename
                checked.filename = bsRename.etFilenameInput.text.toString()


                val newFilename = checked.filename
                if (newFilename != oldFilename) {
                    var newFile = File("$dirPath$newFilename.mp3")
                    File(checked.filePath).renameTo(newFile)
                }
                checked.filePath = "$dirPath$newFilename.mp3"
                appDB.audioRecordDao().update(checked)
                audioAdapter.differ.submitList(appDB.audioRecordDao().getAll())
                setupRecyclerView()
            }

            viewBottomSheetBackGround.setOnClickListener {
                dismiss()
            }
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
            enableDisableBtnRename()
            saveListPosition()
            setupRecyclerView()
            loadListPosition()
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
            binding.mtToolbarGallery.visibility = View.GONE
            enableDisableBtnRename()
        }
        saveListPosition()
        setupRecyclerView()
        loadListPosition()
    }

    private fun enableDisableBtnRename() {
        val isCheckedCount = audioAdapter.differ.currentList.count { it.isChecked }
        if (isCheckedCount == 1) {
            binding.btnRename.setImageResource(R.drawable.ic_rename)
            binding.btnRename.isClickable = true
        } else {
            binding.btnRename.setImageResource(R.drawable.ic_rename_disabled)
            binding.btnRename.isClickable = false
        }
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

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun dismiss() {
        binding.apply {
            viewBottomSheetBackGround.visibility = View.GONE
            binding.tiLayout.visibility = View.VISIBLE
            hideKeyboard(bsRename.etFilenameInput)
            Handler(Looper.getMainLooper()).postDelayed({
                bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED

            }, 100)
            hideKeyboard(bsRename.etFilenameInput)
        }
    }

    override val recyclerScrollKey = "bg.co.vik.scrollposition"

    override fun loadListPosition() {
        binding.apply {
           // var scrollPosition = appDB.audioRecordDao().getPosition()[0]
            if (rvPosition > 0 &&
                rvPosition < rvGallary.layoutManager!!.childCount) {
                rvPosition++ // To offset the "completely visible" item under the action bar
            }
            rvGallary.scrollToPosition(rvPosition)
        }
    }

    override fun saveListPosition() {
        rvPosition = (binding.rvGallary.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
        //rvPosition = RecyclerViewPosition(position)
        //appDB.audioRecordDao().insertPosition(rvPosition)
    }

    override fun resetListPosition() {
        //val updatedPosition = RecyclerViewPosition(0)
        //updatedPosition.id = 0
       // appDB.audioRecordDao().updatePosition(updatedPosition)
    }
}













