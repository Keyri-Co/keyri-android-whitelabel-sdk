package com.keyrico.examplecompose

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.TopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.keyrico.examplecompose.ui.theme.KeyriTheme
import com.keyrico.compose.ScannerPreview
import com.keyrico.keyrisdk.Keyri

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KeyriTheme {
                Surface(color = MaterialTheme.colors.background) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") { Main(navController) }
                        composable("scanner") {
                            ScannerPreview(
                                onScanResult = {
                                    // TODO Keyri auth
                                },
                                onClose = { navController.popBackStack() },
                                loading = true
                            )
                        }
                        composable("authComplete") { AuthComplete() }
                    }
                }
            }
        }
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
                backgroundColor = Color.Blue
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
                backgroundColor = Color.Blue
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

private fun keyriAuth(sessionId: String) {
    val keyri = Keyri()
    // TODO Here is Keyri auth
}