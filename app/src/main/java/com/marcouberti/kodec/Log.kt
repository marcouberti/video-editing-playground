package com.marcouberti.kodec

import android.util.Log

/**
 * Logging utility method.
 *
 * Uses as TAG the library name and the class name for easy filtering.
 */
fun Any.log(msg: String, tag: String = this::class.java.simpleName) {
    Log.d("Kodec - $tag", msg)
}