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
        try {
            val url = URL(repoUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val tagName = json.getString("tag_name").replace("v", "") // "v1.1.0" -> "1.1.0"
                
                val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"

                if (isNewerVersion(tagName, currentVersion)) {
                    val assets = json.getJSONArray("assets")
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val assetName = asset.getString("name")
                        if (assetName.endsWith(".apk")) {
                            return@withContext UpdateInfo(
                                version = tagName,
                                downloadUrl = asset.getString("browser_download_url")
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GithubUpdater", "Error checking for update", e)
        }
        return@withContext null
    }

    private fun isNewerVersion(newVersion: String, currentVersion: String): Boolean {
        val newParts = newVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLength = maxOf(newParts.size, currentParts.size)

        for (i in 0 until maxLength) {
            val n = newParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (n > c) return true
            if (n < c) return false
        }
        return false
    }

    fun downloadAndInstall(updateInfo: UpdateInfo) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(updateInfo.downloadUrl)

        val destinationFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val fileName = "BlockerSpam_update.apk"
        val oldFile = File(destinationFolder, fileName)
        if (oldFile.exists()) {
            oldFile.delete()
        }

        val request = DownloadManager.Request(uri)
            .setTitle("BlockerSpam Atualização")
            .setDescription("Baixando a versão ${updateInfo.version}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "BlockerSpam_update.apk")

        val downloadId = downloadManager.enqueue(request)

        // Registrar o receiver para ouvir quando o download terminar
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    installApk("BlockerSpam_update.apk")
                    context?.unregisterReceiver(this)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_EXPORTED
            )
        } else {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
    }

    private fun installApk(fileName: String) {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (!file.exists()) return

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
