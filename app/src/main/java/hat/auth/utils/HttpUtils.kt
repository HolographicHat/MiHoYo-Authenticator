package hat.auth.utils

import android.graphics.BitmapFactory
import android.os.Build
import android.webkit.WebSettings
import hat.auth.Application.Companion.context
import hat.auth.activities.currentAccount
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import kotlin.random.Random

object OkClients {

    private val UA_STRING = buildString {
        append(WebSettings.getDefaultUserAgent(context))
        append(" miHoYoBBS/$BBS_VERSION")
    }

    val NORMAL by lazy {
        OkHttpClient.Builder().addHttpLogger().build()
    }

    val WEB by lazy {
        OkHttpClient.Builder().addInterceptor { chain ->
            val req = chain.request().newBuilder().apply {
                val (u,t) = currentAccount.run { uid to lsToken }
                addHeader("Cookie","ltuid=$u;ltoken=${t.lToken}")
                addHeader("User-Agent",UA_STRING)
            }.build()
            chain.proceed(req)
        }.addHttpLogger().build()
    }

    val SAPI by lazy {
        OkHttpClient.Builder().addInterceptor { chain ->
            val req = chain.request().newBuilder().apply {
                val (u,t) = currentAccount.run { uid to lsToken }
                addHeader("Cookie","stuid=$u;stoken=${t.sToken};ltuid=$u;ltoken=${t.lToken}")
                addDSHeader()
                addRpcHeader()
            }.build()
            chain.proceed(req)
        }.addHttpLogger().build()
    }

}

fun OkHttpClient.Builder.addHttpLogger() = apply {
    val i = HttpLoggingInterceptor(Log.okLogger)
    i.level = HttpLoggingInterceptor.Level.BODY
    this.addInterceptor(i)
}

fun Request.execute(client: OkHttpClient = OkClients.NORMAL) = client.newCall(this).execute()

fun RequestBody.string() = Buffer().use { buf ->
    writeTo(buf)
    buf.readUtf8()
}

fun String.asJsonBody() = toRequestBody("application/json".toMediaType())

fun String.asUrlEncodedBody() = toRequestBody("application/x-www-form-urlencoded".toMediaType())

fun FormBody.Builder.add(name: String, value: Any) = add(name, value.toString())

fun FormBody.Builder.addTimestamp(key: String = "t") = apply { add(key,currentTimeMills) }

fun FormBody.Builder.addCaptchaData(data: CaptchaData) = apply {
    add("mmt_key",data.mmtKey)
    add("geetest_seccode",data.secCode)
    add("geetest_validate",data.validate)
    add("geetest_challenge",data.challenge)
}

fun ResponseBody.bitmap() = BitmapFactory.decodeStream(byteStream())!!

/** https://github.com/Azure99/GenshinPlayerQuery/blob/a8553e794cb9df4e379586413ed590a88bec4c11/src/Core/GenshinAPI.cs **/
const val BBS_SALT = "xV8v4Qu54lUKrEYFZkJhB8cuOh9Asafs"
const val BBS_VERSION = "2.11.1"
const val BBS_CTYPE = "5"
private val nch = CacheControl.Builder().noCache().build()

fun Request.Builder.noCache() = apply { cacheControl(nch) }

fun Request.Builder.addRpcHeader() = apply {
    addHeader("X-Rpc-Channel", "miyousheluodi")
    addHeader("X-Rpc-Device_id", deviceId)
    addHeader("X-Rpc-Client_type", BBS_CTYPE)
    addHeader("X-Rpc-App_version", BBS_VERSION)
    addHeader("X-Rpc-Sys_version", Build.VERSION.RELEASE)
    addHeader("X-Rpc-Device_name", "${Build.MANUFACTURER} ${Build.MODEL}")
    addHeader("X-Rpc-Device_model", Build.MODEL)
}

fun Request.Builder.addDSHeader() = apply {
    val r = build()
    val u = r.url.toString()
    r.body.let {
        if (it != null) {
            addHeader("Ds",createDynamicSecret(u,it.string()))
        } else {
            addHeader("Ds",createDynamicSecret(u))
        }
    }
}

val Response.notNullBody get() = checkNotNull(body)

fun getText(
    url: String,
    client: OkHttpClient = OkClients.NORMAL,
    header: Map<String,String> = emptyMap(),
    postBody: RequestBody? = null
) = buildHttpRequest {
    url(url)
    noCache()
    addHeader("Connection","close")
    header.forEach { (k,v) ->
        addHeader(k,v)
    }
    postBody?.let { post(postBody) }
}.execute(client).notNullBody.string()

fun buildFormBody(block: FormBody.Builder.() -> Unit) = FormBody.Builder().apply(block).build()

fun buildHttpRequest(block: Request.Builder.() -> Unit) = Request.Builder().apply(block).build()

fun createDynamicSecret(url: String,data: String = ""): String {
    var q = ""
    url.split("?").getOrNull(1)?.let {
        q = it.split("&").sorted().joinToString("&")
    }
    val t = currentTimeSeconds
    val r = Random.nextString(6)
    val chk = "salt=$BBS_SALT&t=$t&r=$r&b=$data&q=$q".digest("MD5")
    return "$t,$r,$chk"
}
