package dev.tarna.randomserver.utils

import kotlinx.serialization.Serializable

@Serializable
data class Server(
    val name: String,
    val ip: String,
    val port: Int,
)