package dev.tarna.randomserver.utils

import dev.tarna.randomserver.client
import dev.tarna.randomserver.servers
import io.ktor.client.call.*
import io.ktor.client.request.*

suspend fun fetchServers() {
    servers = client.get("https://raw.githubusercontent.com/tarna/random-server/refs/heads/main/servers.json").body<List<Server>>()
}