package com.pdfcompress.CamScanner

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlin.concurrent.thread

class PdfShopApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        // Fast in-memory container initialization
        container = AppContainer(applicationContext)

        // Offload heavy SDK and I/O initialization to background thread for lightning-fast startup!
        thread(start = true, isDaemon = true, name = "AppWarmupThread") {
            try {
                // Initialize PDFBox resource loader in background
                PDFBoxResourceLoader.init(applicationContext)
            } catch (_: Exception) {}

            try {
                // Initialize Google Mobile Ads SDK in background
                MobileAds.initialize(this@PdfShopApplication) {}
            } catch (_: Exception) {}

            try {
                // Clean temp files in background
                container.tempFileManager.cleanAllTempFiles()
            } catch (_: Exception) {}
        }
    }
}
