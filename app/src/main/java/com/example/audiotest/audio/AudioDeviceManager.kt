package com.example.audiotest.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log

class AudioDeviceManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioDeviceCallback: android.media.AudioDeviceCallback? = null

    init {
        registerAudioDeviceCallback()
    }



    fun getAudioDevices(isOutput: Boolean): List<Map<String, Any>> {
        val flag = if (isOutput) AudioManager.GET_DEVICES_OUTPUTS else AudioManager.GET_DEVICES_INPUTS
        val devices = audioManager.getDevices(flag)

        return devices.map { device ->
            val typeName = getDeviceTypeName(device.type)
            mapOf(
                "id" to device.id,
                "name" to device.productName.toString(),
                "type" to "$typeName (${device.type})",
                "isSink" to device.isSink,
                "isSource" to device.isSource
            )
        }
    }

    fun setCommunicationDevice(deviceId: Int?): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return if (deviceId == null) {
                audioManager.clearCommunicationDevice()
                Log.d("AudioDeviceManager", "clearCommunicationDevice")
                true
            } else {
                val devices = audioManager.availableCommunicationDevices
                val device = devices.firstOrNull { it.id == deviceId }
                if (device != null) {
                    val success = audioManager.setCommunicationDevice(device)
                    Log.d("AudioDeviceManager", "setCommunicationDevice ($deviceId): $success")
                    success
                } else {
                    Log.w("AudioDeviceManager", "setCommunicationDevice: device $deviceId not found")
                    false
                }
            }
        } else {
            Log.w("AudioDeviceManager", "setCommunicationDevice is only supported on Android 12+")
            return false
        }
    }

    fun setPreferredDeviceForStrategy(streamType: Int, deviceId: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        try {
            // 1. Get strategy by streamType
            val strategyClass = Class.forName("android.media.audiopolicy.AudioProductStrategy")
            val getStrategiesMethod = try {
                AudioManager::class.java.getMethod("getAudioProductStrategies")
            } catch (e: NoSuchMethodException) {
                strategyClass.getMethod("getAudioProductStrategies")
            }
            
            val strategies = if (java.lang.reflect.Modifier.isStatic(getStrategiesMethod.modifiers)) {
                getStrategiesMethod.invoke(null) as? List<*>
            } else {
                getStrategiesMethod.invoke(audioManager) as? List<*>
            } ?: return false
            
            val getAttrsForStreamType = strategyClass.getMethod("getAudioAttributesForStreamType", Int::class.javaPrimitiveType)
            getAttrsForStreamType.isAccessible = true
            val strategy = strategies.firstOrNull { 
                getAttrsForStreamType.invoke(it, streamType) != null
            } ?: return false

            // 2. Get device
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val deviceInfo = devices.firstOrNull { it.id == deviceId } ?: return false

            // 3. Create AudioDeviceAttributes
            val attributesClass = Class.forName("android.media.AudioDeviceAttributes")
            var attributesObj: Any? = null
            try {
                val constructor = attributesClass.getConstructor(AudioDeviceInfo::class.java)
                attributesObj = constructor.newInstance(deviceInfo)
            } catch (e: Exception) {
                val constructor = attributesClass.getConstructor(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
                attributesObj = constructor.newInstance(2 /* ROLE_OUTPUT */, deviceInfo.type, deviceInfo.address)
            }
            
            if (attributesObj == null) return false

            // 4. Set it
            val setPreferredMethod = AudioManager::class.java.getMethod("setPreferredDeviceForStrategy", strategyClass, attributesClass)
            val result = setPreferredMethod.invoke(audioManager, strategy, attributesObj) as? Boolean
            return result == true
        } catch (e: Exception) {
            Log.e("AudioDeviceManager", "setPreferredDeviceForStrategy failed", e)
            return false
        }
    }

    fun removePreferredDeviceForStrategy(streamType: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        try {
            val strategyClass = Class.forName("android.media.audiopolicy.AudioProductStrategy")
            val getStrategiesMethod = try {
                AudioManager::class.java.getMethod("getAudioProductStrategies")
            } catch (e: NoSuchMethodException) {
                strategyClass.getMethod("getAudioProductStrategies")
            }
            
            val strategies = if (java.lang.reflect.Modifier.isStatic(getStrategiesMethod.modifiers)) {
                getStrategiesMethod.invoke(null) as? List<*>
            } else {
                getStrategiesMethod.invoke(audioManager) as? List<*>
            } ?: return false
            
            val getAttrsForStreamType = strategyClass.getMethod("getAudioAttributesForStreamType", Int::class.javaPrimitiveType)
            getAttrsForStreamType.isAccessible = true
            val strategy = strategies.firstOrNull { 
                getAttrsForStreamType.invoke(it, streamType) != null
            } ?: return false

            val removePreferredMethod = AudioManager::class.java.getMethod("removePreferredDeviceForStrategy", strategyClass)
            val result = removePreferredMethod.invoke(audioManager, strategy) as? Boolean
            return result == true
        } catch (e: Exception) {
            Log.e("AudioDeviceManager", "removePreferredDeviceForStrategy failed", e)
            return false
        }
    }

    private fun registerAudioDeviceCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (audioDeviceCallback != null) return // Already registered

            audioDeviceCallback = object : android.media.AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    super.onAudioDevicesAdded(addedDevices)
                    notifyDeviceChange()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    super.onAudioDevicesRemoved(removedDevices)
                    notifyDeviceChange()
                }
            }
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        }
    }

    private fun unregisterAudioDeviceCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioDeviceCallback?.let {
                audioManager.unregisterAudioDeviceCallback(it)
                audioDeviceCallback = null
            }
        }
    }

    private fun notifyDeviceChange() {
        AudioEngine.notifyDeviceChange()
    }

    fun getDeviceTypeName(type: Int): String {
        return when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Built-in Earpiece"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Built-in Speaker"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
            AudioDeviceInfo.TYPE_LINE_ANALOG -> "Line Analog"
            AudioDeviceInfo.TYPE_LINE_DIGITAL -> "Line Digital"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
            AudioDeviceInfo.TYPE_HDMI -> "HDMI"
            AudioDeviceInfo.TYPE_HDMI_ARC -> "HDMI ARC"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Device"
            AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB Accessory"
            AudioDeviceInfo.TYPE_DOCK -> "Dock"
            AudioDeviceInfo.TYPE_FM -> "FM"
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Built-in Mic"
            AudioDeviceInfo.TYPE_FM_TUNER -> "FM Tuner"
            AudioDeviceInfo.TYPE_TV_TUNER -> "TV Tuner"
            AudioDeviceInfo.TYPE_TELEPHONY -> "Telephony"
            AudioDeviceInfo.TYPE_AUX_LINE -> "Aux Line"
            AudioDeviceInfo.TYPE_IP -> "IP"
            AudioDeviceInfo.TYPE_BUS -> "Bus"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
            AudioDeviceInfo.TYPE_HEARING_AID -> "Hearing Aid"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> "Built-in Speaker Safe"
            AudioDeviceInfo.TYPE_REMOTE_SUBMIX -> "Remote Submix"
            AudioDeviceInfo.TYPE_BLE_HEADSET -> "BLE Headset"
            AudioDeviceInfo.TYPE_BLE_SPEAKER -> "BLE Speaker"
            AudioDeviceInfo.TYPE_BLE_BROADCAST -> "BLE Broadcast"
            else -> "Unknown"
        }
    }
}
