package com.setotower.downloader.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.setotower.downloader.database.dao.CommandTemplateDao
import com.setotower.downloader.database.dao.CookieDao
import com.setotower.downloader.database.dao.DownloadDao
import com.setotower.downloader.database.dao.HistoryDao
import com.setotower.downloader.database.dao.LogDao
import com.setotower.downloader.database.dao.ObserveSourcesDao
import com.setotower.downloader.database.dao.ResultDao
import com.setotower.downloader.database.dao.SearchHistoryDao
import com.setotower.downloader.database.dao.TerminalDao
import com.setotower.downloader.database.models.CommandTemplate
import com.setotower.downloader.database.models.CookieItem
import com.setotower.downloader.database.models.DownloadItem
import com.setotower.downloader.database.models.HistoryItem
import com.setotower.downloader.database.models.LogItem
import com.setotower.downloader.database.models.ResultItem
import com.setotower.downloader.database.models.SearchHistoryItem
import com.setotower.downloader.database.models.TemplateShortcut
import com.setotower.downloader.database.models.TerminalItem
import com.setotower.downloader.database.models.observeSources.ObserveSourcesItem

@TypeConverters(Converters::class)
@Database(
    entities = [
        ResultItem::class,
        HistoryItem::class,
        DownloadItem::class,
        CommandTemplate::class,
        SearchHistoryItem::class,
        TemplateShortcut::class,
        CookieItem::class,
        LogItem::class,
        TerminalItem::class,
        ObserveSourcesItem::class
   ],
    version = 29,
    autoMigrations = [
        AutoMigration (from = 1, to = 2),
        AutoMigration (from = 2, to = 3),
        AutoMigration (from = 3, to = 4),
        AutoMigration (from = 4, to = 5),
        AutoMigration (from = 5, to = 6),
        AutoMigration (from = 6, to = 7),
        AutoMigration (from = 7, to = 8),
        AutoMigration (from = 8, to = 9),
        AutoMigration (from = 9, to = 10),
        AutoMigration (from = 10, to = 11),
        AutoMigration (from = 11, to = 12),
        AutoMigration (from = 12, to = 13),
        // AutoMigration (from = 13, to = 14) MANUALLY HANDLED
        AutoMigration (from = 14, to = 15),
        AutoMigration (from = 15, to = 16, spec = Migrations.resetObserveSources::class),
        AutoMigration (from = 16, to = 17),
        AutoMigration (from = 17, to = 18),
        AutoMigration (from = 18, to = 19),
        AutoMigration (from = 19, to = 20),
        //AutoMigration (from = 20, to = 21) MANUALLY HANDLED
        //AutoMigration(from = 21, to = 22) MANUALLY HANDLED
        //AutoMigration(from = 22, to = 23) MANUALLY HANDLED
        //AutoMigration(from = 23, to = 24) MANUALLY HANDLED
        //AutoMigration(from = 24, to = 25) MANUALLY HANDLED
        //AutoMigration(from = 25, to = 26) MANUALLY HANDLED
        AutoMigration(from = 26, to = 27),
        AutoMigration(from = 27, to = 28),
        AutoMigration(from = 28, to = 29)
    ]
)
abstract class DBManager : RoomDatabase(){
    abstract val resultDao : ResultDao
    abstract val historyDao : HistoryDao
    abstract val downloadDao : DownloadDao
    abstract val commandTemplateDao : CommandTemplateDao
    abstract val searchHistoryDao: SearchHistoryDao
    abstract val cookieDao: CookieDao
    abstract val logDao: LogDao
    abstract val terminalDao: TerminalDao
    abstract val observeSourcesDao: ObserveSourcesDao

    enum class SORTING{
        DESC, ASC
    }

    companion object {
        //prevents multiple instances of db getting created at the same time
        @Volatile
        private var instance : DBManager? = null
        //if its not null return it, otherwise create db
        fun getInstance(context: Context) : DBManager {
            return instance ?: synchronized(this){

                val dbInstance = Room.databaseBuilder(
                    context.applicationContext,
                    DBManager::class.java,
                    "Setotower DownloaderDatabase"
                )
                    .addTypeConverter(Converters())
                    .addMigrations(*Migrations.migrationList)
                    .enableMultiInstanceInvalidation()
                    .build()
                instance = dbInstance
                dbInstance
            }
        }

    }

}