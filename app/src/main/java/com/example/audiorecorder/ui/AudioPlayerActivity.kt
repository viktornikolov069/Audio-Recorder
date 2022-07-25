package com.example.audiorecorder.ui

import android.media.MediaPlayer
import android.media.PlaybackParams
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.res.ResourcesCompat
import androidx.room.Room
import com.example.audiorecorder.R
import com.example.audiorecorder.databinding.ActivityAudioPlayerBinding
import com.example.audiorecorder.db.AppDatabase
import com.example.audiorecorder.db.AudioRecord
import com.example.audiorecorder.utils.Constants
import com.example.audiorecorder.utils.Constants.BUNDLE_AUDIO_RECORD_ID

class AudioPlayerActivity : AppCompatActivity() {

    /* Binding variables */
    private lateinit var binding: ActivityAudioPlayerBinding

    /* Database variables */
    private val appDB: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, Constants.APP_DATABASE)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
    }

    /* Media player variables */
    private lateinit var mediaPlayer: MediaPlayer

    /* Handler and Runnable */
    private lateinit var runnable: Runnable
    private lateinit var handler: Handler


    /* AudioRecord variables */
    private lateinit var record: AudioRecord
    private var id = 0
    private var  filename = ""
    private var filePath = ""
    private val delay = 1000L
    private val jumpValue = 1000
    private var playBackSpeed = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /* Receiving intent from onItemClickListener() in GalleryActivity.kt */
        intent.extras?.let {
            id = it.getInt(BUNDLE_AUDIO_RECORD_ID)
        }

        /* Extracting data from database */
        filename = appDB.audioRecordDao().getAll().find { it.id == id }!!.filename
        filePath = appDB.audioRecordDao().getAll().find { it.id == id }!!.filePath

        /* Preparing MediaPlayer */
        mediaPlayer = MediaPlayer()
        mediaPlayer.apply {
            setDataSource(filePath)
            prepare()
        }

        /* The handler delays a task that will be executed by the MainLooper */
        handler = Handler(Looper.getMainLooper())

        /* Set progress at current position and schedule another call to the same function after
        *  one second (this function is calling itself every second and updating the curr. pos.)*/
        runnable = Runnable {
            binding.apply {
                seekBar.progress = mediaPlayer.currentPosition
                handler.postDelayed(runnable, delay)
            }
        }

        binding.btnPlay.setOnClickListener {
            playPausePlayer()
        }

        playPausePlayer()

        /* SeekBar starts at 0 and ends at mediaPlayer duration */
        binding.seekBar.max = mediaPlayer.duration

        mediaPlayer.setOnCompletionListener {
            binding.btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_pause_circle, theme)
        }

        binding.apply {
            btnForward.setOnClickListener {
                mediaPlayer.seekTo(mediaPlayer.currentPosition + jumpValue)
                seekBar.progress += jumpValue
            }

            btnBackward.setOnClickListener {
                mediaPlayer.seekTo(mediaPlayer.currentPosition - jumpValue)
                seekBar.progress -= jumpValue
            }

            chipSpeed.setOnClickListener {
                when (playBackSpeed) {
                    2f -> playBackSpeed = 0.5f
                    else -> playBackSpeed += 0.5f
                }
                mediaPlayer.playbackParams = PlaybackParams().setSpeed(playBackSpeed)
                chipSpeed.text = "x $playBackSpeed"
            }
        }
    }

    private fun playPausePlayer() {

        // Not playing
        if (!mediaPlayer.isPlaying) {
             mediaPlayer.start()
            binding.btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_pause_circle, theme)
            handler.postDelayed(runnable, 0)
        } else {
            mediaPlayer.pause()
            binding.btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_play_circle, theme)
            handler.removeCallbacks(runnable)
        }
    }
}











































