import java.util.Properties
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    application
    id("org.jetbrains.compose")
}

version = rootProject.version
application { mainClass.set("app.spotygram.ServerMainKt") }

configurations.runtimeClasspath {
    // The org.jetbrains shim jars only carry module metadata; androidx has the classes.
    exclude(group = "org.jetbrains.compose.runtime", module = "runtime-desktop")
    exclude(group = "org.jetbrains.compose.runtime", module = "runtime-saveable-desktop")
    exclude(group = "org.jetbrains.compose.runtime", module = "runtime-retain-desktop")
}

tasks.withType<org.gradle.api.tasks.Copy>().configureEach {
    duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
}
tasks.withType<org.gradle.api.tasks.Sync>().configureEach {
    duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
}
val buildState = rootProject.extra["mlibState"] as String
val definitions = layout.buildDirectory.dir("generated/definitions")
val generateDefinitions = tasks.register("generateServerDefinitions") {
    val strings = rootProject.file("shared/src/main/res/values/strings.xml")
    val drawables = rootProject.fileTree("shared/src/main/res/drawable-nodpi") { include("*.png") }
    inputs.file(strings)
    inputs.files(drawables)
    inputs.property("version", rootProject.version.toString())
    inputs.files(listOfNotNull(file("$buildState/telegram.env").takeIf { it.isFile }))
    inputs.property(
        "telegramConfiguration",
        MessageDigest.getInstance("SHA-256")
            .digest("${System.getenv("TELEGRAM_API_ID").orEmpty()}\n${System.getenv("TELEGRAM_API_HASH").orEmpty()}".toByteArray())
            .joinToString("") { "%02x".format(it) },
    )
    outputs.dir(definitions)
    doLast {
        val credentials = Properties().apply {
            val configuration = file("$buildState/telegram.env")
            if (configuration.isFile) configuration.inputStream().use { load(it) }
        }
        fun credential(name: String) =
            (System.getenv(name) ?: credentials.getProperty(name, "")).trim().trim('"', '\'')
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(strings)
        val output = definitions.get().asFile.resolve("app/spotygram").apply { mkdirs() }
        val names = linkedMapOf<Int, String>()
        var id = 1
        output.resolve("R.kt").writeText(
            buildString {
                appendLine("package app.spotygram")
                appendLine("object R {")
                for (type in listOf("string", "plurals")) {
                    appendLine("    object $type {")
                    val nodes = document.getElementsByTagName(type)
                    for (i in 0 until nodes.length) {
                        val name = (nodes.item(i) as org.w3c.dom.Element).getAttribute("name")
                        names[id] = name
                        appendLine("        const val $name = ${id++}")
                    }
                    appendLine("    }")
                }
                appendLine("    object drawable {")
                val drawableNames = drawables.files.sortedBy { it.name }.map { it.nameWithoutExtension }
                drawableNames.forEachIndexed { index, name ->
                    appendLine("        const val $name = ${10000 + index}")
                }
                appendLine("        val names = mapOf(")
                drawableNames.forEach { name -> appendLine("            $name to \"$name\",") }
                appendLine("        )")
                appendLine("    }")
                appendLine("    val names = mapOf(")
                names.forEach { (number, name) -> appendLine("            $number to \"$name\",") }
                appendLine("    )")
                appendLine("}")
            },
        )
        val apiId = credential("TELEGRAM_API_ID").toIntOrNull() ?: 0
        val apiHash = credential("TELEGRAM_API_HASH").replace("\\", "\\\\").replace("\"", "\\\"")
        output.resolve("BuildConfig.kt").writeText(
            """
            package app.spotygram
            object BuildConfig {
                const val VERSION_NAME = "${rootProject.version}"
                const val APPLICATION_ID = "pt.ginjeira.mlib"
                const val DEBUG = false
                const val TELEGRAM_API_ID = $apiId
                const val TELEGRAM_API_HASH = "$apiHash"
                const val TELEGRAM_TEST_DC = false
            }
            """.trimIndent(),
        )
    }
}

kotlin {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    sourceSets.main {
        kotlin.srcDir(rootProject.file("shared/src/main/kotlin"))
        kotlin.srcDir(definitions)
    }
}
sourceSets.main { resources.srcDir(rootProject.file("shared/src/main/res")) }
tasks.named("compileKotlin").configure { dependsOn(generateDefinitions) }

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    implementation("org.json:json:20250517")
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")
    implementation("net.java.dev.jna:jna:5.17.0")
    implementation("net.java.dev.jna:jna-platform:5.17.0")
    implementation("net.jthink:jaudiotagger:3.0.1")
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("org.slf4j:slf4j-nop:2.0.17")
    testImplementation("junit:junit:4.13.2")
}

tasks.register<JavaExec>("server") {
    group = "application"
    description = "Run the MLib server"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("app.spotygram.ServerMainKt")
    jvmArgs("-Xmx512m", "-XX:+UseG1GC")
    providers.gradleProperty("serverData").orNull?.let { jvmArgs("-Dmlib.server.data=$it") }
    providers.gradleProperty("serverNative").orNull?.let { jvmArgs("-Dspotygram.native.dir=$it") }
    providers.gradleProperty("serverPort").orNull?.let { environment("MLIB_SERVER_PORT", it) }
}
