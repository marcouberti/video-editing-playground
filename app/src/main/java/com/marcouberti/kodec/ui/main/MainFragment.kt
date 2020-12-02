package com.marcouberti.kodec.ui.main

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.annotation.RawRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.marcouberti.kodec.R
import com.marcouberti.kodec.findDecoderForFormat
import com.marcouberti.kodec.log

class MainFragment : Fragment(R.layout.main_fragment), SurfaceHolder.Callback {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    // preview surface
    lateinit var surfaceView: SurfaceView
    private var surfaceAvailable = false

    lateinit var extractor: MediaExtractor
    lateinit var decoder: MediaCodec
    private var extractorDone = false

    override fun onDestroyView() {
        super.onDestroyView()
        release()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        surfaceView = view.findViewById(R.id.preview)

        setupSurfaceView()
    }

    private fun onSurfaceAvailable(surface: Surface) {
        val format = createDecoderFromVideoSource(R.raw.paris_01_1080p)
        setupDecoderCallback()
        configureDecoder(format, surface)
        startDecoder()
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

                if (extractorDone || !surfaceAvailable) {
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

            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                log("onOutputBufferAvailable index: $index, info: $info")

                if (extractorDone || !surfaceAvailable) {
                    return
                }

                val image = codec.getOutputImage(index)

                log("Image data: ${image?.width} x ${image?.height}")

                if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    codec.releaseOutputBuffer(index, false)
                    return
                }

                val render = info.size != 0
                codec.releaseOutputBuffer(index, render)

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
        log("release extractor")
        extractor.release()
        log("release decoder")
        decoder.release()
    }

    private fun configureDecoder(format: MediaFormat, surface: Surface) {
        // Configure the decoder
        // flags to 0 for decoding, CONFIGURE_FLAG_ENCODE for encoding
        decoder.configure(format, surface, null, 0)

        log("Input format: ${decoder.inputFormat}")
        log("Output format: ${decoder.outputFormat}")
    }

    private fun setupSurfaceView() {
        surfaceView.holder.addCallback(this)
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                surfaceAvailable = true
                onSurfaceAvailable(holder.surface)
                log("onSurfaceAvailable")
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                surfaceAvailable = false
                log("surfaceDestroyed")
            }

        })
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }
}
