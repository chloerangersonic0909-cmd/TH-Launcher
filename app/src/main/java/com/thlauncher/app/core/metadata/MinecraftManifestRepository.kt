package com.thlauncher.app.core.metadata

import com.thlauncher.app.core.model.MinecraftVersion
import com.thlauncher.app.core.model.MinecraftVersionType
import org.json.JSONObject
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reads Mojang's public version manifest. No credentials are sent.
 * Network work is synchronous by design; call from a worker thread, never the UI thread.
 */
class MinecraftManifestRepository(
    private val manifestUrl: String = MANIFEST_URL,
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 20_000
) {
    fun fetchVersions(): List<MinecraftVersion> {
        val connection = (URL(manifestUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "TH-Launcher/0.1.0 (Android)")
        }

        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IllegalStateException("Version manifest request failed: HTTP $status")
            }

            val body = BufferedInputStream(connection.inputStream).bufferedReader().use { it.readText() }
            val root = JSONObject(body)
            val versions = root.getJSONArray("versions")
            return buildList(versions.length()) {
                for (index in 0 until versions.length()) {
                    val item = versions.getJSONObject(index)
                    val type = when (item.optString("type")) {
                        "release" -> MinecraftVersionType.RELEASE
                        "snapshot" -> MinecraftVersionType.SNAPSHOT
                        "old_beta" -> MinecraftVersionType.OLD_BETA
                        else -> MinecraftVersionType.OLD_ALPHA
                    }
                    add(
                        MinecraftVersion(
                            id = item.getString("id"),
                            type = type,
                            url = item.getString("url"),
                            releaseTime = item.optString("releaseTime"),
                            javaMajorVersion = null
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val MANIFEST_URL =
            "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"
    }
}
