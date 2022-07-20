package com.example.audiorecorder.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.audiorecorder.R
import com.example.audiorecorder.databinding.ActivityMainBinding
import com.example.audiorecorder.databinding.BottomSheetBinding
import com.example.audiorecorder.utils.Constants.REQUEST_CODE
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {

    /* Binding variables */
    private lateinit var binding: ActivityMainBinding
    private lateinit var bindingBottomSheet: BottomSheetBinding

    /*Permission variables */
    private var permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var permissionGranted = false

    /* Recorder variables */
    private lateinit var recorder: MediaRecorder
    private var dirPath = ""
    private var filename = ""
    private var isRecording = false
    private var isPaused = false

    /* Timer variables */
    private lateinit var timer: Timer

    /* Bottom Sheet variables */
    private lateinit var bottomSheetBehaviour: BottomSheetBehavior<LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* View bindings*/
        binding = ActivityMainBinding.inflate(layoutInflater)
        bindingBottomSheet = BottomSheetBinding.inflate(layoutInflater)
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

        /* Initialise BottomSheetBehavior */
        bottomSheetBehaviour = BottomSheetBehavior.from(bindingBottomSheet.bottomSheetSaveRecord)

        /* Initialise Timer */
        timer = Timer(this)

        binding.apply {
            btnRecord.setImageResource(R.drawable.ic_play)
            btnRecord.setOnClickListener {
                when {
                    isPaused -> resumeRecorder()
                    isRecording -> pauseRecorder()
                    else -> startRecording()
                }
            }

            btnList.setOnClickListener {
                //TODO
                Toast.makeText(this@MainActivity, "List button", Toast.LENGTH_LONG).show()
            }

            btnDone.setOnClickListener {
                stopRecorder()
                // TODO
                Toast.makeText(this@MainActivity, "Record saved", Toast.LENGTH_LONG).show()
            }

            btnDelete.setOnClickListener {
                stopRecorder()
                File("$dirPath$filename.mp3")
                Toast.makeText(this@MainActivity, "Record deleted", Toast.LENGTH_LONG).show()

            }
            btnDelete.isClickable = false
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

}



























