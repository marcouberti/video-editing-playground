package com.marcouberti.video.playground

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat

/**
 * Get the best encoder for give MIME type.
 */
fun selectEncoder(mimeType: String): MediaCodecInfo? {
    val codecsInfo = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
    codecsInfo.forEach { codecInfo ->
        if (codecInfo.isEncoder) {
            val types = codecInfo.supportedTypes
            for (j in types.indices) {
                if (types[j].equals(mimeType, ignoreCase = true)) {
                    return codecInfo
                }
            }
        }
    }
    return null
}

/**
 * Find a decoder for the given [MediaFormat].
 */
fun findDecoderForFormat(format: MediaFormat): String {
    return MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(format)
}
