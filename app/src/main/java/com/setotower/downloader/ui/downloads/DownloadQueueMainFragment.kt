package com.setotower.downloader.ui.downloads

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.work.WorkManager
import com.setotower.downloader.MainActivity
import com.setotower.downloader.R
import com.setotower.downloader.database.viewmodel.DownloadCardViewModel
import com.setotower.downloader.database.viewmodel.DownloadViewModel
import com.setotower.downloader.util.Extensions.createBadge
import com.setotower.downloader.util.NavbarUtil
import com.setotower.downloader.util.NotificationUtil
import com.setotower.downloader.util.UiUtil
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class DownloadQueueMainFragment : Fragment(){
    private lateinit var downloadViewModel: DownloadViewModel
    private lateinit var downloadCardViewModel: DownloadCardViewModel
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var workManager: WorkManager
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager2: ViewPager2
    private lateinit var fragmentAdapter : DownloadListFragmentAdapter
    private lateinit var mainActivity: MainActivity
    private lateinit var notificationUtil: NotificationUtil
    private lateinit var sharedPreferences: SharedPreferences
    private var tabMediator: TabLayoutMediator? = null
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    private var selectedTabIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mainActivity = activity as? MainActivity
            ?: return inflater.inflate(R.layout.fragment_download_queue_main_screen, container, false)
        notificationUtil = NotificationUtil(mainActivity)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        return inflater.inflate(R.layout.fragment_download_queue_main_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        workManager = WorkManager.getInstance(requireContext())
        downloadViewModel = ViewModelProvider(requireActivity())[DownloadViewModel::class.java]
        downloadCardViewModel = ViewModelProvider(requireActivity())[DownloadCardViewModel::class.java]

        if (savedInstanceState != null) {
            selectedTabIndex = savedInstanceState.getInt("selected_download_tab", 0)
        }

        topAppBar = view.findViewById(R.id.downloads_toolbar)
        val isInNavBar = NavbarUtil.getNavBarItems(requireActivity()).any { n -> n.itemId == R.id.downloadQueueMainFragment && n.isVisible }
        if (isInNavBar) {
            topAppBar.navigationIcon = null
        }else{
            mainActivity.hideBottomNavigation()
        }
        topAppBar.setNavigationOnClickListener {
            mainActivity.onBackPressedDispatcher.onBackPressed()
        }

        tabLayout = view.findViewById(R.id.download_tablayout)
        viewPager2 = view.findViewById(R.id.download_viewpager)

        (viewPager2.getChildAt(0) as? RecyclerView)?.apply {
            isNestedScrollingEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val fragments = mutableListOf(
            ActiveQueuedDownloadsFragment(),
            ScheduledDownloadsFragment(),
            CancelledDownloadsFragment(),
            ErroredDownloadsFragment(),
            SavedDownloadsFragment()
        )

        fragmentAdapter = DownloadListFragmentAdapter(
            childFragmentManager,
            lifecycle,
            fragments
        )

        viewPager2.adapter = fragmentAdapter
        viewPager2.isSaveFromParentEnabled = false

        tabMediator = TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            when (position) {
                0 -> tab.text = getString(R.string.running)
                1 -> tab.text = getString(R.string.scheduled)
                2 -> tab.text = getString(R.string.cancelled)
                3 -> tab.text = getString(R.string.errored)
                4 -> tab.text = getString(R.string.saved)
            }
        }.also { it.attach() }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let { viewPager2.setCurrentItem(it.position, true) }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        pageChangeCallback = object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tabLayout.getTabAt(position)?.let { tabLayout.selectTab(it) }
                initMenu()
                selectedTabIndex = position
            }
        }.also { viewPager2.registerOnPageChangeCallback(it) }
        viewPager2.setCurrentItem(selectedTabIndex.coerceIn(0, fragments.lastIndex), false)
        initMenu()

        if (arguments?.getString("tab") != null){
            tabLayout.getTabAt(3)?.select()
            viewPager2.postDelayed( {
                viewPager2.setCurrentItem(3, false)
                val reconfigureID = arguments?.getLong("reconfigure")
                reconfigureID?.apply {
                    notificationUtil.cancelErroredNotification(this.toInt())
                    lifecycleScope.launch {
                        kotlin.runCatching {
                            val item = withContext(Dispatchers.IO){
                                downloadViewModel.getItemByID(reconfigureID)
                            }
                            downloadCardViewModel.setResultItem(downloadViewModel.createResultItemFromDownload(item))
                            downloadCardViewModel.setDownloadItem(item)
                            findNavController().navigate(R.id.downloadBottomSheetDialog, bundleOf(
                                Pair("type", item.type)
                                )
                            )
                        }
                    }

                }
            }, 200)
        }

        arguments?.clear()

        if (sharedPreferences.getBoolean("show_count_downloads", false)){
            lifecycleScope.launch {
                combine(downloadViewModel.activeDownloadsCount, downloadViewModel.queuedDownloadsCount) { active, queued -> active + queued }
                    .collectLatest {
                        tabLayout.getTabAt(0)?.apply {
                            createBadge(it)
                        }
                    }
            }
            lifecycleScope.launch {
                downloadViewModel.scheduledDownloadsCount.collectLatest {
                    tabLayout.getTabAt(1)?.apply {
                        createBadge(it)
                    }
                }
            }
            lifecycleScope.launch {
                downloadViewModel.cancelledDownloadsCount.collectLatest {
                    tabLayout.getTabAt(2)?.apply {
                        createBadge(it)
                    }
                }
            }
            lifecycleScope.launch {
                downloadViewModel.erroredDownloadsCount.collectLatest {
                    tabLayout.getTabAt(3)?.apply {
                        removeBadge()
                        if (it > 0) createBadge(it)
                    }
                }
            }
            lifecycleScope.launch {
                downloadViewModel.savedDownloadsCount.collectLatest {
                    tabLayout.getTabAt(4)?.apply {
                        removeBadge()
                        if (it > 0) createBadge(it)
                    }
                }
            }
        }

    }

    private fun initMenu() {

        topAppBar.setOnMenuItemClickListener { m: MenuItem ->
            try{
                when(m.itemId){
                    R.id.clear_all -> {
                        UiUtil.showGenericDeleteAllDialog(requireContext()) {
                            downloadViewModel.deleteAll()
                        }
                    }
                    R.id.clear_queue -> {
                        UiUtil.showGenericDeleteAllDialog(requireContext()) {
                            downloadViewModel.cancelAllDownloads()
                        }
                    }
                }
            }catch (e: Exception){
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }

            true
        }
    }

    fun scrollToActive(){
        tabLayout.getTabAt(0)?.select()
        viewPager2.setCurrentItem(0, true)
    }

    override fun onDestroyView() {
        if (::viewPager2.isInitialized) {
            pageChangeCallback?.let { viewPager2.unregisterOnPageChangeCallback(it) }
        }
        pageChangeCallback = null
        tabMediator?.detach()
        tabMediator = null
        if (::viewPager2.isInitialized) {
            viewPager2.adapter = null
        }
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("selected_download_tab", selectedTabIndex)
    }
}
