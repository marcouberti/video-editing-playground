package com.marcouberti.kodec.ui.main

import android.media.*
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.annotation.RawRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.marcouberti.kodec.R
import com.marcouberti.kodec.findDecoderForFormat

class MainFragment : Fragment(R.layout.main_fragment), SurfaceHolder.Callback {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    // preview surface
    lateinit var surfaceView: SurfaceView
    lateinit var decoder: MediaCodec

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        surfaceView = view.findViewById(R.id.preview)
        setupSurfaceView()
        setupDecoderFromVideoSource(R.raw.paris_01_1080p)
    }

    private fun setupDecoderFromVideoSource(@RawRes videoResId: Int) {
        val extractor = MediaExtractor()
        var format = MediaFormat()
        var mimeType = ""

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
        extractor.release()
        videoFd.close()

        val decoderName = findDecoderForFormat(format)
        decoder = MediaCodec.createByCodecName(decoderName)

        // Configure the encoder
        decoder.configure(format, null, null, 0)
        println("### Input format: ${decoder.inputFormat}")
        println("### Output format: ${decoder.outputFormat}")
    }

    private fun setupSurfaceView() {
        surfaceView.holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }

}
