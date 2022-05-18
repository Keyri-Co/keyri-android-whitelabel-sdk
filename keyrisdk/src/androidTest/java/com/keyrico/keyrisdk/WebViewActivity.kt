package com.keyrico.keyrisdk

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.keyrico.keyrisdk.ExampleAppInstrumentedTest.Companion.APP_KEY
import com.keyrico.keyrisdk.ExampleAppInstrumentedTest.Companion.SERVICE_DOMAIN
import com.keyrico.keyrisdk.ExampleAppInstrumentedTest.Companion.WEB_VIEW_URL
import com.keyrico.keyrisdk.entity.Session
import com.keyrico.keyrisdk.mocked.sessionDenied
import com.keyrico.keyrisdk.mocked.sessionNoIpData
import com.keyrico.keyrisdk.mocked.sessionRegular
import com.keyrico.keyrisdk.mocked.sessionWarning
import com.keyrico.keyrisdk.mocked.sessionWithoutRiskPermission
import com.keyrico.keyrisdk.ui.confirmation.ConfirmationBottomDialog
import kotlinx.coroutines.launch

class WebViewActivity : AppCompatActivity() {

    private val viewModel by viewModels<WebViewViewModel>()

    private val keyriSdk by lazy {
        val appKey = intent.getStringExtra(APP_KEY)
        val serviceDomain = intent.getStringExtra(SERVICE_DOMAIN)

        KeyriSdk(this, requireNotNull(appKey), requireNotNull(serviceDomain))
    }

    private val webView by lazy { WebView(this) }

    private var result = TestResults()

    private var isLinkProcessed = false
    private var isDialogShowed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeViewModel()

        val webViewUrl = requireNotNull(intent.getStringExtra(WEB_VIEW_URL))

        setContentView(webView)

        webView.settings.javaScriptEnabled = true

        webView.loadUrl(webViewUrl)

        webView.afterDelay(10_000L) {
            val picture: Picture = webView.capturePicture()
            val bitmap = Bitmap.createBitmap(picture.width, picture.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            picture.draw(canvas)

            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_AZTEC)
                .build()

            BarcodeScanning.getClient(options)
                .process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { barcodes ->
                    val scannedData = barcodes.firstOrNull()?.displayValue

                    Log.d("Keyri", "Scanned data: $scannedData")

                    val sessionId =
                        scannedData?.toUri()?.getQueryParameters("sessionId")?.firstOrNull()

                    result = result.copy(sessionId = sessionId)

                    Log.d("Keyri", "Session ID: $sessionId")

                    if (!isLinkProcessed) {
                        viewModel.newSession(
                            requireNotNull(sessionId),
                            "some-public-user-id",
                            "some-username",
                            "Secure custom",
                            "Public custom",
                            keyriSdk
                        )

                        isLinkProcessed = true
                    }
                }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.authenticated.collect {
                    if (it != null) {
                        result = result.copy(domainName = it.widgetOrigin)

                        if (!isDialogShowed) {
                            checkRegularDialog()

                            isDialogShowed = true
                        }
                    }
                }
            }
        }
    }

    private fun checkRegularDialog() {
        showDialog(sessionRegular, {
            result = result.copy(sessionRegularDialog = it)
        }) { checkSessionDenied() }
    }

    private fun checkSessionDenied() {
        showDialog(sessionDenied, {
            result = result.copy(sessionDeniedDialog = it)
        }) { checkSessionWarning() }
    }

    private fun checkSessionWarning() {
        showDialog(sessionWarning, {
            result = result.copy(sessionWarningDialog = it)
        }) { checkSessionNoIpData() }
    }

    private fun checkSessionNoIpData() {
        showDialog(sessionNoIpData, {
            result = result.copy(sessionNoIpDataDialog = it)
        }) { checkSessionWithoutRiskPermission() }
    }

    private fun checkSessionWithoutRiskPermission() {
        showDialog(sessionWithoutRiskPermission, {
            result = result.copy(sessionWithoutRiskPermissionDialog = it)
        }) {
            val intent = Intent().apply {
                putExtra(TEST_RESULTS, result)
            }

            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private fun showDialog(
        session: Session,
        onDelay: (Map<String, Boolean>) -> Unit,
        onDialogResult: (Boolean) -> Unit
    ) {
        val dialog = ConfirmationBottomDialog(session, onDialogResult)

        dialog.show(supportFragmentManager, ConfirmationBottomDialog::class.java.name)

        webView.afterDelay {
            val mobileIpDataVisible = dialog.view?.isChildVisible(R.id.llMobileLocation) ?: false
            val widgetIpDataVisible = dialog.view?.isChildVisible(R.id.llWidgetLocation) ?: false
            val userAgentVisible = dialog.view?.isChildVisible(R.id.llWidgetAgent) ?: false
            val buttonsVisible = dialog.view?.isChildVisible(R.id.llButtons) ?: false

            val confirmationDialogResult =
                mapOf(
                    "mobileIpDataVisible" to mobileIpDataVisible,
                    "widgetIpDataVisible" to widgetIpDataVisible,
                    "userAgentVisible" to userAgentVisible,
                    "buttonsVisible" to buttonsVisible
                )

            onDelay(confirmationDialogResult)
            dialog.view?.findViewById<Button>(R.id.bYes)?.callOnClick()
        }
    }

    private fun WebView.afterDelay(delayTimeMs: Long = 4_000, callback: () -> Unit) {
        postDelayed({
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                callback()
            }
        }, delayTimeMs)
    }

    private fun View.isChildVisible(resId: Int): Boolean {
        return findViewById<View>(resId).isVisible
    }

    companion object {
        const val TEST_RESULTS = "TEST_RESULTS"
    }
}
