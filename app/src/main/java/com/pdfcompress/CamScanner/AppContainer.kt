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
import com.pdfcompress.CamScanner.domain.usecase.OrganizePagesUseCase
import com.pdfcompress.CamScanner.domain.usecase.AddPageNumbersUseCase
import com.pdfcompress.CamScanner.domain.usecase.GrayscalePdfUseCase
import com.pdfcompress.CamScanner.domain.usecase.CropPdfUseCase
import com.pdfcompress.CamScanner.domain.usecase.RedactPdfUseCase
import com.pdfcompress.CamScanner.domain.usecase.TextToPdfUseCase
import com.pdfcompress.CamScanner.domain.usecase.MarkdownToPdfUseCase
import com.pdfcompress.CamScanner.domain.usecase.HtmlToPdfUseCase
import com.pdfcompress.CamScanner.domain.usecase.RtfToPdfUseCase
import com.pdfcompress.CamScanner.domain.usecase.PdfToMarkdownUseCase
import com.pdfcompress.CamScanner.domain.usecase.ImageStudioUseCase


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

    val organizePagesUseCase: OrganizePagesUseCase by lazy {
        OrganizePagesUseCase(pdfEngine, historyRepository)
    }

    val addPageNumbersUseCase: AddPageNumbersUseCase by lazy {
        AddPageNumbersUseCase(pdfEngine, historyRepository)
    }

    val grayscalePdfUseCase: GrayscalePdfUseCase by lazy {
        GrayscalePdfUseCase(pdfEngine, historyRepository)
    }

    val cropPdfUseCase: CropPdfUseCase by lazy {
        CropPdfUseCase(pdfEngine, historyRepository)
    }

    val redactPdfUseCase: RedactPdfUseCase by lazy {
        RedactPdfUseCase(pdfEngine, historyRepository)
    }

    val textToPdfUseCase: TextToPdfUseCase by lazy {
        TextToPdfUseCase(pdfEngine, historyRepository)
    }

    val markdownToPdfUseCase: MarkdownToPdfUseCase by lazy {
        MarkdownToPdfUseCase(pdfEngine, historyRepository)
    }

    val htmlToPdfUseCase: HtmlToPdfUseCase by lazy {
        HtmlToPdfUseCase(pdfEngine, historyRepository)
    }

    val rtfToPdfUseCase: RtfToPdfUseCase by lazy {
        RtfToPdfUseCase(pdfEngine, historyRepository)
    }

    val pdfToMarkdownUseCase: PdfToMarkdownUseCase by lazy {
        PdfToMarkdownUseCase(pdfEngine, historyRepository)
    }

    val imageStudioUseCase: ImageStudioUseCase by lazy {
        ImageStudioUseCase(pdfEngine, historyRepository)
    }
}