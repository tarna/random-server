package dev.tarna.randomserver.utils

import io.github.cdimascio.dotenv.Dotenv

/**
 * Utility class for accessing environment variables
 *
 * @see <a href="https://github.com/SantioMC/MinehutUtils/blob/master/src/main/kotlin/me/santio/minehututils/utils/EnvUtils.kt">EnvUtils.kt</a>
 */
@Suppress("unused")
object EnvUtils {
    private val dotenv = Dotenv.configure().ignoreIfMissing().load()

    fun env(key: String, def: String? = null): String {
        return env(key) ?: def ?: throw IllegalStateException("Missing environment variable $key")
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> env(key: String, def: T): T {
        return env(key) as? T ?: def
    }

    fun env(key: String): String? {
        return dotenv[key.uppercase()]
            ?: System.getenv(key.uppercase())
    }

}