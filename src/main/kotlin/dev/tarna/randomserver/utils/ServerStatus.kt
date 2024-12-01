package dev.tarna.randomserver.utils

import dev.tarna.randomserver.client
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ServerStatus(
    val ip: String,
    val port: Int,
    val debug: JsonObject,
    val motd: Motd?,
    val players: Players?,
    val version: String?,
    val online: Boolean,
    val protocol: Protocol?,
    val hostname: String?,
    val icon: String?,
    val software: String?,
    val eula_blocked: Boolean?,
) {
    @Serializable
    data class Motd(
        val raw: List<String>,
        val clean: List<String>,
        val html: List<String>
    )

    @Serializable
    data class Players(
        val online: Int,
        val max: Int,
    )

    @Serializable
    data class Protocol(
        val name: String,
        val version: Int
    )
}

suspend fun getServerStatus(server: Server): ServerStatus {
    println("Fetching server status for ${server.ip}:${server.port}")
    return client.get("https://api.mcsrvstat.us/3/${server.ip}").body<ServerStatus>()
}