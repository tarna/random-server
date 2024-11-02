package dev.tarna.randomserver.utils

import dev.tarna.randomserver.client
import dev.tarna.randomserver.servers
import io.ktor.client.call.*
import io.ktor.client.request.*

suspend fun fetchServers() {
    servers = client.get("https://github.com/tarna/minehut/blob/master/servers.json").body<List<Server>>()
}

fun randomServer(): Server {
    return servers.random()
}