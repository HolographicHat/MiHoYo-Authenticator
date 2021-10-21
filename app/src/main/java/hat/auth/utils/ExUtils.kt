package hat.auth.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.StrictMode
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import hat.auth.Application.Companion.context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import kotlin.random.Random

val ioScope = CoroutineScope(Dispatchers.IO)
val QRCodeUrlRegex = Regex("https://user\\.mihoyo\\.com/qr_code_in_game\\.html\\?app_id=\\d&app_name=[%A-Z0-9]+&bbs=true&biz_key=\\w+&expire=\\d{10}&ticket=[a-f0-9]{24}")
val PhoneNumRegex = Regex("^1(?:3\\d{3}|5[^4\\D]\\d{2}|8\\d{3}|7(?:[0-35-9]\\d{2}|4(?:0\\d|1[0-2]|9\\d))|9[0-35-9]\\d{2}|6[2567]\\d{2}|4(?:(?:10|4[01])\\d{3}|[68]\\d{4}|[579]\\d{2}))\\d{6}\$")
val currentTimeSeconds get() = currentTimeMills / 1000
val currentTimeMills get() = System.currentTimeMillis()

val deviceId by lazy {
    context.getDataFile("uuid").getText {
        UUID.randomUUID().toString()
    }
}

fun buildThreadPolicy(block: StrictMode.ThreadPolicy.Builder.() -> Unit): StrictMode.ThreadPolicy =
    StrictMode.ThreadPolicy.Builder().apply(block).build()

fun buildVmPolicy(block: StrictMode.VmPolicy.Builder.() -> Unit): StrictMode.VmPolicy =
    StrictMode.VmPolicy.Builder().apply(block).build()

fun getDrawableAsBitmap(@DrawableRes resId: Int) =
    AppCompatResources.getDrawable(context,resId)!!.toBitmap()

fun getDrawableAsImageBitmap(@DrawableRes resId: Int) = getDrawableAsBitmap(resId).asImageBitmap()

suspend fun Activity.getBitmap(
    url: String,
) = withContext(Dispatchers.IO) {
    runCatching {
        val bm = buildHttpRequest {
            url(url)
        }.execute().body?.bitmap()
        checkNotNull(bm)
    }.onFailure {
        it.printStackTrace()
        toast("图片加载失败: ${it.message}")
    }.getOrNull()
}

fun Activity.toast(
    msg: String,
    length: Int = Toast.LENGTH_SHORT
) = runOnUiThread {
    Toast.makeText(this,msg,length).show()
}

fun Activity.openWebPage(url: String) {
    val intent = Intent(Intent.ACTION_VIEW,Uri.parse(url))
    startActivity(intent)
}

fun Context.getDataFile(name: String) = File(dataDir.absolutePath,name)

fun File.getText(init: () -> String) = if (exists()) {
    readText()
} else {
    createNewFile()
    init().apply {
        writeText(this)
    }
}

fun Random.nextString(length: Int): String {
    val charArray = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    var rString = ""
    (1..length).forEach { _ ->
        rString += charArray.random(this)
    }
    return rString
}

fun String.digest(algorithm: String): String = MessageDigest.getInstance(algorithm).run {
    update(toByteArray())
    BigInteger(1,digest()).toString(16)
}