package com.setotower.downloader

import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.navigation.findNavController
import android.view.View
import com.google.android.material.navigation.NavigationView
import com.google.android.material.navigation.NavigationBarView
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    private fun openQueue(activity: MainActivity) {
        val bottomNavigation = activity.findViewById<NavigationBarView>(R.id.bottomNavigationView)
        val bottomItem = bottomNavigation?.menu?.findItem(R.id.downloadQueueMainFragment)
        if (bottomNavigation != null && bottomItem != null && bottomItem.isVisible) {
            bottomNavigation.selectedItemId = R.id.downloadQueueMainFragment
            return
        }

        val largeScreenNavigation = activity.findViewById<NavigationView>(R.id.navigationView)
        val largeScreenItem = largeScreenNavigation?.menu?.findItem(R.id.downloadQueueMainFragment)
        if (largeScreenNavigation != null && largeScreenItem != null && largeScreenItem.isVisible) {
            largeScreenNavigation.menu.performIdentifierAction(R.id.downloadQueueMainFragment, 0)
            return
        }

        // Queue may be intentionally hidden from the navigation bar. Use the same
        // NavController route that the More screen uses in that configuration.
        activity.findNavController(R.id.frame_layout).navigate(R.id.downloadQueueMainFragment)
    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        Assert.assertEquals("com.setotower.downloader", appContext.packageName)
    }

    @Test
    fun appIdentityIsSetotowerBranded() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        Assert.assertEquals("Setotower Downloader", appContext.getString(R.string.app_name))
        Assert.assertFalse(appContext.applicationInfo.loadLabel(appContext.packageManager).toString().contains("YTDLnis"))
    }

    @Test
    fun queueTabOpensWithoutCrashing() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(appContext)
            .edit()
            .putBoolean("welcome_completed", true)
            .apply()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                openQueue(activity)
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                val tabs = activity.findViewById<View>(R.id.download_tablayout)
                assertNotNull("Queue tabs should be created after opening Queue", tabs)
                assertEquals(View.VISIBLE, tabs.visibility)
            }
        }
    }

    @Test
    fun queueTabSurvivesActivityRecreation() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(appContext)
            .edit()
            .putBoolean("welcome_completed", true)
            .putString("navigation_bar", "0,1,-3,2")
            .apply()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                openQueue(activity)
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.recreate()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                val tabs = activity.findViewById<View>(R.id.download_tablayout)
                assertNotNull("Queue tabs should survive Activity recreation", tabs)
                assertEquals(View.VISIBLE, tabs.visibility)
            }
        }
    }

    @Test
    fun queueOpensFromMoreWithoutCrashing() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(appContext)
            .edit()
            .putBoolean("welcome_completed", true)
            .apply()

        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    activity.findViewById<View>(R.id.moreFragment).performClick()
                }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()

                scenario.onActivity { activity ->
                    activity.findViewById<View>(R.id.download_queue).performClick()
                }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()

                scenario.onActivity { activity ->
                    val tabs = activity.findViewById<View>(R.id.download_tablayout)
                    assertNotNull("Queue tabs should be created from More", tabs)
                    assertEquals(View.VISIBLE, tabs.visibility)
                }
            }
        } finally {
            PreferenceManager.getDefaultSharedPreferences(appContext)
                .edit()
                .remove("navigation_bar")
                .apply()
        }
    }

    @Test
    fun downloadsAndMoviesTabsOpenWithoutCrashing() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(appContext)
            .edit()
            .putBoolean("welcome_completed", true)
            .apply()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val bottomNavigation = activity.findViewById<NavigationBarView>(R.id.bottomNavigationView)
                bottomNavigation.selectedItemId = R.id.historyFragment
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertNotNull(activity.findViewById<View>(R.id.download_tablayout))
                activity.findViewById<NavigationBarView>(R.id.bottomNavigationView)
                    .selectedItemId = R.id.downloadQueueMainFragment
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertNotNull(activity.findViewById<View>(R.id.movies_scroll))
            }
        }
    }
}
