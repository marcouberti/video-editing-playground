package com.marcouberti.kodec.ui.extractimages

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import android.view.View
import androidx.annotation.RawRes
import androidx.fragment.app.Fragment
import com.marcouberti.kodec.R
import com.marcouberti.kodec.findDecoderForFormat
import com.marcouberti.kodec.log
import java.io.File

/**
 * Decodes a mp4 file and renders it on a SurfaceTexture (no UI/View), reads each frame
 * [Image] and converts it to a [Bitmap].
 *
 * Then save the frames as PNGs (slow).
 */
class ExtractImagesFragment : Fragment(R.layout.extract_images_fragment) {

    lateinit var extractor: MediaExtractor
    lateinit var decoder: MediaCodec
    private var extractorDone = false
    lateinit var outputSurface: CodecOutputSurface

    val MAX_FRAME = 10
    var counter = 0

    override fun onDestroyView() {
        super.onDestroyView()
        release()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val format = createDecoderFromVideoSource(R.raw.paris_01_1080p)
        setupDecoderCallback()
        setupOutputSurface()
        configureDecoder(format)
        startDecoder()
    }

    private fun setupOutputSurface() {
        // Could use width/height from the MediaFormat to get full-size frames.
        outputSurface = CodecOutputSurface(640, 480)
    }

    private fun startDecoder() {
        decoder.start()
        log("startDecoder")
    }

    private fun setupDecoderCallback() {
        // Use null to clear a previously set callback before configure
        decoder.setCallback(null)
        decoder.setCallback(object : MediaCodec.Callback() {
            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                log("onInputBufferAvailable index: $index")

                if (extractorDone) {
                    return
                }

                val buffer = codec.getInputBuffer(index) ?: return

                val size: Int = extractor.readSampleData(buffer, 0)
                val pts: Long = extractor.sampleTime

                extractorDone = !extractor.advance()

                if (size >= 0) {
                    var flags = extractor.sampleFlags
                    if (extractorDone) {
                        log("extractor done")
                        flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    }
                    codec.queueInputBuffer(index, 0, size, pts, flags)
                }
            }

            override fun onOutputBufferAvailable(
                codec: MediaCodec,
                index: Int,
                info: MediaCodec.BufferInfo
            ) {
                log("onOutputBufferAvailable index: $index, info: $info")

                if (extractorDone) {
                    return
                }

                //val image = codec.getOutputImage(index)
                //log("Image data: ${image?.width} x ${image?.height}")

                if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    codec.releaseOutputBuffer(index, false)
                    return
                }

                val render = info.size != 0

                // As soon as we call releaseOutputBuffer, the buffer will be forwarded
                // to SurfaceTexture to convert to a texture.  The API doesn't guarantee
                // that the texture will be available before the call returns, so we
                // need to wait for the onFrameAvailable callback to fire.
                codec.releaseOutputBuffer(index, render)

                // extract frame pixels
                if (render) {
                    log("awaiting decode of frame $index")
                    try {
                        outputSurface.awaitNewImage()
                    } catch (e: Throwable) {
                        return
                    }
                    outputSurface.drawImage(true)

                    if (counter < MAX_FRAME) {
                        val filename = String.format("frame-%02d.png", counter)
                        val outputFile: File =
                            File(requireContext().cacheDir, filename)
                        outputSurface.saveFrame(outputFile.toString())
                        log("saved frame $filename")
                        counter++
                    }
                }

                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    log("received BUFFER_FLAG_END_OF_STREAM")
                    release()
                }
            }

            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                log("onError error: $e")
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                log("onOutputFormatChanged format: $format")
            }

        })
    }

    private fun createDecoderFromVideoSource(@RawRes videoResId: Int): MediaFormat {
        var format = MediaFormat()
        var mimeType = ""

        extractor = MediaExtractor()

        // Load file from raw directory
        val videoFd = this.resources.openRawResourceFd(videoResId)
        extractor.setDataSource(videoFd.fileDescriptor, videoFd.startOffset, videoFd.length)

        // Find the MIME type from the raw video file
        for (i in 0 until extractor.trackCount) {
            format = extractor.getTrackFormat(i)
            // Use this in API 21+ as stated by the documentation, but crashes the app
            // format.setString(MediaFormat.KEY_FRAME_RATE, null)
            mimeType = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mimeType.startsWith("video/")) {
                extractor.selectTrack(i)
                break
            }
        }
        //extractor.release()
        videoFd.close()

        val decoderName = findDecoderForFormat(format)
        decoder = MediaCodec.createByCodecName(decoderName)

        return format
    }

    private fun release() {
        log("release surface")
        outputSurface.release()
        log("release extractor")
        extractor.release()
        log("release decoder")
        decoder.release()
    }

    private fun configureDecoder(format: MediaFormat) {
        // Configure the decoder
        // flags to 0 for decoding, CONFIGURE_FLAG_ENCODE for encoding
        decoder.configure(format, outputSurface.surface, null, 0)

        log("Input format: ${decoder.inputFormat}")
        log("Output format: ${decoder.outputFormat}")
    }
}
