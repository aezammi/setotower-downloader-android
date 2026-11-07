package com.setotower.downloader

import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.assertion.ViewAssertions.matches
import org.junit.Assert
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

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.downloadQueueMainFragment)).perform(click())
            onView(withId(R.id.download_tablayout)).check(matches(isDisplayed()))
        }
    }
}
