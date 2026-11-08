package com.setotower.downloader.ui.welcome

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.setotower.downloader.R

class WelcomeIntroFragment : Fragment(R.layout.fragment_welcome_intro) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.get_started).setOnClickListener {
            (requireActivity() as WelcomeActivity).goTo(1)
        }
    }
}
