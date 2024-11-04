package dev.tarna.randomserver

import dev.tarna.randomserver.utils.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.event.server.ServerListPingEvent
import net.minestom.server.network.packet.server.common.TransferPacket
import net.minestom.server.ping.ResponseData
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.world.DimensionType

lateinit var client: HttpClient
var servers: List<Server> = emptyList()
var randomServer: Server? = null
var serverStatus: ServerStatus? = null

fun main() {
    val minecraftServer = MinecraftServer.init()

    client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                explicitNulls = false
                ignoreUnknownKeys = true
            })

            register(ContentType.Text.Html, KotlinxSerializationConverter(Json))
            register(ContentType.Text.Plain, KotlinxSerializationConverter(Json))
        }
    }

    val scheduler = MinecraftServer.getSchedulerManager()
    scheduler.submitTask {
        GlobalScope.launch {
            fetchServers()
            println("Fetched ${servers.size} servers")
        }
        TaskSchedule.minutes(5)
    }

    scheduler.submitTask {
        GlobalScope.launch {
            randomServer = servers.randomOrNull() ?: return@launch
            serverStatus = getServerStatus(randomServer ?: return@launch)
            println("Random server: ${randomServer?.name} (${randomServer?.ip}:${randomServer?.port})")
        }
        TaskSchedule.seconds(10)
    }

    val fullbrightDimension = MinecraftServer.getDimensionTypeRegistry().register(
        "randomserver:full_bright",
        DimensionType.builder()
            .ambientLight(2.0f)
            .build()
    )

    val instanceManager = MinecraftServer.getInstanceManager()

    val globalEventHandler = MinecraftServer.getGlobalEventHandler()
    globalEventHandler.addListener(AsyncPlayerConfigurationEvent::class.java) { event ->
        event.spawningInstance = instanceManager.createInstanceContainer(fullbrightDimension)

        val player = event.player
        val playerVersion = player.playerConnection.protocolVersion
        if (playerVersion < 766) {
            player.kick(Component.text("You must be on 1.20.5+ to join this server"))
            return@addListener
        }
    }

    globalEventHandler.addListener(PlayerSpawnEvent::class.java) { event ->
        val player = event.player
        scheduler.scheduleNextTick {
            val server = randomServer ?: return@scheduleNextTick
            player.sendPacket(TransferPacket(server.ip, server.port))
        }
    }

    globalEventHandler.addListener(ServerListPingEvent::class.java) { event ->
        val data = ResponseData()
        if (serverStatus == null) {
            data.description = Component.text("Loading...")
            event.responseData = data
            return@addListener
        }
        data.description = serverStatus?.motd?.raw?.joinToString("\n")?.let { Component.text(it) }
        data.online = serverStatus?.players?.online ?: 0
        data.maxPlayer = serverStatus?.players?.max ?: 0
        data.version = serverStatus?.version
        event.responseData = data

        if (serverStatus?.icon != null) {
            data.favicon = serverStatus?.icon
        }

        println("Pinged server: ${serverStatus?.hostname} (${serverStatus?.ip}:${serverStatus?.port})")
    }

    val port = EnvUtils.env("PORT", "25565").toInt()
    minecraftServer.start("0.0.0.0", port)
    println("Server started on port $port")
}