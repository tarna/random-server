package dev.tarna.randomserver

import dev.tarna.randomserver.utils.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
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
lateinit var servers: List<Server>
lateinit var randomServer: Server
lateinit var serverStatus: ServerStatus

fun main() {
    val minecraftServer = MinecraftServer.init()

    client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                explicitNulls = false
            })
        }
    }

    val scheduler = MinecraftServer.getSchedulerManager()
    scheduler.submitTask {
        GlobalScope.launch {
            fetchServers()
        }
        TaskSchedule.minutes(5)
    }

    scheduler.submitTask {
        GlobalScope.launch {
            randomServer = randomServer()
            serverStatus = getServerStatus(randomServer)
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
        val randomServer = randomServer()
        scheduler.scheduleNextTick {
            player.sendPacket(TransferPacket(randomServer.ip, randomServer.port))
        }
    }

    globalEventHandler.addListener(ServerListPingEvent::class.java) { event ->
        val data = ResponseData()
        data.description = Component.text(serverStatus.motd.raw.joinToString("\n"))
        data.online = serverStatus.players.online
        data.maxPlayer = serverStatus.players.max
        data.version = serverStatus.version
        event.responseData = data
    }

    val port = EnvUtils.env("PORT", "25565").toInt()
    minecraftServer.start("0.0.0.0", port)
    println("Server started on port $port")
}