package com.example.util

import android.util.Log
import cu.daniel.anical.BuildConfig

object AppLogger {
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message) // mobsf-ignore: android_kotlin_logging
        }
    }

    fun w(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message) // mobsf-ignore: android_kotlin_logging
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.e(tag, message, throwable) // mobsf-ignore: android_kotlin_logging
            } else {
                Log.e(tag, message) // mobsf-ignore: android_kotlin_logging
            }
        }
    }
}
