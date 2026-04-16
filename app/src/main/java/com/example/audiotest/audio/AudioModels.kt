package com.example.audiotest.audio

data class AudioDevice(
    val id: Int,
    val name: String,
    val type: String,
    val isSink: Boolean,
    val isSource: Boolean
)

data class FileAudioInfo(
    val sampleRate: Int,
    val channelConfig: Int,
    val audioFormat: Int
)

data class RoutedDevice(
    val name: String,
    val type: String,
    val id: Int
)

data class AudioInfo(
    val id: Int,
    val sampleRate: Int,
    val channelCount: Int,
    val audioFormat: Int,
    val isOffloaded: Boolean? = null,
    val audioSource: Int? = null,
    val usage: Int? = null,
    val contentType: Int? = null,
    val flags: Int? = null,
    val routedDevices: List<RoutedDevice>? = null
)

data class AmplitudeData(
    val id: Int,
    val amp: Double,
    val path: String? = null
)
