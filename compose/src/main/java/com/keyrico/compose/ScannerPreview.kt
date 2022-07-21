package com.keyrico.compose

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.tasks.TaskExecutors
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.MlKitException

@Composable
fun ScannerPreview(
    modifier: Modifier = Modifier,
    onScanResult: (Result<String>) -> Unit = {},
    onClose: () -> Unit = {},
    isLoading: Boolean = false
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }

    remember {
        ProcessCameraProvider.getInstance(context)
            .configureCamera(
                previewView = previewView,
                lifecycleOwner = lifecycleOwner,
                context = context,
                onScanResult = onScanResult,
                isLoading = isLoading
            )
    }

    BoxWithConstraints(modifier = modifier) {
        CameraPreview(previewView)

        OutlinedButton(
            modifier = Modifier
                .padding(16.dp)
                .size(40.dp)
                .align(Alignment.TopEnd),
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
            shape = CircleShape,
            border = BorderStroke(1.dp, colorResource(id = R.color.close_border)),
            contentPadding = PaddingValues(8.dp)
        ) {
            Icon(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = "Close",
                tint = colorResource(id = R.color.close_background)
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x00FFFFFF),
                            Color.White
                        )
                    )
                )
                .align(Alignment.BottomCenter)
                .fillMaxHeight(0.28F)
                .fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colorResource(id = R.color.text_color)
                )
            }

            Text(
                text = "Powered by Keyri",
                style = typography.bodySmall,
                color = colorResource(id = R.color.text_color),
                modifier = Modifier
                    .padding(bottom = 36.dp)
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun CameraPreview(previewView: PreviewView) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            previewView.apply {
                this.scaleType = PreviewView.ScaleType.FILL_CENTER

                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }

            previewView
        })
}

private fun ListenableFuture<ProcessCameraProvider>.configureCamera(
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    context: Context,
    onScanResult: (Result<String>) -> Unit = {},
    isLoading: Boolean
): ListenableFuture<ProcessCameraProvider> {
    addListener({
        val cameraSelector = CameraSelector.Builder().build()

        val preview = Preview.Builder()
            .build()
            .apply { setSurfaceProvider(previewView.surfaceProvider) }

        try {
            get().apply {
                unbindAll()

                if (isLoading) {
                    bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                } else {
                    val analysis = bindAnalysisUseCase(onScanResult)

                    bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                    bindToLifecycle(lifecycleOwner, cameraSelector, analysis)
                }
            }
        } catch (exception: Exception) {
            Log.d("Keyri", "Failed to init Camera")

            onScanResult(Result.failure(exception))
        }
    }, ContextCompat.getMainExecutor(context))
    return this
}

private fun bindAnalysisUseCase(onScanResult: (Result<String>) -> Unit = {}): ImageAnalysis? {

    val imageProcessor = try {
        BarcodeDetectorProcessor()
    } catch (exception: Exception) {
        Log.e("Keyri", "Can not create image processor", exception)

        onScanResult(Result.failure(exception))
        return null
    }
    val builder = ImageAnalysis.Builder()
    val analysisUseCase = builder.build()

    analysisUseCase.setAnalyzer(TaskExecutors.MAIN_THREAD) { imageProxy: ImageProxy ->
        try {
            imageProcessor.processImageProxy(imageProxy, onScanResult)
        } catch (exception: MlKitException) {
            val message = "Failed to process image. Error: " + exception.localizedMessage

            Log.e("Keyri", message)

            onScanResult(Result.failure(exception))
        }
    }

    return analysisUseCase
}
