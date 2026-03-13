package com.attentionbeeper

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.attentionbeeper.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var beepService: BeepService? = null
    private var serviceBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val b = binder as BeepService.BeepBinder
            beepService = b.getService()
            serviceBound = true
            observeService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            beepService = null
            serviceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupControls()
    }

    override fun onStart() {
        super.onStart()
        Intent(this, BeepService::class.java).also { intent ->
            startService(intent)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            unbindService(connection)
            serviceBound = false
        }
    }

    private fun setupControls() {
        // Sound spinner uses display names; maps by index to sound IDs
        val soundNames = resources.getStringArray(R.array.sound_names)
        val soundAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, soundNames)
        soundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSound.adapter = soundAdapter
        // Default: Digital Beep is index 0 — matches default "digital"

        binding.btnTest.setOnClickListener {
            val soundId = selectedSoundId()
            SoundSynthesizer.play(soundId)
        }

        binding.btnStart.setOnClickListener {
            val value = binding.editInterval.text.toString().toIntOrNull() ?: 60
            val unit = if (binding.spinnerUnit.selectedItemPosition == 0)
                IntervalUnit.SECONDS else IntervalUnit.MINUTES
            val mode = if (binding.radioFixed.isChecked) IntervalMode.FIXED else IntervalMode.RANDOM
            val soundId = selectedSoundId()

            val intent = Intent(this, BeepService::class.java)
            startService(intent)

            beepService?.startSession(value, unit, mode, soundId)
        }

        binding.btnStop.setOnClickListener {
            beepService?.stopSession()
        }
    }

    private fun selectedSoundId(): String {
        val soundIds = resources.getStringArray(R.array.sound_ids)
        val index = binding.spinnerSound.selectedItemPosition
        return if (index in soundIds.indices) soundIds[index] else "digital"
    }

    private fun observeService() {
        val service = beepService ?: return
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    service.sessionRunning.collect { running ->
                        updateSessionRunningUi(running)
                    }
                }
                launch {
                    service.countdown.collect { ms ->
                        updateCountdown(ms)
                    }
                }
            }
        }
    }

    private fun updateSessionRunningUi(running: Boolean) {
        if (running) {
            // Lock controls
            binding.editInterval.isEnabled = false
            binding.spinnerUnit.isEnabled = false
            binding.radioFixed.isEnabled = false
            binding.radioRandom.isEnabled = false
            binding.spinnerSound.isEnabled = false

            binding.btnStart.visibility = View.GONE
            binding.btnStop.visibility = View.VISIBLE
            binding.textCountdown.visibility = View.VISIBLE
            binding.statusText.text = getString(R.string.status_running)
            binding.statusIndicator.setImageResource(R.drawable.ic_status_running)
        } else {
            // Unlock controls
            binding.editInterval.isEnabled = true
            binding.spinnerUnit.isEnabled = true
            binding.radioFixed.isEnabled = true
            binding.radioRandom.isEnabled = true
            binding.spinnerSound.isEnabled = true

            binding.btnStart.visibility = View.VISIBLE
            binding.btnStop.visibility = View.GONE
            binding.textCountdown.visibility = View.GONE
            binding.statusText.text = getString(R.string.status_stopped)
            binding.statusIndicator.setImageResource(R.drawable.ic_status_stopped)
        }
    }

    private fun updateCountdown(ms: Long) {
        if (ms < 0) return
        val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        binding.textCountdown.text = String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
