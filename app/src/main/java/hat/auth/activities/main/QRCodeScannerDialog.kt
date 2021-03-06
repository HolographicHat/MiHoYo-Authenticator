package hat.auth.activities.main

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import hat.auth.activities.MainActivity
import hat.auth.data.MiAccount
import hat.auth.data.TapAccount
import hat.auth.utils.MiHoYoUrlRegex
import hat.auth.utils.TapUrlRegex
import hat.auth.utils.toast
import hat.auth.utils.ui.PermissionRequiredDialog
import hat.auth.utils.ui.QRCodeScanner
import hat.auth.utils.ui.TextButton

var captureManager by mutableStateOf<CaptureManager?>(null)
var barcodeView    by mutableStateOf<DecoratedBarcodeView?>(null)

private fun stopCamera() {
    captureManager?.onPause()
    captureManager = null
    barcodeView = null
}

private var isDialogShowing by mutableStateOf(false)

private typealias ScanCallback = (BarcodeResult) -> Unit
private var currentCallback: ScanCallback? = null
private var currentCheckRegex: Regex? = null

fun showQRCodeScannerDialog() {
    currentCheckRegex = when (currentAccount) {
        is MiAccount -> MiHoYoUrlRegex
        is TapAccount -> TapUrlRegex
        else -> throw IllegalArgumentException("Unknown account type.")
    }
    isDialogShowing = true
}

fun registerScanCallback(func: ScanCallback) {
    currentCallback = func
}

@Composable
@ExperimentalPermissionsApi
fun MainActivity.QRCodeScannerDialog() {
    if (isDialogShowing) QCD()
}

@Composable
@ExperimentalPermissionsApi
private fun MainActivity.QCD(
    onDismissRequest: () -> Unit = { isDialogShowing = false }
) = PermissionRequiredDialog(
    permission = Manifest.permission.CAMERA,
    permissionNotGrantedContent = {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text("????????????")
            },
            text = {
                Text(
                    buildAnnotatedString {
                        append("??????????????????????????????")
                        withStyle(SpanStyle(Color.Red)) {
                            append(" ?????? ")
                        }
                        append("??????")
                    }
                )
            },
            confirmButton = {
                TextButton("??????") {
                    it.launchPermissionRequest()
                }
            }
        )
    },
    permissionNotAvailableContent = {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text("????????????????????????")
            },
            text = {
                Text(
                    buildAnnotatedString {
                        append("??????????????????????????????")
                        withStyle(SpanStyle(Color.Red)) {
                            append(" ?????? ")
                        }
                        append("????????????????????????????????????")
                    }
                )
            },
            confirmButton = {
                TextButton("??????????????????") {
                    startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", packageName, null)
                        )
                    )
                }
            }
        )
    }
) {
    Dialog(
        onDismissRequest = {
            onDismissRequest()
            stopCamera()
        }
    ) {
        QRCodeScanner(
            status = currentAccount.uid,
            modifier = Modifier
                .size(275.dp)
                .clip(RoundedCornerShape(15.dp)),
            callback = {
                if (text.matches(currentCheckRegex!!)) {
                    stopCamera()
                    onDismissRequest()
                    currentCallback!!.invoke(this)
                } else {
                    toast("??????????????????")
                }
            }
        )
    }
}
