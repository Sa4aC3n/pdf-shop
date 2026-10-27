package com.pdfcompress.CamScanner

import android.content.Context
import com.pdfcompress.CamScanner.core.common.dispatcher.DefaultDispatcherProvider
import com.pdfcompress.CamScanner.core.common.dispatcher.DispatcherProvider
import com.pdfcompress.CamScanner.core.files.SafFileHelper
import com.pdfcompress.CamScanner.core.files.TempFileManager
import com.pdfcompress.CamScanner.data.db.AppDatabase
import com.pdfcompress.CamScanner.data.engine.BackupArchiveEngineImpl
import com.pdfcompress.CamScanner.data.engine.PdfBoxDocumentEngine
import com.pdfcompress.CamScanner.data.repository.HistoryRepositoryImpl
import com.pdfcompress.CamScanner.data.repository.SettingsRepositoryImpl
import com.pdfcompress.CamScanner.domain.engine.PdfDocumentEngine
import com.pdfcompress.CamScanner.domain.repository.HistoryRepository
import com.pdfcompress.CamScanner.domain.repository.SettingsRepository
import com.pdfcompress.CamScanner.domain.usecase.AddWatermarkUseCase
import com.pdfcompress.CamScanner.domain.usecase.BackupArchiveEngine
import com.pdfcompress.CamScanner.domain.usecase.BackupRestoreUseCase
import com.pdfcompress.CamScanner.domain.usecase.ClearHistoryUseCase
import com.pdfcompress.CamScanner.domain.usecase.CompressPdfUseCase
import com.pdfcompress.CamScanner.domain.usecase.ExtractTextUseCase
import com.pdfcompress.CamScanner.domain.usecase.GetHistoryUseCase
import com.pdfcompress.CamScanner.domain.usecase.GetSettingsUseCase
import com.pdfcompress.CamScanner.domain.usecase.IdCardUseCase
import com.pdfcompress.CamScanner.domain.usecase.ImagesToPdfUseCase
import com.pdfcompress.CamScanner.domain.usecase.MergePdfsUseCase
import com.pdfcompress.CamScanner.domain.usecase.OcrUseCase
import com.pdfcompress.CamScanner.domain.usecase.PassportMrzUseCase
import com.pdfcompress.CamScanner.domain.usecase.PdfToImagesUseCase
import com.pdfcompress.CamScanner.domain.usecase.RepairPdfUseCase
import com.pdfcompress.CamScanner.domain.usecase.ScanDocumentUseCase
import com.pdfcompress.CamScanner.domain.usecase.SplitPdfUseCase
import com.pdfcompress.CamScanner.domain.usecase.UpdateSettingsUseCase
import com.pdfcompress.CamScanner.domain.usecase.RotatePdfUseCase
import com.pdfcompress.CamScanner.domain.usecase.ProtectPdfUseCase
import com.pdfcompress.CamScanner.domain.usecase.UnlockPdfUseCase
import com.pdfcompress.CamScanner.domain.usecase.PdfMetadataUseCase
import com.pdfcompress.CamScanner.domain.usecase.FlattenPdfUseCase


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

    val backupArchiveEngine: BackupArchiveEngine by lazy {
        BackupArchiveEngineImpl(context, safHelper, tempFileManager, dispatchers)
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

    val compressPdfUseCase: CompressPdfUseCase by lazy {
        CompressPdfUseCase(pdfEngine, historyRepository)
    }

    val addWatermarkUseCase: AddWatermarkUseCase by lazy {
        AddWatermarkUseCase(pdfEngine, historyRepository)
    }

    val pdfToImagesUseCase: PdfToImagesUseCase by lazy {
        PdfToImagesUseCase(pdfEngine, historyRepository)
    }

    val extractTextUseCase: ExtractTextUseCase by lazy {
        ExtractTextUseCase(pdfEngine, historyRepository)
    }

    val repairPdfUseCase: RepairPdfUseCase by lazy {
        RepairPdfUseCase(pdfEngine, historyRepository)
    }

    val scanDocumentUseCase: ScanDocumentUseCase by lazy {
        ScanDocumentUseCase(pdfEngine, historyRepository)
    }

    val ocrUseCase: OcrUseCase by lazy {
        OcrUseCase(pdfEngine, historyRepository)
    }

    val idCardUseCase: IdCardUseCase by lazy {
        IdCardUseCase(pdfEngine, historyRepository)
    }

    val passportMrzUseCase: PassportMrzUseCase by lazy {
        PassportMrzUseCase(pdfEngine, historyRepository)
    }

    val backupRestoreUseCase: BackupRestoreUseCase by lazy {
        BackupRestoreUseCase(backupArchiveEngine, historyRepository)
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

    val rotatePdfUseCase: RotatePdfUseCase by lazy {
        RotatePdfUseCase(pdfEngine, historyRepository)
    }

    val protectPdfUseCase: ProtectPdfUseCase by lazy {
        ProtectPdfUseCase(pdfEngine, historyRepository)
    }

    val unlockPdfUseCase: UnlockPdfUseCase by lazy {
        UnlockPdfUseCase(pdfEngine, historyRepository)
    }

    val pdfMetadataUseCase: PdfMetadataUseCase by lazy {
        PdfMetadataUseCase(pdfEngine, historyRepository)
    }

    val flattenPdfUseCase: FlattenPdfUseCase by lazy {
        FlattenPdfUseCase(pdfEngine, historyRepository)
    }
}