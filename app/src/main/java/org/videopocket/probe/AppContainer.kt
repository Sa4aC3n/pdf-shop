package org.videopocket.probe

import android.content.Context
import org.videopocket.probe.core.common.dispatcher.DefaultDispatcherProvider
import org.videopocket.probe.core.common.dispatcher.DispatcherProvider
import org.videopocket.probe.core.files.SafFileHelper
import org.videopocket.probe.core.files.TempFileManager
import org.videopocket.probe.data.db.AppDatabase
import org.videopocket.probe.data.engine.PdfBoxDocumentEngine
import org.videopocket.probe.data.repository.HistoryRepositoryImpl
import org.videopocket.probe.data.repository.SettingsRepositoryImpl
import org.videopocket.probe.domain.engine.PdfDocumentEngine
import org.videopocket.probe.domain.repository.HistoryRepository
import org.videopocket.probe.domain.repository.SettingsRepository
import org.videopocket.probe.domain.usecase.ClearHistoryUseCase
import org.videopocket.probe.domain.usecase.GetHistoryUseCase
import org.videopocket.probe.domain.usecase.GetSettingsUseCase
import org.videopocket.probe.domain.usecase.ImagesToPdfUseCase
import org.videopocket.probe.domain.usecase.MergePdfsUseCase
import org.videopocket.probe.domain.usecase.SplitPdfUseCase
import org.videopocket.probe.domain.usecase.UpdateSettingsUseCase

class AppContainer(private val context: Context) {

    val dispatchers: DispatcherProvider by lazy { DefaultDispatcherProvider() }
    val safHelper: SafFileHelper by lazy { SafFileHelper(context) }
    val tempFileManager: TempFileManager by lazy { TempFileManager(context) }

    private val database: AppDatabase by lazy { AppDatabase.getInstance(context) }

    val pdfEngine: PdfDocumentEngine by lazy {
        PdfBoxDocumentEngine(
            context = context,
            safHelper = safHelper,
            tempFileManager = tempFileManager,
            dispatchers = dispatchers
        )
    }

    val historyRepository: HistoryRepository by lazy {
        HistoryRepositoryImpl(database.operationDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(context)
    }

    val mergePdfsUseCase: MergePdfsUseCase by lazy {
        MergePdfsUseCase(pdfEngine, historyRepository)
    }

    val imagesToPdfUseCase: ImagesToPdfUseCase by lazy {
        ImagesToPdfUseCase(pdfEngine, historyRepository)
    }

    val splitPdfUseCase: SplitPdfUseCase by lazy {
        SplitPdfUseCase(pdfEngine, historyRepository)
    }

    val getHistoryUseCase: GetHistoryUseCase by lazy {
        GetHistoryUseCase(historyRepository)
    }

    val clearHistoryUseCase: ClearHistoryUseCase by lazy {
        ClearHistoryUseCase(historyRepository)
    }

    val getSettingsUseCase: GetSettingsUseCase by lazy {
        GetSettingsUseCase(settingsRepository)
    }

    val updateSettingsUseCase: UpdateSettingsUseCase by lazy {
        UpdateSettingsUseCase(settingsRepository)
    }
}
