package com.setotower.downloader.ui.movies

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.setotower.downloader.R
import com.setotower.downloader.ui.adapter.CinebPosterAdapter
import com.setotower.downloader.util.CinebCatalogApi
import com.setotower.downloader.util.CinebCatalogCache
import com.setotower.downloader.util.CinebMedia
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MoviesFragment : Fragment() {
    private lateinit var scroll: androidx.core.widget.NestedScrollView
    private lateinit var status: TextView
    private lateinit var search: TextInputEditText
    private lateinit var moreAdapter: CinebPosterAdapter

    private val rails = mutableMapOf<Int, CinebPosterAdapter>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_movies, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scroll = view.findViewById(R.id.movies_scroll)
        status = view.findViewById(R.id.movies_status)
        search = view.findViewById(R.id.movies_search)

        configureRail(view.findViewById(R.id.movies_featured_list))
        configureRail(view.findViewById(R.id.movies_recommended_list))
        configureRail(view.findViewById(R.id.movies_trending_list))
        configureRail(view.findViewById(R.id.movies_tv_list))

        val moreList: RecyclerView = view.findViewById(R.id.movies_more_list)
        moreAdapter = CinebPosterAdapter(emptyList(), ::openMedia, gridMode = true)
        moreList.layoutManager = GridLayoutManager(requireContext(), 3)
        moreList.adapter = moreAdapter

        search.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                searchFor(search.text?.toString().orEmpty())
                true
            } else false
        }

        val cached = CinebCatalogCache.read(requireContext())
        cached?.let { renderHome(it.groups) }
        loadHome(cached)
    }

    private fun configureRail(list: RecyclerView) {
        val adapter = CinebPosterAdapter(emptyList(), ::openMedia)
        rails[list.id] = adapter
        list.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        list.adapter = adapter
        list.isNestedScrollingEnabled = false
        list.itemAnimator = null
    }

    private fun loadHome(cached: com.setotower.downloader.util.CinebHomeSnapshot? = null) {
        val shouldRefresh = cached == null || !CinebCatalogCache.isFresh(cached)
        if (!shouldRefresh) {
            setLoading(false)
            return
        }
        if (cached == null) setLoading(true) else status.visibility = View.GONE
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { CinebCatalogApi.prefetchHome(requireContext()) }
                .onSuccess { snapshot ->
                    snapshot?.let { renderHome(it.groups) }
                    setLoading(false)
                }
                .onFailure {
                    if (cached == null) {
                        status.text = "Movies are temporarily unavailable. Try again."
                        status.visibility = View.VISIBLE
                    }
                    setLoading(false)
                }
        }
    }

    private fun renderHome(groups: List<List<CinebMedia>>) {
        if (groups.size < 5) return
        updateRail(R.id.movies_featured_list, groups[0])
        updateRail(R.id.movies_recommended_list, groups[1])
        updateRail(R.id.movies_trending_list, groups[2])
        updateRail(R.id.movies_tv_list, groups[3])
        moreAdapter.submitList(groups[4])
    }

    private fun searchFor(query: String) {
        if (query.isBlank()) {
            val cached = CinebCatalogCache.read(requireContext())
            cached?.let { renderHome(it.groups) }
            loadHome(cached)
            return
        }
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { withContext(Dispatchers.IO) { CinebCatalogApi.search(query.trim()) } }
                .onSuccess {
                    status.text = if (it.isEmpty()) "No movies found" else "Search results"
                    status.visibility = View.VISIBLE
                    moreAdapter.submitList(it)
                    scroll.smoothScrollTo(0, status.top)
                    setLoading(false)
                }
                .onFailure {
                    status.text = "Search failed. Try another title."
                    status.visibility = View.VISIBLE
                    setLoading(false)
                }
        }
    }

    private fun setLoading(loading: Boolean) {
        if (loading) {
            status.text = "Loading movies…"
            status.visibility = View.VISIBLE
        } else if (status.text == "Loading movies…") {
            status.visibility = View.GONE
        }
    }

    private fun updateRail(id: Int, items: List<CinebMedia>) {
        rails[id]?.submitList(items)
    }

    private fun openMedia(media: CinebMedia) {
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.downloadQueueMainFragment) return
        runCatching {
            navController.navigate(
                R.id.action_downloadQueueMainFragment_to_cinebPlayerFragment,
                bundleOf(
                    "media_id" to media.id,
                    "media_title" to media.title,
                    "media_type" to media.type,
                    "media_overview" to media.overview,
                    "media_poster" to media.posterPath,
                    "media_backdrop" to media.backdropPath,
                    "media_release" to media.releaseDate,
                    "media_rating" to media.voteAverage,
                    "media_votes" to media.voteCount
                )
            )
        }
    }

    fun scrollToTop() {
        if (::scroll.isInitialized) scroll.smoothScrollTo(0, 0)
    }
}
