package com.thlauncher.app.core.account

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Local offline identity. It does not grant Microsoft/Minecraft online entitlement. */
data class OfflineAccount(
    val id: String,
    val playerName: String,
    val createdAtEpochMillis: Long
) {
    init {
        require(id.isNotBlank())
        require(playerName.matches(Regex("[A-Za-z0-9_]{3,16}"))) {
            "Offline player name must be 3-16 letters, digits, or underscores"
        }
        require(createdAtEpochMillis > 0L)
    }
}

/**
 * Simple persistent local account store. It stores no password or Microsoft token.
 * Keep this account type separate from authenticated Microsoft accounts.
 */
class OfflineAccountRepository(private val storageFile: File) {
    @Synchronized
    fun getAll(): List<OfflineAccount> {
        if (!storageFile.exists()) return emptyList()
        val array = JSONArray(storageFile.readText(Charsets.UTF_8))
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    OfflineAccount(
                        id = item.getString("id"),
                        playerName = item.getString("playerName"),
                        createdAtEpochMillis = item.getLong("createdAtEpochMillis")
                    )
                )
            }
        }
    }

    @Synchronized
    fun create(playerName: String): OfflineAccount {
        val normalized = playerName.trim()
        require(getAll().none { it.playerName.equals(normalized, ignoreCase = true) }) {
            "An offline account with this name already exists"
        }
        val account = OfflineAccount(
            id = UUID.randomUUID().toString(),
            playerName = normalized,
            createdAtEpochMillis = System.currentTimeMillis()
        )
        val all = getAll() + account
        val array = JSONArray()
        all.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("playerName", item.playerName)
                    .put("createdAtEpochMillis", item.createdAtEpochMillis)
            )
        }
        storageFile.parentFile?.let { parent ->
            check(parent.exists() || parent.mkdirs()) { "Cannot create account storage directory" }
        }
        val temporary = File(storageFile.parentFile, storageFile.name + ".tmp")
        temporary.writeText(array.toString(), Charsets.UTF_8)
        check(temporary.renameTo(storageFile)) { "Cannot commit offline account data" }
        return account
    }

    @Synchronized
    fun delete(accountId: String): Boolean {
        val remaining = getAll().filterNot { it.id == accountId }
        if (remaining.size == getAll().size) return false
        val array = JSONArray()
        remaining.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("playerName", item.playerName)
                    .put("createdAtEpochMillis", item.createdAtEpochMillis)
            )
        }
        val temporary = File(storageFile.parentFile, storageFile.name + ".tmp")
        temporary.writeText(array.toString(), Charsets.UTF_8)
        check(temporary.renameTo(storageFile)) { "Cannot commit offline account data" }
        return true
    }
}
