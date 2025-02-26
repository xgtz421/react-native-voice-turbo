package com.grapecity.leyserkids.voice

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.annotation.NonNull
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import java.util.*

class VoiceTurboModule(reactContext: ReactApplicationContext) : NativeVoiceTurboSpec(reactContext), RecognitionListener {
    companion object {
        const val NAME = "VoiceTurbo"

        fun getErrorText(errorCode: Int): String {
            return when (errorCode) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
                SpeechRecognizer.ERROR_SERVER -> "Error from server"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Didn't understand, please try again."
            }
        }
    }

    private val reactContext: ReactApplicationContext = reactContext
    private var speech: SpeechRecognizer? = null
    private var isRecognizing = false
    private var locale: String? = null

    private fun getLocale(locale: String?): String {
        return if (!locale.isNullOrEmpty()) locale else Locale.getDefault().toString()
    }

    private fun startListening(opts: ReadableMap) {
        speech?.destroy()
        speech = null

        speech = if (opts.hasKey("RECOGNIZER_ENGINE")) {
            when (opts.getString("RECOGNIZER_ENGINE")) {
                "GOOGLE" -> SpeechRecognizer.createSpeechRecognizer(reactContext, ComponentName.unflattenFromString("com.google.android.googlequicksearchbox/com.google.android.voicesearch.serviceapi.GoogleRecognitionService"))
                else -> SpeechRecognizer.createSpeechRecognizer(reactContext)
            }
        } else {
            SpeechRecognizer.createSpeechRecognizer(reactContext)
        }

        speech?.setRecognitionListener(this)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        val iterator = opts.keySetIterator()
        while (iterator.hasNextKey()) {
            val key = iterator.nextKey()
            when (key) {
                "EXTRA_LANGUAGE_MODEL" -> {
                    when (opts.getString(key)) {
                        "LANGUAGE_MODEL_FREE_FORM" -> intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        "LANGUAGE_MODEL_WEB_SEARCH" -> intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
                        else -> intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    }
                }
                "EXTRA_MAX_RESULTS" -> {
                    val extras = opts.getDouble(key)
                    intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, extras.toInt())
                }
                "EXTRA_PARTIAL_RESULTS" -> intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, opts.getBoolean(key))
                "EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS" -> {
                    val extras = opts.getDouble(key)
                    intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, extras.toInt())
                }
                "EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS" -> {
                    val extras = opts.getDouble(key)
                    intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, extras.toInt())
                }
                "EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS" -> {
                    val extras = opts.getDouble(key)
                    intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, extras.toInt())
                }
            }
        }

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, getLocale(this.locale))
        speech?.startListening(intent)
    }

    private fun startSpeechWithPermissions(locale: String?, opts: ReadableMap, callback: Callback) {
        this.locale = locale

        val mainHandler = Handler(reactContext.mainLooper)
        mainHandler.post {
            try {
                startListening(opts)
                isRecognizing = true
                callback.invoke(false)
            } catch (e: Exception) {
                callback.invoke(e.message)
            }
        }
    }

    override fun getName(): String {
        return NAME
    }

    @ReactMethod
    override fun startSpeech(locale: String?, opts: ReadableMap, callback: Callback) {
        if (!isPermissionGranted() && opts.getBoolean("REQUEST_PERMISSIONS_AUTO")) {
            val PERMISSIONS = arrayOf(Manifest.permission.RECORD_AUDIO)
            if (currentActivity != null) {
                (currentActivity as PermissionAwareActivity).requestPermissions(PERMISSIONS, 1, object : PermissionListener {
                    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray): Boolean {
                        val permissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                        startSpeechWithPermissions(locale, opts, callback)
                        return permissionsGranted
                    }
                })
            }
            return
        }
        startSpeechWithPermissions(locale, opts, callback)
    }

    @ReactMethod
    override fun stopSpeech(callback: Callback) {
        val mainHandler = Handler(reactContext.mainLooper)
        mainHandler.post {
            try {
                speech?.stopListening()
                isRecognizing = false
                callback.invoke(false)
            } catch (e: Exception) {
                callback.invoke(e.message)
            }
        }
    }

    @ReactMethod
    override fun cancelSpeech(callback: Callback) {
        val mainHandler = Handler(reactContext.mainLooper)
        mainHandler.post {
            try {
                speech?.cancel()
                isRecognizing = false
                callback.invoke(false)
            } catch (e: Exception) {
                callback.invoke(e.message)
            }
        }
    }

    @ReactMethod
    override fun destroySpeech(callback: Callback) {
        val mainHandler = Handler(reactContext.mainLooper)
        mainHandler.post {
            try {
                speech?.destroy()
                speech = null
                isRecognizing = false
                callback.invoke(false)
            } catch (e: Exception) {
                callback.invoke(e.message)
            }
        }
    }

    @ReactMethod
    override fun isSpeechAvailable(callback: Callback) {
        val self = this
        val mainHandler = Handler(reactContext.mainLooper)
        mainHandler.post {
            try {
                val isSpeechAvailable = SpeechRecognizer.isRecognitionAvailable(self.reactContext)
                callback.invoke(isSpeechAvailable, false)
            } catch (e: Exception) {
                callback.invoke(false, e.message)
            }
        }
    }

    @ReactMethod
    override fun getSpeechRecognitionServices(promise: Promise) {
        @SuppressLint("QueryPermissionsNeeded") val services = reactContext.packageManager
            .queryIntentServices(Intent(RecognitionService.SERVICE_INTERFACE), 0)
        val serviceNames = Arguments.createArray()
        for (service in services) {
            serviceNames.pushString(service.serviceInfo.packageName)
        }

        promise.resolve(serviceNames)
    }

    private fun isPermissionGranted(): Boolean {
        val permission = Manifest.permission.RECORD_AUDIO
        val res = reactContext.checkCallingOrSelfPermission(permission)
        return res == PackageManager.PERMISSION_GRANTED
    }

    @ReactMethod
    override fun isRecognizing(callback: Callback) {
        callback.invoke(isRecognizing)
    }

    private fun sendEvent(eventName: String, params: WritableMap?) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    override fun onBeginningOfSpeech() {
        val event = Arguments.createMap()
        event.putBoolean("error", false)
        sendEvent("onSpeechStart", event)
        Log.d("ASR", "onBeginningOfSpeech()")
    }

    override fun onBufferReceived(buffer: ByteArray) {
        val event = Arguments.createMap()
        event.putBoolean("error", false)
        sendEvent("onSpeechRecognized", event)
        Log.d("ASR", "onBufferReceived()")
    }

    override fun onEndOfSpeech() {
        val event = Arguments.createMap()
        event.putBoolean("error", false)
        sendEvent("onSpeechEnd", event)
        Log.d("ASR", "onEndOfSpeech()")
        isRecognizing = false
    }

    override fun onError(errorCode: Int) {
        val errorMessage = String.format("%d/%s", errorCode, getErrorText(errorCode))
        val error = Arguments.createMap()
        error.putString("message", errorMessage)
        error.putString("code", errorCode.toString())
        val event = Arguments.createMap()
        event.putMap("error", error)
        sendEvent("onSpeechError", event)
        Log.d("ASR", "onError() - $errorMessage")
    }

    override fun onEvent(arg0: Int, arg1: Bundle) {}

    override fun onPartialResults(results: Bundle) {
        val arr = Arguments.createArray()

        val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        matches?.forEach { result ->
            arr.pushString(result)
        }

        val event = Arguments.createMap()
        event.putArray("value", arr)
        sendEvent("onSpeechPartialResults", event)
        Log.d("ASR", "onPartialResults()")
    }

    override fun onReadyForSpeech(arg0: Bundle) {
        val event = Arguments.createMap()
        event.putBoolean("error", false)
        sendEvent("onSpeechStart", event)
        Log.d("ASR", "onReadyForSpeech()")
    }

    override fun onResults(results: Bundle) {
        val arr = Arguments.createArray()

        val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        matches?.forEach { result ->
            arr.pushString(result)
        }

        val event = Arguments.createMap()
        event.putArray("value", arr)
        sendEvent("onSpeechResults", event)
        Log.d("ASR", "onResults()")
    }

    override fun onRmsChanged(rmsdB: Float) {
        val event = Arguments.createMap()
        event.putDouble("value", rmsdB.toDouble())
        sendEvent("onSpeechVolumeChanged", event)
    }
}
