package com.keyrico.examplecompose

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.keyrico.compose.ConfirmationModalBottomSheet
import com.keyrico.examplecompose.ui.theme.KeyriTheme
import com.keyrico.compose.ScannerPreview
import com.keyrico.keyrisdk.Keyri
import com.keyrico.keyrisdk.entity.session.Session
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val keyri by lazy(::Keyri)

    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KeyriTheme {
                Surface(color = MaterialTheme.colors.background) {
                    var sessionState by remember { mutableStateOf<Session?>(null) }
                    var isLoading by remember { mutableStateOf(false) }

                    val coroutineScope = rememberCoroutineScope()
                    val navController = rememberNavController()

                    val modalBottomSheetState = rememberModalBottomSheetState(
                        initialValue = ModalBottomSheetValue.Hidden,
                        skipHalfExpanded = true
                    )

                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") { Main(navController) }
                        composable("scanner") {
                            val onFailure: (Throwable) -> Unit = {
                                navController.navigateUp()
                                navController.navigate("authComplete/false")
                            }

                            ScannerPreview(
                                onScanResult = { scanResult ->
                                    if (!isLoading) {
                                        isLoading = true

                                        scanResult.onSuccess {
                                            processScannedData(it)?.let { sessionId ->
                                                coroutineScope.launch {
                                                    initiateQrSession(sessionId).onSuccess { session ->
                                                        sessionState = session
                                                        modalBottomSheetState.show()
                                                    }.onFailure(onFailure)
                                                }
                                            } ?: onFailure(Throwable("Session Id is null"))
                                        }.onFailure(onFailure)
                                    }
                                },
                                onClose = { navController.popBackStack() },
                                isLoading = isLoading
                            )
                        }
                        composable(
                            "authComplete/{result}",
                            arguments = listOf(navArgument("result") { type = NavType.BoolType })
                        ) { backStackEntry ->
                            AuthComplete(
                                backStackEntry.arguments?.getBoolean("result") ?: false
                            )
                        }
                    }

                    ConfirmationModalBottomSheet(modalBottomSheetState, sessionState) { result ->
                        navController.navigateUp()
                        navController.navigate("authComplete/$result")
                    }
                }
            }
        }
    }

    // TODO 1. Move to Scanner
    // TODO 2. Implement all callbacks
    private fun processScannedData(scannedData: String): String? {
        Log.d("Keyri", "QR processed: $scannedData")

        return try {
            processLink(scannedData.toUri())
        } catch (e: Exception) {
            Log.d("Keyri", "Not valid link: $scannedData")

            null
        }
    }

    private fun processLink(uri: Uri?): String {
        return uri?.getQueryParameters("sessionId")?.firstOrNull()
            ?: throw Exception("Failed to process link")
    }

    private suspend fun initiateQrSession(sessionId: String): Result<Session> {
        // TODO Uncomment
//        keyri.initiateQrSession("Your app key here", sessionId, "publicUserId")
        return keyri.initiateQrSession(
            "IT7VrTQ0r4InzsvCNJpRCRpi1qzfgpaj",
            sessionId,
            "publicUserId"
        )
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun Main(navController: NavController) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                navController.navigate("scanner")
            } else {
                Log.d("Keyri", "Camera permission denied")
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keyri Compose Example", color = Color.White) },
                backgroundColor = Color(0xFF4a138c)
            )
        },
        content = {
            Button(modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp), onClick = {
                launcher.launch(Manifest.permission.CAMERA)
            }) {
                Text(text = "Easy Keyri Auth", color = Color.White)
            }
        },
    )
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun AuthComplete(result: Boolean = false) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keyri Compose Example", color = Color.White) },
                backgroundColor = Color(0xFF4a138c)
            )
        },
        content = {
            val resultText = if (result) {
                "You have been successfully authenticated"
            } else {
                "Unable to authorize"
            }

            Text(
                text = resultText, modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp), color = Color.White
            )
        },
    )
}
