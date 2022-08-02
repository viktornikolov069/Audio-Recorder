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

    /* This variable is responsible for the check box inside the recycle view when all are checked */
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

        /* --- Filtering the recycle view for a specific file --- */
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

        /* --- Enables swiping and deletes a recording on swipe --- */
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

        /* --- Toolbar functions --- */
        binding.apply {
            /* Opens DetailsActivity which shows details about the currently chosen recording */
            btnInfo.isClickable = false
            btnInfo.setOnClickListener {
                sendIntentToDetailsActivity()
            }

            /* Closes the toolbar */
            binding.btnClose.setOnClickListener {
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

            /* Checks all check boxes */
            btnSelectAll.setOnClickListener {
                allChecked = !allChecked
                audioAdapter.differ.currentList.map {
                    it.isChecked = allChecked
                }
                btnRename.isClickable = false
                btnRename.setImageResource(R.drawable.ic_rename_disabled)
                saveListPosition()
                setupRecyclerView()
                loadListPosition()
            }

            /* Renames the currently checked row of the recycler view */
            btnRename.isClickable = false
            btnRename.setOnClickListener {
                bottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
                viewBottomSheetBackGround.visibility = View.VISIBLE
                tiLayout.visibility = View.GONE
                val checked = audioAdapter.differ.currentList.first { it.isChecked }
                bsRename.etFilenameInput.setText(checked.filename)
            }

            /* --- BottomSheetRename Buttons --- */

            bsRename.btnCancel.setOnClickListener {
                dismiss()
            }

            /* After pressing OK button and renaming the file there is a bug where the check box
            *  looks like it's checked but it's actually not. This causes a crash if either the
            * rename or the details buttons are pressed right after pressing OK. To prevent the
            * crash I have chosen to disable them in the btnOk function. Clicking the already
            * checked check box after clicking the OK button actually checks the check box fixing
            * the issue and it also enables the details and rename buttons. */
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
                    checked.filePath = "$dirPath$newFilename.mp3"
                    btnInfo.isClickable = false
                    btnRename.isClickable = false
                    btnRename.setImageResource(R.drawable.ic_rename_disabled)
                    btnInfo.setImageResource(R.drawable.ic_info_disabled)
                    appDB.audioRecordDao().update(checked)
                    audioAdapter.differ.submitList(appDB.audioRecordDao().getAll())
                    runOnUiThread {
                        saveListPosition()
                        setupRecyclerView()
                        loadListPosition()
                    }
                }
            }

            viewBottomSheetBackGround.setOnClickListener {
                dismiss()
            }
        }
    }

    /* This function is used when searching for a file
     in the search field inside activity_gallery.xml */
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
                    runOnUiThread {
                        setupRecyclerView()
                    }
                }
            }
        }
    }

    /* Refreshes the RV */
    private fun setupRecyclerView() {
        binding.rvGallary.apply {
            layoutManager = LinearLayoutManager(this@GalleryActivity)
            adapter = audioAdapter
        }
    }

    /* Opens AudioPlayerActivity or if EditMode is on it will
       either check or uncheck the current row. */
    override fun onItemClickListener(position: Int) {
        var audioRecord = audioAdapter.differ.currentList[position]

        if (audioAdapter.isEditMode()) {
            audioAdapter.differ.currentList[position].isChecked =
                !audioAdapter.differ.currentList[position].isChecked
            enableDisableBtnRenameBtnInfo()

            runOnUiThread {
                saveListPosition()
                setupRecyclerView()
                loadListPosition()
            }
        } else {
            val intent = Intent(this, AudioPlayerActivity::class.java)
            intent.putExtra(BUNDLE_AUDIO_RECORD_ID, audioRecord.id)
            startActivity(intent)
        }
    }

    /* Enables EditMode which in turn activates the toolbar and shows the check boxes. */
    override fun onItemLongClickListener(position: Int) {
        audioAdapter.setEditMode(true)
        audioAdapter.differ.currentList[position].isChecked =
            !audioAdapter.differ.currentList[position].isChecked

        if (binding.editBar.visibility == View.GONE) {
            binding.editBar.visibility = View.VISIBLE
            binding.mtToolbarGallery.visibility = View.GONE
        }
        enableDisableBtnRenameBtnInfo()

        runOnUiThread {
            saveListPosition()
            setupRecyclerView()
            loadListPosition()
        }
    }

    /* Deletes rec. and is called on swipe. Also shows a snackbar message item has been deleted.
    *  Currently working on a UNDO option. UNDO button is not active.*/
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

    /* --- HELPER FUNCTIONS --- */

    /* */
    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /* Mainly this function hides the bottom sheet rename layout. It's used after clicking
    *  OK or CANCEL or in the background view */
    private fun dismiss() {
        binding.apply {
            /* Hides bottomSheetBackGround */
            viewBottomSheetBackGround.visibility = View.GONE
            /* Makes the tiLayout which contains the search field in activity_gallery.xml visible */
            binding.tiLayout.visibility = View.VISIBLE
            /* Hides keyboard and bottom sheet with an added delay of 100ms */
            Handler(Looper.getMainLooper()).postDelayed({
                bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED

            }, 100)
            hideKeyboard(bsRename.etFilenameInput)
        }
    }

    /* This variable is not used but necessary because of ListPositioner Interface.
    *  Probably the whole interface will be removed because I don't think is needed. */
    override val recyclerScrollKey = "bg.co.vik.scrollposition"

    /* Loads the current position of the RV */
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

    /* Saves the current position of the RV */
    override fun saveListPosition() {
        rvPosition = (binding.rvGallary.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
    }

    /* Resets RV position */
    override fun resetListPosition() {
        rvPosition = 0
    }

    /* Snackbar is shown temporarily after a row is deleted and provides an UNDO option.
    *  UNDO is not active. */
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

    /* Undoes delete by inserting the deleted recording back and giving the previous list
    *  to the adapter.
    *  I haven't still figured out how to save the .mp3 file itself. */
    private fun undoDelete() {
        appDB.audioRecordDao().insert(deletedRecording)
        audioAdapter.differ.submitList(oldRecordingList)
        runOnUiThread {
            setupRecyclerView()
        }
    }

    /* If there is only one row that has a checked check box it will enable the rename button. */
    private fun enableDisableBtnRenameBtnInfo() {
        val isCheckedCount = audioAdapter.differ.currentList.count { it.isChecked }
        binding.apply {
            if (isCheckedCount == 1) {
                btnInfo.setImageResource(R.drawable.ic_info)
                btnRename.setImageResource(R.drawable.ic_rename)
                btnInfo.isClickable = true
                btnRename.isClickable = true
            } else {
                btnInfo.setImageResource(R.drawable.ic_info_disabled)
                btnRename.setImageResource(R.drawable.ic_rename_disabled)
                btnRename.isClickable = false
                btnInfo.isClickable = false
            }
        }
    }

    /* Used in btnInfo to go to DetailsActivity */
    private fun sendIntentToDetailsActivity() {
        val checked = audioAdapter.differ.currentList.first { it.isChecked }
        val intent = Intent(this, DetailsActivity::class.java)
        intent.putExtra(BUNDLE_AUDIO_RECORD_ID, checked.id)
        startActivity(intent)
    }

}













