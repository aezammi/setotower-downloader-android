package com.setotower.downloader.ui.movies

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.squareup.picasso.Picasso
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.setotower.downloader.R
import com.setotower.downloader.util.CinebCatalogApi
import com.setotower.downloader.util.CinebMedia
import com.setotower.downloader.util.VideoPlayerUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CinebPlayerFragment : Fragment() {
    private lateinit var nativePlayerView: androidx.media3.ui.PlayerView
    private var player: ExoPlayer? = null
    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressUpdater = object : Runnable {
        override fun run() {
            updateProgress()
            progressHandler.postDelayed(this, 500L)
        }
    }
    private lateinit var sourceGroup: ChipGroup
    private lateinit var titleView: TextView
    private lateinit var metaView: TextView

    private lateinit var media: CinebMedia
    private var selectedUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_cineb_player, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        media = CinebMedia(
            id = requireArguments().getInt("media_id"),
            title = requireArguments().getString("media_title").orEmpty(),
            type = requireArguments().getString("media_type", "movie"),
            overview = requireArguments().getString("media_overview").orEmpty(),
            posterPath = requireArguments().getString("media_poster"),
            backdropPath = requireArguments().getString("media_backdrop"),
            releaseDate = requireArguments().getString("media_release").orEmpty(),
            voteAverage = requireArguments().getDouble("media_rating"),
            voteCount = requireArguments().getInt("media_votes")
        )

        titleView = view.findViewById(R.id.player_title)
        metaView = view.findViewById(R.id.player_meta)
        sourceGroup = view.findViewById(R.id.player_sources)
        nativePlayerView = view.findViewById(R.id.cineb_native_player)
        titleView.text = media.title
        metaView.text = "${media.releaseDate.take(4).ifBlank { "Cineb" }}  •  ${media.voteCount} votes  •  ${"%.1f".format(media.voteAverage)} rating"

        val backdrop = view.findViewById<ImageView>(R.id.player_backdrop)
        val artworkUrl = media.backdropUrl.ifBlank { media.posterUrl }
        if (artworkUrl.isNotBlank()) {
            Picasso.get().load(artworkUrl).fit().centerCrop().into(backdrop)
        }

        view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.player_toolbar)
            .setNavigationOnClickListener { findNavController().navigateUp() }
        view.findViewById<View>(R.id.player_description).setOnClickListener { showDescription() }
        view.findViewById<View>(R.id.player_comments).setOnClickListener { showComments() }
        view.findViewById<View>(R.id.player_more).setOnClickListener { showMoreOptions() }
        view.findViewById<View>(R.id.player_share).setOnClickListener { shareTitle() }
        configureCinebActions(view)

        configurePlayer()
        configureNativeControls(view)
        loadSources()
    }

    private fun configureNativeControls(view: View) {
        val controls = view.findViewById<View>(R.id.player_controls)
        val playPause = view.findViewById<ImageButton>(R.id.player_play_pause)
        val seek = view.findViewById<SeekBar>(R.id.player_seek)

        fun refreshPlayButton() {
            val playing = player?.isPlaying == true
            playPause.setImageResource(if (playing) R.drawable.ic_lyrics_pause else R.drawable.ic_lyrics_play)
            playPause.contentDescription = if (playing) "Pause" else "Play"
        }

        playPause.setOnClickListener {
            player?.let { exo ->
                if (exo.isPlaying) exo.pause() else exo.play()
                refreshPlayButton()
            }
        }
        view.findViewById<View>(R.id.player_rewind).setOnClickListener {
            player?.let { it.seekTo((it.currentPosition - 10_000L).coerceAtLeast(0L)) }
        }
        view.findViewById<View>(R.id.player_forward).setOnClickListener {
            player?.let { it.seekTo((it.currentPosition + 10_000L).coerceAtMost(it.duration.coerceAtLeast(0L))) }
        }
        view.findViewById<View>(R.id.player_settings).setOnClickListener { showMoreOptions() }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) player?.seekTo(progress.toLong())
            }
            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) = Unit
        })
        controls.setOnClickListener {
            controls.animate().alpha(if (controls.alpha > 0.5f) 0f else 1f).setDuration(180L).start()
        }
        refreshPlayButton()
        progressHandler.removeCallbacks(progressUpdater)
        progressHandler.post(progressUpdater)
    }

    private fun updateProgress() {
        val root = view ?: return
        val exo = player ?: return
        val seek = root.findViewById<SeekBar>(R.id.player_seek)
        val duration = exo.duration.coerceAtLeast(0L)
        val position = exo.currentPosition.coerceIn(0L, duration)
        seek.max = duration.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        seek.progress = position.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        root.findViewById<TextView>(R.id.player_position).text = formatTime(position)
        root.findViewById<TextView>(R.id.player_duration).text = formatTime(duration)
        root.findViewById<ImageButton>(R.id.player_play_pause).apply {
            val playing = exo.isPlaying
            setImageResource(if (playing) R.drawable.ic_lyrics_pause else R.drawable.ic_lyrics_play)
            contentDescription = if (playing) "Pause" else "Play"
        }
    }

    private fun formatTime(milliseconds: Long): String {
        val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
        return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
    }

    private fun configureCinebActions(view: View) {
        val like = view.findViewById<MaterialButton>(R.id.player_like)
        val dislike = view.findViewById<MaterialButton>(R.id.player_dislike)
        val subscribe = view.findViewById<MaterialButton>(R.id.player_subscribe)
        val active = ColorStateList.valueOf(Color.BLACK)
        val inactive = ColorStateList.valueOf(Color.rgb(70, 70, 70))

        fun refreshReactions() {
            like.iconTint = if (like.isSelected) active else inactive
            dislike.iconTint = if (dislike.isSelected) active else inactive
        }

        like.setOnClickListener {
            like.isSelected = !like.isSelected
            if (like.isSelected) dislike.isSelected = false
            refreshReactions()
        }
        dislike.setOnClickListener {
            dislike.isSelected = !dislike.isSelected
            if (dislike.isSelected) like.isSelected = false
            refreshReactions()
        }
        subscribe.setOnClickListener {
            subscribe.isSelected = !subscribe.isSelected
            subscribe.text = if (subscribe.isSelected) "Subscribed" else "Subscribe"
            subscribe.backgroundTintList = ColorStateList.valueOf(
                if (subscribe.isSelected) Color.rgb(236, 236, 236) else Color.BLACK
            )
            subscribe.setTextColor(if (subscribe.isSelected) Color.DKGRAY else Color.WHITE)
            subscribe.iconTint = if (subscribe.isSelected) inactive else ColorStateList.valueOf(Color.WHITE)
        }
        refreshReactions()
    }

    private fun configurePlayer() {
        player = ExoPlayer.Builder(requireContext()).build().also { exoPlayer ->
            nativePlayerView.player = exoPlayer
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        view?.findViewById<ImageView>(R.id.player_backdrop)?.visibility = View.GONE
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updateProgress()
                }
            })
        }
    }

    private fun loadSources() {
        sourceGroup.removeAllViews()
        addSourceChip("Finding sources…", null, true)
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { withContext(Dispatchers.IO) { CinebCatalogApi.sources(media) } }
                .onSuccess { sources ->
                    sourceGroup.removeAllViews()
                    if (sources.isEmpty()) {
                        addSourceChip("No sources available", null, true)
                    } else {
                        sources.forEachIndexed { index, source ->
                            addSourceChip(source.name, source.encodedUrl, index == 0)
                        }
                    }
                }
                .onFailure {
                    sourceGroup.removeAllViews()
                    addSourceChip("Sources unavailable", null, true)
                }
        }
    }

    private fun addSourceChip(label: String, url: String?, checked: Boolean) {
        val chip = Chip(requireContext()).apply {
            text = label
            isCheckable = url != null
            isChecked = checked && url != null
            setOnClickListener {
                if (!url.isNullOrBlank()) loadProvider(url)
            }
        }
        sourceGroup.addView(chip)
        if (checked && !url.isNullOrBlank()) loadProvider(url)
    }

    private fun loadProvider(encodedSource: String) {
        val streamUrl = CinebCatalogApi.streamUrl(media, encodedSource)
        if (streamUrl == selectedUrl) return
        selectedUrl = streamUrl
        val exoPlayer = player ?: return
        // Cineb resolves the provider through its short-lived server-side
        // helper. Media3 receives the resulting MP4 stream directly.
        runCatching {
            exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = false
        }.onFailure {
            selectedUrl = null
        }
    }

    private fun showDescription() {
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 32)
        }
        content.addView(TextView(requireContext()).apply {
            text = "Description"
            textSize = 20f
            setPadding(0, 0, 0, 16)
        })
        content.addView(TextView(requireContext()).apply {
            text = media.title
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        content.addView(TextView(requireContext()).apply {
            text = media.releaseDate.ifBlank { "Release date unavailable" }
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, 4, 0, 14)
        })
        val stats = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
        }
        listOf("${media.voteCount}", "${"%.1f".format(media.voteAverage)}", "HD").forEachIndexed { index, value ->
            stats.addView(TextView(requireContext()).apply {
                text = "$value\n${listOf("Votes", "Rating", "Quality")[index]}"
                gravity = android.view.Gravity.CENTER
                textSize = 13f
                setPadding(4, 12, 4, 12)
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.rgb(248, 239, 237))
                    cornerRadius = 14f
                }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(if (index == 0) 0 else 4, 0, if (index == 2) 0 else 4, 0)
                }
            })
        }
        content.addView(stats)
        content.addView(TextView(requireContext()).apply {
            text = media.overview.ifBlank { "No description available." }
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(12, 14, 12, 14)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(248, 239, 237))
                cornerRadius = 14f
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 14 }
        })
        if (media.posterUrl.isNotBlank()) {
            content.addView(ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (140 * resources.displayMetrics.density).toInt(),
                    (210 * resources.displayMetrics.density).toInt()
                ).apply { gravity = android.view.Gravity.CENTER }
                scaleType = ImageView.ScaleType.CENTER_CROP
                Picasso.get().load(media.posterUrl).fit().centerCrop().into(this)
            })
        }
        content.addView(TextView(requireContext()).apply {
            text = media.overview.ifBlank { "No description available." }
            textSize = 16f
            setPadding(0, 16, 0, 0)
        })
        BottomSheetDialog(requireContext()).apply {
            setContentView(content)
            show()
        }
    }

    private fun showComments() {
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }
        content.addView(TextView(requireContext()).apply {
            text = "Comments"
            textSize = 20f
        })
        content.addView(TextView(requireContext()).apply {
            text = "No comments yet\nStart the conversation."
            textSize = 16f
            setPadding(0, 24, 0, 24)
        })
        val composer = LinearLayout(requireContext()).apply {
            gravity = android.view.Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        val input = com.google.android.material.textfield.TextInputEditText(requireContext()).apply {
            hint = "Comment…"
            singleLine = true
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(241, 241, 241))
                cornerRadius = 40f
            }
            setPadding(18, 0, 18, 0)
            layoutParams = LinearLayout.LayoutParams(0, 48).apply { weight = 1f }
        }
        val send = ImageButton(requireContext()).apply {
            setImageResource(R.drawable.outline_arrow_forward_24)
            contentDescription = "Post comment"
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.rgb(87, 153, 229))
            }
            setColorFilter(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(44, 44).apply { marginStart = 8 }
        }
        composer.addView(input)
        composer.addView(send)
        content.addView(composer)
        BottomSheetDialog(requireContext()).apply {
            setContentView(content)
            show()
        }
    }

    private fun showMoreOptions() {
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 14, 8, 24)
        }
        listOf(
            R.drawable.baseline_save_24 to "Save",
            R.drawable.baseline_download_24 to "Download",
            R.drawable.baseline_stars_24 to "Thanks",
            R.drawable.baseline_error_24 to "Report"
        ).forEach { (icon, label) ->
            content.addView(LinearLayout(requireContext()).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
                orientation = LinearLayout.HORIZONTAL
                setPadding(14, 8, 14, 8)
                minimumHeight = 56
                addView(ImageView(requireContext()).apply {
                    setImageResource(icon)
                    layoutParams = LinearLayout.LayoutParams(28, 28).apply { marginEnd = 14 }
                })
                addView(TextView(requireContext()).apply {
                    text = label
                    textSize = 16f
                })
            })
        }
        BottomSheetDialog(requireContext()).apply {
            setContentView(content)
            show()
        }
    }

    private fun shareTitle() {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, media.cinebUrl)
        }, "Share title"))
    }

    override fun onDestroyView() {
        progressHandler.removeCallbacks(progressUpdater)
        nativePlayerView.player = null
        player?.release()
        player = null
        context?.let(VideoPlayerUtil::clearPlaybackCache)
        super.onDestroyView()
    }
}
