package com.example.network

import com.example.data.DaledouAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

object DaledouRunner {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    private const val BASE_URL = "https://dld.qzapp.z.qq.com/qpet/cgi-bin/phonepk?"

    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    /**
     * Executes a single Custom Command path and returns the clean string result.
     */
    suspend fun executeSingleCommand(account: DaledouAccount, commandPath: String): String {
        return try {
            val html = makeGetRequest(commandPath, account.cookieString)
            cleanHtmlToText(html)
        } catch (e: Exception) {
            "Error: ${e.localizedMessage ?: "Unknown network error"}"
        }
    }

    /**
     * Makes a GET request with cookies, auto-detecting/decoding GBK and UTF-8 charsets.
     */
    private suspend fun makeGetRequest(path: String, cookieString: String): String = withContext(Dispatchers.IO) {
        val cleanPath = path.trim().removePrefix("?").removePrefix("&")
        val url = "$BASE_URL$cleanPath"

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36 Edg/146.0.0.0")
            .addHeader("Cookie", cookieString)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Server returned code ${response.code}")
            }

            val bodyBytes = response.body?.bytes() ?: byteArrayOf()
            val contentType = response.body?.contentType()
            val declaredCharset = contentType?.charset()

            if (declaredCharset != null) {
                String(bodyBytes, declaredCharset)
            } else {
                // Autodetect GBK / GB2312 which is widely used in legacy Tencent web systems
                val gbkString = String(bodyBytes, Charset.forName("GBK"))
                if (gbkString.contains("乐斗") || gbkString.contains("宝库") || gbkString.contains("大笨钟") || gbkString.contains("系统")) {
                    gbkString
                } else {
                    String(bodyBytes, Charset.forName("UTF-8"))
                }
            }
        }
    }

    /**
     * Converts raw Q-Pet daledou HTML responses into beautifully structured plain text logs.
     */
    fun cleanHtmlToText(html: String): String {
        var text = html
        
        // Remove document head/title if present to focus purely on the main visual text
        if (text.contains("</head>")) {
            text = text.substringAfter("</head>")
        }
        
        // Replace line breaks with standard newlines
        text = text.replace(Regex("(?i)<br\\s*/?>"), "\n")
        
        // Treat paragraphs as separate lines
        text = text.replace(Regex("(?i)<p>"), "")
        text = text.replace(Regex("(?i)</p>"), "\n")
        
        // Strip out all other HTML tags
        text = text.replace(Regex("<[^>]*>"), "")
        
        // Decode common HTML character entities
        text = text.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")

        // Clean up redundant spacings and lines
        return text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("返回[") && !it.contains("回到腾讯游戏") }
            .joinToString("\n")
    }

    /**
     * Runs selected automated tasks sequentially and passes logs to the UI callback.
     */
    suspend fun runAutomation(
        account: DaledouAccount,
        tasksToRun: List<String>,
        onLog: (String) -> Unit
    ) {
        val qq = account.qq
        val cookies = account.cookieString

        if (cookies.isBlank()) {
            onLog("[${getCurrentTime()}] ✖ [QQ:$qq] 启动失败：未配置有效的 Cookie 信息。")
            return
        }

        onLog("[${getCurrentTime()}] ▶ [QQ:$qq] 开始执行所选的自动化任务...")
        delay(500)

        for (task in tasksToRun) {
            try {
                onLog("[${getCurrentTime()}] ➔ [QQ:$qq] 正在加载：$task ...")
                delay(600)

                when (task) {
                    "每日签到" -> {
                        val response = makeGetRequest("cmd=dailyAct", cookies)
                        val clean = cleanHtmlToText(response)
                        val resultText = clean.lines().firstOrNull { it.contains("签到") || it.contains("礼包") || it.contains("成功") }
                            ?: clean.take(150)
                        onLog("[${getCurrentTime()}] 每日签到：$resultText")
                    }

                    "大笨钟" -> {
                        val response = makeGetRequest("cmd=newAct&subtype=18", cookies)
                        val clean = cleanHtmlToText(response)
                        onLog("[${getCurrentTime()}] 大笨钟：$clean")
                    }

                    "幸运金蛋" -> {
                        // Phase 1: Retrieve Egg Page and Index ID
                        val response = makeGetRequest("cmd=newAct&subtype=110&op=0", cookies)
                        val matcher = Pattern.compile("index=(\\d+)").matcher(response)
                        if (matcher.find()) {
                            val index = matcher.group(1) ?: "1"
                            // Phase 2: Smash Egg
                            val smashResponse = makeGetRequest("cmd=newAct&subtype=110&op=1&index=$index", cookies)
                            val cleanResult = cleanHtmlToText(smashResponse)
                            onLog("[${getCurrentTime()}] 幸运金蛋(砸蛋)：$cleanResult")
                        } else {
                            val cleanPage = cleanHtmlToText(response)
                            onLog("[${getCurrentTime()}] 幸运金蛋: ${if(cleanPage.contains("已")) "今日已经砸过金蛋" else "暂无可用免费次数"}")
                        }
                    }

                    "帮派巡礼" -> {
                        val response = makeGetRequest("cmd=abysstide&op=getfactiongift", cookies)
                        val clean = cleanHtmlToText(response)
                        onLog("[${getCurrentTime()}] 帮派巡礼：$clean")
                    }

                    "邪神秘宝" -> {
                        // Run both free normal and free advanced draws
                        val res1 = makeGetRequest("cmd=tenlottery&op=2&type=0", cookies)
                        val clean1 = cleanHtmlToText(res1)
                        onLog("[${getCurrentTime()}] 邪神秘宝(高级)：${clean1.lines().firstOrNull() ?: clean1}")

                        delay(600)

                        val res2 = makeGetRequest("cmd=tenlottery&op=2&type=1", cookies)
                        val clean2 = cleanHtmlToText(res2)
                        onLog("[${getCurrentTime()}] 邪神秘宝(极品)：${clean2.lines().firstOrNull() ?: clean2}")
                    }

                    "帮派宝库" -> {
                        val response = makeGetRequest("cmd=fac_corp&op=0", cookies)
                        // Search for gift links: gift_id=(\d+)&amp;type=(\d+)
                        val pattern = Pattern.compile("gift_id=(\\d+)&amp;type=(\\d+)")
                        val matcher = pattern.matcher(response)
                        var claimCount = 0
                        while (matcher.find()) {
                            val giftId = matcher.group(1) ?: ""
                            val type = matcher.group(2) ?: ""
                            if (giftId.isNotEmpty() && type.isNotEmpty()) {
                                val claimRes = makeGetRequest("cmd=fac_corp&op=3&gift_id=$giftId&type=$type", cookies)
                                val cleanClaim = cleanHtmlToText(claimRes)
                                onLog("[${getCurrentTime()}] 帮派宝库领取($giftId): $cleanClaim")
                                claimCount++
                                delay(600)
                            }
                        }
                        if (claimCount == 0) {
                            onLog("[${getCurrentTime()}] 帮派宝库：无可领取的帮派商会礼包。")
                        }
                    }
                    
                    else -> {
                        onLog("[${getCurrentTime()}] ✖ 未知任务类型：$task")
                    }
                }
            } catch (e: Exception) {
                onLog("[${getCurrentTime()}] ✖ 执行任务 [$task] 异常：${e.localizedMessage ?: "网络接入超时"}")
            }
            delay(500) // Brief safety sleep
        }

        onLog("[${getCurrentTime()}] ✔ [QQ:$qq] 自动化任务执行完成。")
    }
}
