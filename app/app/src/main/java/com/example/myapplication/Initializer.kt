package com.example.myapplication

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class Initializer : Application() {

    private lateinit var tts: TextToSpeech

    override fun onCreate() {
        super.onCreate()
        // 初始化 TTS

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale("zh", "TW")) // 繁體中文
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported or missing data")
                } else {
                    Log.d("TTS", "Text-to-Speech initialized successfully")
                }
            } else {
                Log.e("TTS", "Initialization failed")
            }
        }

    }

    // 定義 speak 函數
    fun speak(text: String) {
        if (::tts.isInitialized) { // 確保 TTS 已初始化
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Log.e("TTS", "TTS not initialized")
        }
    }

    // 提供一個方法來釋放資源
    fun shutdownTTS() {
        if (::tts.isInitialized) {
            tts.shutdown() // 釋放 TTS 資源
        }
    }
}
