package com.setotower.downloader.ui.home

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.ui.PlayerView
import androidx.navigation.fragment.findNavController
import com.setotower.downloader.R
import com.setotower.downloader.database.models.ResultItem
import com.setotower.downloader.database.viewmodel.ResultViewModel
import com.setotower.downloader.util.VideoPlayerUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResultPlayerFragment : Fragment() {
    private var player: androidx.media3.exoplayer.ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var status: TextView
    private lateinit var loading: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_result_player, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val item = if (Build.VERSION.SDK_INT >= 33) {
            arguments?.getParcelable("result", ResultItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable<ResultItem>("result")
        }
        if (item == null) {
            findNavController().navigateUp()
            return
        }

        view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.result_player_toolbar)
            .setNavigationOnClickListener { findNavController().navigateUp() }
        view.findViewById<TextView>(R.id.result_player_title).text = item.title.ifBlank { item.url }
        status = view.findViewById(R.id.result_player_status)
        loading = view.findViewById(R.id.result_player_loading)
        playerView = view.findViewById(R.id.result_player_view)

        player = VideoPlayerUtil.buildPlayer(requireContext()).also { playerView.player = it }
        load(item, ViewModelProvider(this)[ResultViewModel::class.java])
    }

    private fun load(item: ResultItem, resultViewModel: ResultViewModel) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    if (item.urls.isBlank()) {
                        resultViewModel.getStreamingUrlAndChapters(item.url).first
                    } else {
                        item.urls.split("\n").filter { it.isNotBlank() }
                    }
                }
            }.onSuccess { urls ->
                if (urls.isEmpty()) {
                    loading.visibility = View.GONE
                    status.text = "This video could not be played. You can still download it with the buttons on the result card."
                    status.visibility = View.VISIBLE
                    return@onSuccess
                }
                val exoPlayer = player ?: return@onSuccess
                if (urls.size >= 2) {
                    val factory = DefaultMediaSourceFactory(requireContext())
                    val first: MediaSource = factory.createMediaSource(MediaItem.fromUri(urls[0]))
                    val second: MediaSource = factory.createMediaSource(MediaItem.fromUri(urls[1]))
                    exoPlayer.setMediaSource(MergingMediaSource(first, second))
                } else {
                    exoPlayer.setMediaItem(MediaItem.fromUri(urls[0]))
                }
                loading.visibility = View.GONE
                status.visibility = View.GONE
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }.onFailure {
                loading.visibility = View.GONE
                status.text = "This video could not be played. You can still download it with the buttons on the result card."
                status.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        playerView.player = null
        player?.release()
        player = null
        super.onDestroyView()
    }
}
