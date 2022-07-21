package com.keyrico.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.keyrico.keyrisdk.entity.session.Session

@ExperimentalMaterialApi
@Composable
fun ConfirmationModalBottomSheet(
    modalBottomSheetState: ModalBottomSheetState,
    session: Session? = null,
    onResult: (Result<Boolean>) -> Unit
) {
    ModalBottomSheetLayout(
        sheetState = modalBottomSheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetContent = {
            // TODO Add Impl
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Are you trying to log in?",
                    modifier = Modifier.padding(top = 36.dp)
                )

                Text(
                    text = "Your login attempt was denied.\nIf you would still like to log in, please turn off your VPN then rescan the QR code",
                    modifier = Modifier
                        .padding(top = 24.dp, start = 28.dp, end = 28.dp)
                        .fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .padding(top = 30.dp)
                        .fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_widget_location),
                        contentDescription = "Widget location"
                    )
                    Column {
                        Text(
                            text = "Near Oakland, CA",
                            modifier = Modifier.padding(horizontal = 28.dp)
                        )

                        Text(
                            text = "VPN Detected",
                            color = colorResource(id = R.color.vpn_red),
                            modifier = Modifier.padding(horizontal = 28.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(top = 30.dp)
                        .fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_mobile),
                        contentDescription = "Mobile location"
                    )
                    Column {
                        Text(
                            text = "Near Oakland, CA",
                            modifier = Modifier.padding(horizontal = 28.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(top = 30.dp)
                        .fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_widget),
                        contentDescription = "Widget agent"
                    )
                    Column {
                        Text(
                            text = "Chrome on Mac OS",
                            modifier = Modifier.padding(horizontal = 28.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(top = 30.dp)
                        .fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            // TODO Add impl
                        },
                        border = BorderStroke(1.dp, colorResource(id = R.color.red)),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.faded_red)),
                        modifier = Modifier
                            .height(50.dp)
                            .weight(1F)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            modifier = Modifier.size(16.dp),
                            contentDescription = "Deny",
                            tint = colorResource(id = R.color.red)
                        )

                        Text("No")
                    }

                    Button(
                        onClick = {
                            // TODO Add impl
                        },
                        border = BorderStroke(1.dp, colorResource(id = R.color.green)),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.faded_green)),
                        modifier = Modifier
                            .height(50.dp)
                            .weight(1F)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_accept),
                            modifier = Modifier.size(16.dp),
                            contentDescription = "Confirm",
                            tint = colorResource(id = R.color.green)
                        )

                        Text("Yes")
                    }
                }

                Text(
                    text = "Powered by Keyri",
                    modifier = Modifier.padding(top = 26.dp, bottom = 36.dp)
                )
            }
        }
    ) {}
}
