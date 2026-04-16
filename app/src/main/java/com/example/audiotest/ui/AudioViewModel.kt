package com.example.audiotest.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audiotest.audio.AudioEngine
import kotlinx.coroutines.flow.SharingStarted
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
    
    init {
        // We can listen to device changes and update state accordingly if needed
    }
}
