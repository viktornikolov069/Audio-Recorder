package com.example.audiorecorder.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Vibrator
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import com.example.audiorecorder.R
import com.example.audiorecorder.databinding.ActivityMainBinding
import com.example.audiorecorder.utils.Constants.REQUEST_CODE
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {

    private lateinit var binding: ActivityMainBinding
    private var permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var permissionGranted = false
    private lateinit var recorder: MediaRecorder

    private var dirPath = ""
    private var filename = ""
    private var isRecording = false
    private var isPaused = false

    private lateinit var timer: Timer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionGranted = ActivityCompat.checkSelfPermission(
            this, permissions[0]) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {
            ActivityCompat.requestPermissions(
                this,
                permissions,
                REQUEST_CODE
            )
        }

        /* Initialising Timer */
        timer = Timer(this)

        binding.btnRecord.setImageResource(R.drawable.ic_play)
        binding.btnRecord.setOnClickListener {
            when {
                isPaused -> resumeRecorder()
                isRecording -> pauseRecorder()
                else -> startRecording()
            }
        }
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
        binding.btnRecord.setImageResource(R.drawable.ic_pause)
        isRecording = true
        isPaused = false
        timer.start()
    }

    private fun stopRecorder() {
        timer.stop()
    }

    override fun onTimerTick(duration: String) {
        binding.tvTimer.text = duration
    }
}



























