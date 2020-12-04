package com.marcouberti.kodec.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import androidx.navigation.fragment.findNavController
import com.marcouberti.kodec.R

class MainFragment : Fragment(R.layout.fragment_main) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.button1).setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_renderVideoOnSurfaceViewFragment)
        }

        view.findViewById<Button>(R.id.button2).setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_imageFromDecoderFragment)
        }

        view.findViewById<Button>(R.id.button3).setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_GLSurfaceViewFragment)
        }
    }
}