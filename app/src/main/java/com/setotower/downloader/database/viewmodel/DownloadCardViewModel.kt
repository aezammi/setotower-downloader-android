package com.setotower.downloader.database.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.setotower.downloader.R
import com.setotower.downloader.database.DBManager
import com.setotower.downloader.database.enums.DownloadType
import com.setotower.downloader.database.models.DownloadItem
import com.setotower.downloader.database.models.Format
import com.setotower.downloader.database.models.ResultItem
import com.setotower.downloader.database.repository.DownloadRepository
import com.setotower.downloader.ui.downloadcard.FormatSelectionBottomSheetDialog.FormatCategory
import com.setotower.downloader.ui.downloadcard.FormatSelectionBottomSheetDialog.FormatSorting
import com.setotower.downloader.ui.downloadcard.FormatTuple
import com.setotower.downloader.ui.downloadcard.MultipleItemFormatTuple
import com.setotower.downloader.util.Extensions.isYoutubeURL
import com.setotower.downloader.util.FileUtil
import com.setotower.downloader.util.FormatUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File

class DownloadCardViewModel(application: Application) : AndroidViewModel(application) {
    var resultItem: ResultItem? = null
        private set

    var downloadItem: DownloadItem? = null
        private set

    fun setDownloadItem(item: DownloadItem?) {
        downloadItem = item
    }

    fun setResultItem(item: ResultItem?) {
        resultItem = item
    }
}