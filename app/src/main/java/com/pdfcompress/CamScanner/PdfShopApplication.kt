package com.pdfcompress.CamScanner

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class PdfShopApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize Google Mobile Ads SDK (AdMob)
        try {
            MobileAds.initialize(this) {}
        } catch (_: Exception) {}

        // Initialize PDFBox resource loader for font mapping and asset rendering
        PDFBoxResourceLoader.init(applicationContext)

        // Initialize Dependency Container
        container = AppContainer(applicationContext)

        // Clean any leftover temp files on startup
        container.tempFileManager.cleanAllTempFiles()
    }
}
