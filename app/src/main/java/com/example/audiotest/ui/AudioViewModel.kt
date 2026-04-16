package com.example.audiotest.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audiotest.audio.AudioEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AudioViewModel(application: Application) : AndroidViewModel(application) {
    
    val amplitudeData = AudioEngine.amplitudeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    
    val audioInfoData = AudioEngine.audioInfoFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Map Unit to Int counter so we can detect changes via collectAsState
    private var _deviceChangeCounter = 0
    val deviceChangeData = AudioEngine.deviceChangeFlow
        .map { _deviceChangeCounter++ }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    
    init {
        // We can listen to device changes and update state accordingly if needed
    }
}

