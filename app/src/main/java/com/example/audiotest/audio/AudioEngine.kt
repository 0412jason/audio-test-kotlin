package com.example.audiotest.audio

import android.app.Activity
import android.media.AudioManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import android.content.Context

object AudioEngine {
    private var isInitialized = false
    
    lateinit var deviceManager: AudioDeviceManager
    lateinit var playbackManager: AudioPlaybackManager
    lateinit var recordingManager: AudioRecordingManager

    // Flows to emit events to the UI
    private val _amplitudeFlow = MutableSharedFlow<AmplitudeData>(extraBufferCapacity = 64)
    val amplitudeFlow = _amplitudeFlow.asSharedFlow()

    private val _deviceChangeFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val deviceChangeFlow = _deviceChangeFlow.asSharedFlow()

    private val _audioInfoFlow = MutableSharedFlow<AudioInfo>(extraBufferCapacity = 64)
    val audioInfoFlow = _audioInfoFlow.asSharedFlow()

    // Caches
    var cachedUsagesMap = emptyMap<String, Int>()
    var cachedContentTypesMap = emptyMap<String, Int>()
    var cachedFlagsMap = emptyMap<String, Int>()
    var cachedAudioSourcesMap = emptyMap<String, Int>()
    var cachedStreamTypesMap = emptyMap<String, Int>()

    fun init(activity: Activity) {
        if (isInitialized) return
        deviceManager = AudioDeviceManager(activity)
        playbackManager = AudioPlaybackManager(activity)
        recordingManager = AudioRecordingManager(activity)
        
        val options = AudioInfoHelper.getAudioAttributesOptions()
        cachedUsagesMap = options["usages"] ?: emptyMap()
        cachedContentTypesMap = options["contentTypes"] ?: emptyMap()
        cachedFlagsMap = options["flags"] ?: emptyMap()
        cachedAudioSourcesMap = AudioInfoHelper.getAudioSourceOptions()
        cachedStreamTypesMap = AudioInfoHelper.getAudioStreamOptions()
        
        isInitialized = true
    }

    // Callbacks from Managers
    fun notifyAmplitude(id: Int, amp: Double, path: String? = null) {
        _amplitudeFlow.tryEmit(AmplitudeData(id, amp, path))
    }

    fun notifyDeviceChange() {
        _deviceChangeFlow.tryEmit(Unit)
    }

    fun notifyAudioInfo(info: AudioInfo) {
        _audioInfoFlow.tryEmit(info)
    }

    fun getAudioMode(context: Context): Int {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.mode
    }

    fun setAudioMode(context: Context, mode: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.mode = mode
    }
}
