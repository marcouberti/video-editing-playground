package com.marcouberti.kodec

import android.util.Log

/**
 * Logging utility method.
 */
fun Any.log(msg: String, tag: String = this::class.java.simpleName) {
    Log.d("Kodec - $tag", msg)
}