package com.setotower.downloader.work.background

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.setotower.downloader.App
import com.setotower.downloader.BuildConfig
import com.setotower.downloader.R
import com.setotower.downloader.core.RuntimeManager
import com.setotower.downloader.util.FileUtil
import com.setotower.downloader.util.NotificationUtil
import com.setotower.downloader.util.SetotowerProductPolicy
import com.setotower.downloader.util.UpdateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class UpdateCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val updateUtil = UpdateUtil(App.instance)
        val notificationUtil = NotificationUtil(App.instance)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val checkUpdatesApp = sharedPreferences.getBoolean("update_app", true)
        val checkUpdatesYTDL = sharedPreferences.getBoolean("auto_update_ytdlp", true)

        if (checkUpdatesApp) {
            val newRelease = updateUtil.tryGetNewVersion()
            if (newRelease.isSuccess && BuildConfig.FLAVOR == "github") {
                notificationUtil.showNewAppUpdate(newRelease.getOrNull()!!.tag_name)
            }

            if (SetotowerProductPolicy.allowEngineMaintenanceSettings) {
                val skipRemindingPackageUpdate = sharedPreferences.getStringSet("skip_reminding_package_update", setOf())!!.toMutableSet()
                RuntimeManager.packages.forEach { pkg ->
                    val instance = pkg.plugin.getInstance()
                    if (instance.bundledVersion.isNullOrBlank() && instance.downloadedVersion.isNullOrBlank()) return@forEach
                    val biggestVersionPresent : Int? = listOf(instance.downloadedVersion, instance.bundledVersion)
                        .map { it?.replace("v", "")?.toInt() }.sortedByDescending { it }.first()

                    instance.getReleases().apply {
                        val releases = this.getOrElse { listOf() }
                        if (releases.isEmpty()) return@apply

                        val latestRelease = releases.first()
                        if (latestRelease.isBundled || latestRelease.isInstalled) return@apply
                        if (skipRemindingPackageUpdate.contains(latestRelease.tag_name)) return@apply

                        if (biggestVersionPresent != null && latestRelease.tag_name.split("-")[1].toInt() <= biggestVersionPresent) {
                            return@apply
                        }
                        notificationUtil.showNewPackageUpdate(pkg.title, latestRelease.tag_name)

                        skipRemindingPackageUpdate.add(latestRelease.tag_name)
                        sharedPreferences.edit().putStringSet("skip_reminding_package_update", skipRemindingPackageUpdate).apply()
                    }
                }
            }
        }

        if (checkUpdatesYTDL && SetotowerProductPolicy.allowEngineMaintenanceSettings) {
            val res = updateUtil.updateYTDL()
            when (res.status) {
                UpdateUtil.YTDLPUpdateStatus.DONE -> {
                    withContext(Dispatchers.IO) {
                        File(FileUtil.getInfoJsonPath(context)).deleteRecursively()
                    }
                }

                else -> {}
            }
        }

        return Result.success()
    }

    companion object {

        const val WORK_NAME = "setotower_update_check"

        fun schedule(context: Context, intervalMinutes: Long = 24 * 60L) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                intervalMinutes, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInitialDelay(intervalMinutes, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }

}
