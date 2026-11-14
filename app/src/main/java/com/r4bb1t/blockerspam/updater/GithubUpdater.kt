package com.r4bb1t.blockerspam.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class GithubUpdater(private val context: Context) {

    private val repoUrl = "https://api.github.com/repos/Mr4bb1t/BlockerSpam/releases/latest"

    data class UpdateInfo(val version: String, val downloadUrl: String)

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(repoUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                useCaches = false
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "BlockerSpam-Android")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w("GithubUpdater", "GitHub respondeu ${connection.responseCode}")
                return@withContext null
            }

            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            val json = JSONObject(response)
            val tagName = json.optString("tag_name", "").trim()
            val releaseVersion = parseVersion(tagName) ?: run {
                Log.w("GithubUpdater", "Tag de release inválida/ignorada: '$tagName'")
                return@withContext null
            }

            val currentVersionName = context.packageManager
                .getPackageInfo(context.packageName, 0).versionName
            val currentVersion = parseVersion(currentVersionName)

            if (currentVersion == null) {
                Log.w("GithubUpdater", "Versão instalada inválida: '$currentVersionName'")
                return@withContext null
            }

            // Compara números de versão (1.6 > 1.5). Se a versão instalada já é
            // igual ou maior que a tag, o banner de atualização desaparece.
            if (!isNewerVersion(releaseVersion, currentVersion)) {
                return@withContext null
            }

            val assets = json.optJSONArray("assets") ?: return@withContext null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.optString("name", "")
                if (assetName.endsWith(".apk", ignoreCase = true)) {
                    return@withContext UpdateInfo(
                        version = formatVersion(releaseVersion),
                        downloadUrl = asset.getString("browser_download_url")
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("GithubUpdater", "Error checking for update", e)
        } finally {
            connection?.disconnect()
        }
        return@withContext null
    }

    private fun parseVersion(raw: String?): List<Int>? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.trim().removePrefix("v").removePrefix("V")
        val match = Regex("""^(\d+)(?:\.(\d+))?(?:\.(\d+))?""").find(cleaned) ?: return null
        return listOfNotNull(
            match.groupValues[1].toIntOrNull(),
            match.groupValues[2].toIntOrNull(),
            match.groupValues[3].toIntOrNull()
        )
    }

    private fun isNewerVersion(newVersion: List<Int>, currentVersion: List<Int>): Boolean {
        val maxLength = maxOf(newVersion.size, currentVersion.size)
        for (i in 0 until maxLength) {
            val n = newVersion.getOrElse(i) { 0 }
            val c = currentVersion.getOrElse(i) { 0 }
            if (n > c) return true
            if (n < c) return false
        }
        return false
    }

    private fun formatVersion(parts: List<Int>): String = parts.joinToString(".")

    fun downloadAndInstall(updateInfo: UpdateInfo) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(updateInfo.downloadUrl)

        val destinationFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: run {
            Log.e("GithubUpdater", "Diretório de download indisponível")
            return
        }
        val fileName = "BlockerSpam_update.apk"
        val oldFile = File(destinationFolder, fileName)
        if (oldFile.exists()) {
            oldFile.delete()
        }

        val request = DownloadManager.Request(uri)
            .setTitle("BlockerSpam Atualização")
            .setDescription("Baixando a versão ${updateInfo.version}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)

        val downloadId = downloadManager.enqueue(request)
        val appContext = context.applicationContext

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    try {
                        appContext.unregisterReceiver(this)
                    } catch (e: IllegalArgumentException) {
                        Log.w("GithubUpdater", "Receiver de download já removido", e)
                    }
                    installApk(fileName, updateInfo.version)
                }
            }
        }

        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun installApk(fileName: String, expectedVersion: String) {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (!file.exists() || file.length() == 0L) {
            Log.e("GithubUpdater", "APK baixado não encontrado ou vazio")
            return
        }

        // Proteção contra cache antigo: não abre o instalador se o APK baixado
        // não for realmente mais novo que a versão instalada.
        val downloadedVersion = try {
            context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)?.versionName
        } catch (e: Exception) {
            null
        }
        val installedVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            null
        }

        val downloadedParts = parseVersion(downloadedVersion)
        val installedParts = parseVersion(installedVersion)
        if (downloadedParts != null && installedParts != null &&
            !isNewerVersion(downloadedParts, installedParts)
        ) {
            Log.w(
                "GithubUpdater",
                "APK baixado ($downloadedVersion) não é mais novo que o instalado " +
                    "($installedVersion) para a release $expectedVersion. Instalação cancelada."
            )
            file.delete()
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("GithubUpdater", "Error installing APK", e)
        }
    }
}
