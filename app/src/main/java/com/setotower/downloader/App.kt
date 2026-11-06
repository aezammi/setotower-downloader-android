package com.setotower.downloader

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.webkit.WebView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceManager
import com.setotower.downloader.core.RuntimeManager
import com.setotower.downloader.core.models.ExecuteException
import com.setotower.downloader.database.DBManager
import com.setotower.downloader.database.repository.ObserveSourcesRepository
import com.setotower.downloader.services.BgUtilsPoTokenGeneratorService
import com.setotower.downloader.util.ApkInstallUtil
import com.setotower.downloader.util.BgUtilsPoTokenGeneratorUtil
import com.setotower.downloader.util.Extensions.hasReachedEnd
import com.setotower.downloader.util.NotificationUtil
import com.setotower.downloader.util.ObserveAlarmScheduler
import com.setotower.downloader.util.ThemeUtil
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        val sharedPreferences =  PreferenceManager.getDefaultSharedPreferences(this@App)
        setDefaultValues()
        ThemeUtil.init(this)

        val processName = getProcessNameImpl()
        if (processName.endsWith(":incognito_process") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WebView.setDataDirectorySuffix("incognito_store")
            return
        }

        applicationScope = CoroutineScope(SupervisorJob())
        applicationScope.launch((Dispatchers.IO)) {
            try {
                createNotificationChannels()
                initLibraries()

                val appVer = sharedPreferences.getString("version", "")!!
                if(appVer.isEmpty() || appVer != BuildConfig.VERSION_NAME){
                    sharedPreferences.edit(commit = true){
                        putString("version", BuildConfig.VERSION_NAME)
                    }
                }

                val db = DBManager.getInstance(this@App)
                val scheduler = ObserveAlarmScheduler(this@App)
                db.observeSourcesDao.getAllSources()
                    .filter { it.status == ObserveSourcesRepository.SourceStatus.ACTIVE && !it.hasReachedEnd() }
                    .forEach { scheduler.schedule(it) }         // idempotent: FLAG_UPDATE_CURRENT updates in place

            }catch (e: Exception){
                Looper.prepare().runCatching {
                    Toast.makeText(this@App, e.message, Toast.LENGTH_SHORT).show()
                }
                e.printStackTrace()
            }
        }
    }
    @Throws(ExecuteException::class)
    private fun initLibraries() {
        RuntimeManager.getInstance().init(this)
    }

    private fun setDefaultValues(){
        val SPL = 1
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        if (sp.getInt("spl", 0) != SPL) {
            PreferenceManager.setDefaultValues(this, R.xml.root_preferences, true)
            PreferenceManager.setDefaultValues(this, R.xml.downloading_preferences, true)
            PreferenceManager.setDefaultValues(this, R.xml.general_preferences, true)
            PreferenceManager.setDefaultValues(this, R.xml.processing_preferences, true)
            PreferenceManager.setDefaultValues(this, R.xml.folders_preference, true)
            PreferenceManager.setDefaultValues(this, R.xml.updating_preferences, true)
            PreferenceManager.setDefaultValues(this, R.xml.ytdlp_update_preferences, true)
            PreferenceManager.setDefaultValues(this, R.xml.app_update_preferences, true)
            PreferenceManager.setDefaultValues(this, R.xml.advanced_preferences, true)
            sp.edit().putInt("spl", SPL).apply()
        }

    }

    private fun createNotificationChannels() {
        val notificationUtil = NotificationUtil(this)
        notificationUtil.createNotificationChannel()
    }

    private fun getProcessNameImpl() : String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getProcessName()
        } else {
            packageName
        }
    }

    companion object {
        private const val TAG = "App"
        private lateinit var applicationScope: CoroutineScope
        lateinit var instance: App
    }
}