package com.setotower.downloader

import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.view.View
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
                activity.findViewById<View>(R.id.downloadQueueMainFragment).performClick()
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
            .apply()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<View>(R.id.downloadQueueMainFragment).performClick()
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
}
