package app.spotygram

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch

fun main() {
    val config = ServerConfig()
    println("MLib Server ${BuildConfig.VERSION_NAME} starting")
    println("  data: ${config.dataDir}")
    println("  cache: ${config.cacheDir} (ttl ${config.cacheTtlMillis / 60_000} min, max ${config.cacheMaxBytes / 1_048_576} MB)")
    println("  http: ${config.host}:${config.port}")
    if (config.streamToken == null) {
        println("  WARNING: MLIB_STREAM_TOKEN is not set; the API is unauthenticated (Wave 1 only)")
    }

    val platform = ServerPlatform(config)
    val app = SpotygramApp(platform)
    app.start()
    runBlocking {
        app.awaitLibrary()
        app.prefs.edit().putBoolean("telegram_connected", true).commit()
        if (config.telegramEnabled) {
            runCatching { app.telegram.start() }
                .onFailure { System.err.println("Telegram start failed: ${it.message}") }
        } else {
            println("Telegram disabled by MLIB_TELEGRAM_ENABLED=0")
        }
    }

    val cache = MediaCache(app, config)
    cache.start()
    val http = HttpApi(app, cache, config)
    http.start()

    if (config.telegramEnabled && config.sourceChat != null) {
        app.scope.launch {
            while (app.telegram.auth.value.type != "authorizationStateReady") {
                delay(2_000)
            }
            val id = config.sourceChat ?: return@launch
            println("Indexing source $id (${config.sourceTitle})")
            runCatching {
                app.library.source(ChatChoice(id, config.sourceTitle), true)
                app.refresh()
            }.onFailure { System.err.println("Source indexing failed: ${it.message}") }
            println("Indexed tracks: ${app.library.state.value.tracks.size}")
        }
    }

    if (config.consoleLogin && config.telegramEnabled) {
        Thread(
            {
                runBlocking {
                    runCatching { ConsoleAuth.login(app, config) }
                        .onFailure { System.err.println("Telegram login: ${it.message}") }
                }
            },
            "MLib console login",
        )
            .apply { isDaemon = true }
            .start()
    }

    val stopped = CountDownLatch(1)
    Runtime.getRuntime().addShutdownHook(
        Thread(
            {
                println("MLib Server stopping")
                http.stop()
                runBlocking { runCatching { app.close() } }
                stopped.countDown()
            },
            "MLib shutdown",
        )
    )
    println("MLib Server ready: http://${config.host}:${config.port}/healthz")
    stopped.await()
}

object ConsoleAuth {
    suspend fun login(app: SpotygramApp, config: ServerConfig) {
        val input = System.`in`.bufferedReader()
        var handled = ""
        while (true) {
            val state = app.telegram.auth.value
            when (state.type) {
                "authorizationStateReady" -> {
                    println("Telegram: ready")
                    return
                }
                "authorizationStateClosed" -> return
                "authorizationStateWaitPhoneNumber" -> {
                    if (handled != state.type) {
                        handled = state.type
                        val value =
                            config.phone ?: withContext(Dispatchers.IO) {
                                print("Telegram phone (international): ")
                                System.out.flush()
                                input.readLine().orEmpty()
                            }
                        if (value.isNotBlank()) app.telegram.submit(value, state)
                    }
                }
                "authorizationStateWaitCode" -> {
                    if (handled != state.type) {
                        handled = state.type
                        val value =
                            config.code ?: withContext(Dispatchers.IO) {
                                print("Telegram code: ")
                                System.out.flush()
                                input.readLine().orEmpty()
                            }
                        if (value.isNotBlank()) app.telegram.submit(value, state)
                    }
                }
                "authorizationStateWaitPassword" -> {
                    if (handled != state.type) {
                        handled = state.type
                        val value =
                            config.password ?: withContext(Dispatchers.IO) {
                                print("Telegram 2FA password: ")
                                System.out.flush()
                                input.readLine().orEmpty()
                            }
                        if (value.isNotBlank()) app.telegram.submit(value, state)
                    }
                }
                else -> {
                    handled = ""
                }
            }
            delay(1_000)
        }
    }
}
