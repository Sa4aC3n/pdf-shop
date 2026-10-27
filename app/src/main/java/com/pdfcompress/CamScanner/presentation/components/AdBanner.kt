package com.pdfcompress.CamScanner.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

const val DEFAULT_AD_UNIT_ID = "ca-app-pub-5797893761930988/6849666067"

@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = DEFAULT_AD_UNIT_ID
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
