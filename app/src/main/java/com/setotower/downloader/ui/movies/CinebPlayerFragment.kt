package com.setotower.downloader.ui.movies

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.setotower.downloader.R
import com.setotower.downloader.util.CinebCatalogApi
import com.setotower.downloader.util.CinebMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CinebPlayerFragment : Fragment() {
    private lateinit var webPlayer: WebView
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
        webPlayer = view.findViewById(R.id.cineb_web_player)
        titleView.text = media.title
        metaView.text = "${media.releaseDate.take(4).ifBlank { "Cineb" }}  •  ${media.voteCount} votes  •  ${"%.1f".format(media.voteAverage)} rating"

        view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.player_toolbar)
            .setNavigationOnClickListener { findNavController().navigateUp() }
        view.findViewById<View>(R.id.player_description).setOnClickListener { showDescription() }
        view.findViewById<View>(R.id.player_comments).setOnClickListener { showComments() }
        view.findViewById<View>(R.id.player_more).setOnClickListener { showMoreOptions() }
        view.findViewById<View>(R.id.player_share).setOnClickListener { shareTitle() }

        configurePlayer()
        loadSources()
    }

    private fun configurePlayer() {
        webPlayer.settings.javaScriptEnabled = true
        webPlayer.settings.domStorageEnabled = true
        webPlayer.settings.mediaPlaybackRequiresUserGesture = true
        webPlayer.settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        webPlayer.webChromeClient = WebChromeClient()
        webPlayer.webViewClient = WebViewClient()
        webPlayer.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.black))
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
                            val decoded = runCatching {
                                String(Base64.decode(source.encodedUrl, Base64.DEFAULT), Charsets.UTF_8)
                            }.getOrNull()
                            addSourceChip(source.name, decoded, index == 0)
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

    private fun loadProvider(url: String) {
        if (url == selectedUrl) return
        selectedUrl = url
        webPlayer.loadUrl(url)
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
            text = media.overview.ifBlank { "No description available." }
            textSize = 16f
        })
        BottomSheetDialog(requireContext()).apply {
            setContentView(content)
            show()
        }
    }

    private fun showComments() {
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 40)
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
        BottomSheetDialog(requireContext()).apply {
            setContentView(content)
            show()
        }
    }

    private fun showMoreOptions() {
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 32)
        }
        listOf("Save", "Download", "Report").forEach { label ->
            content.addView(MaterialButton(requireContext()).apply {
                text = label
                setOnClickListener { }
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
        webPlayer.stopLoading()
        webPlayer.destroy()
        super.onDestroyView()
    }
}
