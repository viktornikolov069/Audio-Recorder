package com.example.audiorecorder.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.room.Room
import com.example.audiorecorder.R
import com.example.audiorecorder.databinding.ActivityMainBinding
import com.example.audiorecorder.db.AppDatabase
import com.example.audiorecorder.db.AudioRecord
import com.example.audiorecorder.utils.Constants.APP_DATABASE
import com.example.audiorecorder.utils.Constants.REQUEST_CODE
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {

    /* Binding variables */
    private lateinit var binding: ActivityMainBinding
    //private lateinit var bindingBottomSheet: BottomSheetBinding

    /* Permission variables */
    private var permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var permissionGranted = false

    /* Recorder variables */
    private lateinit var recorder: MediaRecorder
    private var dirPath = ""
    private var filename = ""
    private var isRecording = false
    private var isPaused = false
    private var duration = ""

    /* Database variable */
    private val appDB: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, APP_DATABASE)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
    }

    /* Timer variables */
    private lateinit var timer: Timer

    /* Bottom Sheet variables */
    private lateinit var bottomSheetBehaviour: BottomSheetBehavior<LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* View bindings */
        binding = ActivityMainBinding.inflate(layoutInflater)
        //bindingBottomSheet = BottomSheetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /* Permissions */
        permissionGranted = ActivityCompat.checkSelfPermission(
            this, permissions[0]) == PackageManager.PERMISSION_GRANTED
        if (!permissionGranted) {
            ActivityCompat.requestPermissions(
                this,
                permissions,
                REQUEST_CODE
            )
        }

        /* --- Initialise BottomSheetBehavior and Setup --- */

        /* Simply creating another view binding variable
           Example -> (bindingBottomSheet:  BottomSheetBinding)
           and trying to access the id -> "bindingBottomSheet.bottomSheetSaveRecord",
           does not work. It gives the error "The view is not a child of CoordinatorLayout".
           What solved the error was giving the <include> layout its own id
           "bottomSheetSave" and accessing the actual bottom sheet layout id
           "bottomSheetSaveRecord" through it as can be seen in the code bellow.

           Solution found in the Medium blog of
           Somesh Kumar "Exploring Android View Binding in Depth" */
        bottomSheetBehaviour = BottomSheetBehavior.from(binding.bottomSheetSave.bottomSheetSaveRecord)
        bottomSheetBehaviour.peekHeight = 0 // This hides the bottom sheet
        bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED

        /* --- Initialise Timer --- */

        /* Timer(listener: OnTimerTickListener). MainActivity inherits OnTimerTickListener
        *  which makes "listener" a part of the context. That's why we can do Timer(this) */
        timer = Timer(this)

        /* MainActivity Buttons */
        binding.apply {
            btnRecord.setImageResource(R.drawable.ic_play)
            btnRecord.setOnClickListener {
                when {
                    isPaused -> resumeRecorder()
                    isRecording -> pauseRecorder()
                    else -> startRecording()
                }
                /* Temporary Log */
                Log.d("tag", "Test to see if log message works")
            }

            btnList.setOnClickListener {
                //TODO
                Toast.makeText(this@MainActivity, "List button", Toast.LENGTH_SHORT).show()
            }

            btnDone.setOnClickListener {
                stopRecorder()
                Toast.makeText(this@MainActivity, "Record saved", Toast.LENGTH_SHORT).show()

                bottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
                viewBottomSheetBackGround.visibility = View.VISIBLE
                bottomSheetSave.etFilenameInput.setText(filename)
            }

            btnDelete.setOnClickListener {
                stopRecorder()
                File("$dirPath$filename.mp3")
                Toast.makeText(this@MainActivity, "Record deleted", Toast.LENGTH_SHORT).show()

            }
            btnDelete.isClickable = false

            /* --- BottomSheetSave Buttons --- */
            bottomSheetSave.btnCancel.setOnClickListener {
                File("$dirPath$filename.mp3").delete()
                dismiss()
            }

            bottomSheetSave.btnOk.setOnClickListener {
                dismiss()
                save()
            }

            viewBottomSheetBackGround.setOnClickListener {
                File("$dirPath$filename.mp3").delete()
                dismiss()
            }
        }
    }


    /* --- Recorder functions --- */
    private fun pauseRecorder() {
        recorder.pause()
        isPaused = true
        binding.btnRecord.setImageResource(R.drawable.ic_play)
        timer.pause()
    }

    private fun resumeRecorder() {
        recorder.resume()
        isPaused = false
        binding.btnRecord.setImageResource(R.drawable.ic_pause)
        timer.start()
    }

    private fun startRecording() {
        if (!permissionGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
            return
        }
        recorder = MediaRecorder()
        dirPath = "${externalCacheDir?.absolutePath}/"

        var simpleDateFormat = SimpleDateFormat("yyyy.MM.DD_hh.mm.ss")
        var date = simpleDateFormat.format(Date())
        filename = "audio_record_$date"

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$filename.mp3")
            try {
                prepare()
            } catch(e: IOException) {}
            start()
        }
        binding.apply {
            btnRecord.setImageResource(R.drawable.ic_pause)
            btnDelete.isClickable = true
            btnDelete.setImageResource(R.drawable.ic_delete)
            btnList.visibility = View.GONE
            btnDone.visibility = View.VISIBLE
        }

        timer.start()
        isRecording = true
        isPaused = false
    }

    private fun stopRecorder() {
        timer.stop()
        recorder.apply {
            stop()
            reset() // In test!!!
            release()
        }

        isPaused = false
        isRecording = false

        binding.apply {
            btnList.visibility = View.VISIBLE
            btnDone.visibility = View.GONE
            btnDelete.isClickable = false
            btnDelete.setImageResource(R.drawable.ic_delete_disabled)
            btnRecord.setImageResource(R.drawable.ic_record)
            btnRecord.setImageResource(R.drawable.ic_play)
            tvTimer.text = "00:00.00"
        }
    }

    /* --- Overrides --- */
    override fun onTimerTick(duration: String) {
        binding.tvTimer.text = duration
        this.duration = duration.dropLast(3)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
        }
    }

    /* Hide Keyboard on Pressing Empty Space */
    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /* --- Helper Functions --- */
    private fun dismiss() {
        binding.apply {
            viewBottomSheetBackGround.visibility = View.GONE
            hideKeyboard(bottomSheetSave.etFilenameInput)
            Handler(Looper.getMainLooper()).postDelayed({
                bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED

            }, 100)
            hideKeyboard(bottomSheetSave.etFilenameInput)
        }
    }

    private fun save() {
        val newFilename = binding.bottomSheetSave.etFilenameInput.text.toString()
        if (newFilename != filename) {
            var newFile = File("$dirPath$newFilename.mp3")
            File("$dirPath$newFilename.mp3").renameTo(newFile)
        }

        /* Database variables */
        var filePath = "$dirPath$newFilename.mp3"
        var timestamp = Date().time
        //var ampsPath = "$dirPath$newFilename" // for drawing

        var record = AudioRecord(newFilename, filePath, timestamp, duration)

        /* Running on background thread for better performance */
        GlobalScope.launch {
            appDB.audioRecordDao().insert(record)
            Log.d("tag", "saving test")
        }
    }
}



























