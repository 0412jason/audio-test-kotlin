package com.example.audiotest.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log

object AudioInfoHelper {

    fun getAudioModeOptions(): Map<String, Int> {
        val options = mutableMapOf<String, Int>()
        for (field in AudioManager::class.java.fields) {
            try {
                if (field.type == Int::class.javaPrimitiveType && field.name.startsWith("MODE_")) {
                    val value = field.getInt(null)
                    val name = field.name
                    options[name] = value
                }
            } catch (e: Exception) {
                // Ignore inaccessible fields
            }
        }
        return options
    }

    fun getAudioStreamOptions(): Map<String, Int> {
        val options = mutableMapOf<String, Int>()
        for (field in AudioManager::class.java.fields) {
            try {
                if (field.type == Int::class.javaPrimitiveType && field.name.startsWith("STREAM_")) {
                    val value = field.getInt(null)
                    val name = field.name
                    options[name] = value
                }
            } catch (e: Exception) {
                // Ignore inaccessible fields
            }
        }
        return options
    }

    fun getAudioSourceOptions(): Map<String, Int> {
        val options = mutableMapOf<String, Int>()
        for (field in android.media.MediaRecorder.AudioSource::class.java.fields) {
            try {
                if (field.type == Int::class.javaPrimitiveType) {
                    val value = field.getInt(null)
                    val name = field.name
                    options[name] = value
                }
            } catch (e: Exception) {
                // Ignore inaccessible fields
            }
        }
        return options
    }

    fun getAudioAttributesOptions(): Map<String, Map<String, Int>> {
        val usages = mutableMapOf<String, Int>()
        val contentTypes = mutableMapOf<String, Int>()
        val flags = mutableMapOf<String, Int>()

        // getDeclaredFields() includes non-public (@hide) fields; setAccessible(true) unlocks them.
        for (field in AudioAttributes::class.java.declaredFields) {
            try {
                field.isAccessible = true
                if (field.type == Int::class.javaPrimitiveType) {
                    val value = field.getInt(null)
                    val name = field.name
                    if (name.startsWith("USAGE_")) {
                        usages[name] = value
                    } else if (name.startsWith("CONTENT_TYPE_")) {
                        contentTypes[name] = value
                    } else if (name.startsWith("FLAG_")) {
                        flags[name] = value
                    }
                }
            } catch (e: Exception) {
                // Ignore inaccessible fields
            }
        }

        // Fallback: manually add @hide flags that Android's hidden API enforcement may block via reflection.
        // Values are stable across AOSP versions (confirmed in AudioAttributes.java source).
        val hiddenFlags = mapOf(
            "FLAG_SECURE"              to 0x2,     // 1 << 1
            "FLAG_SCO"                 to 0x4,     // 1 << 2
            "FLAG_DEEP_BUFFER"         to 0x200,   // 1 << 9
            "FLAG_NO_MEDIA_PROJECTION" to 0x400,   // 1 << 10
            "FLAG_MUTE_HAPTIC"         to 0x800,   // 1 << 11
            "FLAG_NO_SYSTEM_CAPTURE"   to 0x1000,  // 1 << 12
            "FLAG_CAPTURE_PRIVATE"     to 0x2000,  // 1 << 13
            "FLAG_CONTENT_SPATIALIZED" to 0x4000,  // 1 << 14
            "FLAG_NEVER_SPATIALIZE"    to 0x8000,  // 1 << 15
            "FLAG_SUPPRESS_BROADCAST"  to 0x10000, // 1 << 16
        )
        for ((name, value) in hiddenFlags) {
            if (!flags.containsKey(name)) {
                flags[name] = value
            }
        }

        return mapOf("usages" to usages, "contentTypes" to contentTypes, "flags" to flags)
    }

    fun getFileAudioInfo(filePath: String): Map<String, Int>? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(filePath)
            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                if (fmt.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    break
                }
            }
            if (audioTrackIndex < 0) return null

            val fmt = extractor.getTrackFormat(audioTrackIndex)
            val sampleRate =
                if (fmt.containsKey(MediaFormat.KEY_SAMPLE_RATE))
                    fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                else 48000
            val channelCount =
                if (fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
                    fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                else 1
            
            val pcmEncoding =
                if (fmt.containsKey(MediaFormat.KEY_PCM_ENCODING))
                    fmt.getInteger(MediaFormat.KEY_PCM_ENCODING)
                else AudioFormat.ENCODING_PCM_24BIT_PACKED

            val channelConfig = when (channelCount) {
                1 -> AudioFormat.CHANNEL_OUT_MONO
                2 -> AudioFormat.CHANNEL_OUT_STEREO
                else -> AudioFormat.CHANNEL_OUT_MONO
            }

            mapOf(
                "sampleRate" to sampleRate,
                "channelConfig" to channelConfig,
                "audioFormat" to pcmEncoding
            )
        } catch (e: Exception) {
            Log.e("AudioInfoHelper", "getFileAudioInfo failed: $e")
            null
        } finally {
            extractor.release()
        }
    }

    fun getAudioFormatForMime(mime: String): Int {
        return when (mime) {
            MediaFormat.MIMETYPE_AUDIO_MPEG -> AudioFormat.ENCODING_MP3
            MediaFormat.MIMETYPE_AUDIO_AAC -> AudioFormat.ENCODING_AAC_LC
            MediaFormat.MIMETYPE_AUDIO_OPUS -> AudioFormat.ENCODING_OPUS
            "audio/mp4a-latm" -> AudioFormat.ENCODING_AAC_LC
            else -> AudioFormat.ENCODING_INVALID
        }
    }
}
