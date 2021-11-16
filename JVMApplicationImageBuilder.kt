/*
 * Copyright (C) 2021 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.vaticle.typedb.studio

import com.vaticle.typedb.studio.OS.*
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.ProcessResult
import java.nio.file.Files
import java.util.Locale.ENGLISH

fun main(args: Array<String>) {
    val config = parseConfig(args[0])
    val verboseLoggingEnabled = config["verbose"].toBoolean()

    val privateConfig = parseConfig(args[1], verboseLoggingEnabled = verboseLoggingEnabled, private = true)
    val applicationFilename = config.require("applicationFilename")
    val version = File(config.require("versionFilePath")).readLines()[0]
    val appleCodeSigningPassword = privateConfig["appleCodeSigningPassword"]

    val os = getCurrentOS()

    fun runShell(script: List<String>, baseDir: Path = Paths.get("."), env: Map<String, String> = mapOf(),
                 expectExitValueNormal: Boolean = true, printParamsEndIndex: Int? = null): ProcessResult {
        var builder = ProcessExecutor(script)
            .readOutput(true)
            .redirectError(System.err)
            .directory(baseDir.toFile())
            .environment(env)

        if (verboseLoggingEnabled) builder = builder.redirectOutput(System.out)
        if (expectExitValueNormal) builder = builder.exitValueNormal()
        val execution = builder.execute()
        if (execution.exitValue != 0 || verboseLoggingEnabled) {
            val loggedScript = when (printParamsEndIndex) {
                null -> "$script"
                else -> "${script.subList(0, printParamsEndIndex)} (+${script.size - printParamsEndIndex} hidden argument(s))"
            }
            println("Execution of $loggedScript finished with status code '${execution.exitValue}'")
        }
        return execution
    }

    fun signFile(file: File, keychainName: String, deep: Boolean = false, replaceExisting: Boolean = false) {
        if (!replaceExisting) {
            val verifySignatureResult = runShell(listOf("codesign", "-v", "--strict", file.path), expectExitValueNormal = false)
            if (verifySignatureResult.exitValue == 0) return // file is already signed, skip
            if (verifySignatureResult.exitValue != 1) throw IllegalStateException("Command 'codesign' failed with exit code " +
                    "${verifySignatureResult.exitValue} and output: ${verifySignatureResult.outputString()}")
        }

        file.setWritable(true)
        val signCommand: MutableList<String> = mutableListOf(
            "codesign", "-s", "Developer ID Application: Vaticle LTD (RHKH8FP9SX)",
            "-f",
            "--entitlements", config.require("macEntitlementsPath"),
            "--prefix", "com.vaticle.typedb.studio.",
            "--options", "runtime",
            "--timestamp",
            "--keychain", keychainName,
            file.path)
        if (deep) signCommand += "--deep"
        if (verboseLoggingEnabled) signCommand += "-vvv"
        runShell(signCommand)
    }

    // Extract JDK
    Files.createDirectory(Path.of("jdk"))
    val jdkArchivePath = config.require("jdkPath")
    when (os) {
        MAC, LINUX -> runShell(script = listOf("tar", "-xf", jdkArchivePath, "-C", "jdk"))
        WINDOWS -> runShell(script = listOf("jar", "xf", Path.of("..", jdkArchivePath).toString()), baseDir = Path.of("jdk"))
    }

    // On Windows, extract WiX Toolset and add it to the PATH
    if (os == WINDOWS) {
        Files.createDirectory(Path.of("wixtoolset"))
        val wixToolsetPath = config.require("windowsWixToolsetPath")
        runShell(script = listOf("jar", "xf", Path.of("..", wixToolsetPath).toString()), baseDir = Path.of("wixtoolset"))
    }

    val jpackageBinaryName = if (os == WINDOWS) "jpackage.exe" else "jpackage"
    val jpackage = File("jdk").listFilesRecursively().firstOrNull { it.name == jpackageBinaryName }
        ?: throw IllegalStateException("Could not locate '$jpackageBinaryName' in the provided JDK")

    Files.createDirectory(Path.of("src-temp"))
    runShell(script = listOf("jar", "xf", Path.of("..", config.require("srcFilename")).toString()),
        baseDir = Path.of("src-temp"))
//    unzip(archivePath = config.require("srcFilename"), outputPath = "src-temp")
//
    // Emulate the behaviour of `tar -xf --strip-components=1`
    // TODO: this is necessary because of our own ZIP structure, right?
    val files = File("src-temp").listFiles()
    assert(files!!.size == 1)
    assert(files[0].isDirectory)
    Files.move(files[0].toPath(), Path.of("src"))

    val keychainName = "jvm-application-image-builder.keychain"

    if (os == MAC) {
        if (appleCodeSigningPassword == null) {
            println("Skipping MacOS code signing step: variable APPLE_CODE_SIGNING_PASSWORD is not set " +
                    "(it should only be set when deploying a distribution)")
        } else {
            val appleCodeSigningCertPath = config.require("appleCodeSigningCertificatePath")
            val keychainPassword = "jvm-application-image-builder"
//            runShell(listOf("curl", "-o", "code-signing-cert.p12", appleCodeSigningCertURL), printParamsEndIndex = 3)

            // These checks ensure the script doesn't fail if run twice on the same machine, e.g in local testing
            val keychainListInfo = runShell(listOf("security", "list-keychains")).outputString()
            if (keychainName in keychainListInfo) runShell(listOf("security", "delete-keychain", keychainName))
            runShell(listOf("security", "create-keychain", "-p", keychainPassword, keychainName), printParamsEndIndex = 2)
            runShell(listOf("security", "default-keychain", "-s", keychainName))
            runShell(listOf("security", "list-keychains", "-d", "user", "-s", "login.keychain", keychainName))
            runShell(listOf("security", "unlock-keychain", "-p", keychainPassword, keychainName), printParamsEndIndex = 2)
            runShell(listOf("security", "import", appleCodeSigningCertPath, "-k", keychainName, "-P", appleCodeSigningPassword, "-T", "/usr/bin/codesign"), printParamsEndIndex = 5)
            runShell(listOf("security", "set-key-partition-list", "-S", "apple-tool:,apple:,codesign:", "-s", "-k", keychainPassword, keychainName), printParamsEndIndex = 4)

            for (file in File("src").listFilesRecursively()) {
                if (!file.isFile) continue

                // Some JARs contain unsigned `.jnilib` and `.dylib` files, which we can extract, sign and repackage
                if (file.extension == "jar" && (file.name.startsWith("io-netty-netty-") || "skiko-jvm-runtime" in file.name)) {
                    var containsNativeLib = false
                    val tmpDir = Path.of("tmp")
                    Files.createDirectory(tmpDir)
                    runShell(listOf("jar", "xf", "../${file.path}"), baseDir = tmpDir).outputString()

                    val jarContents = File("tmp").listFilesRecursively()
                    for (jarEntry: File in jarContents) {
                        if (jarEntry.extension in listOf("jnilib", "dylib")) {
                            containsNativeLib = true
                            signFile(jarEntry, keychainName)
                        }
                    }

                    if (containsNativeLib) {
                        file.setWritable(true)
                        file.delete()
                        runShell(script = listOf("jar", "cMf", "../${file.path}", "."), baseDir = tmpDir)
                    }

                    File("tmp").deleteRecursively()
                }
            }
        }
    }

    val shortVersion = version.split("-")[0] // e.g: 2.0.0-alpha5 -> 2.0.0

    // TODO: what about Windows? Can we make the filename nicer, or do we need to build an MSI for that?
    val jpackageScript = mutableListOf(
        jpackage.path,
        "--name", applicationFilename,
        "--app-version", shortVersion,
        "--description", "TypeDB's Integrated Development Environment",
        "--vendor", "Vaticle Ltd",
        "--copyright", config["copyrightNotice"] ?: "",
        "--input", "src",
        "--main-jar", config.require("mainJar"),
        "--main-class", config.require("mainClass"),
        "-d", "dist")

    if ("iconPath" in config) {
        jpackageScript += listOf("--icon", config.require("iconPath"))
    }

    if (os != MAC) {
        // On MacOS, this gets added later, at the DMG step
        jpackageScript += listOf("--license-file", Path.of("src", "LICENSE").toString())
    }

    jpackageScript += when (os) {
        MAC -> listOf(
            "--type", "app-image",
            "--mac-package-name", config.require("applicationName"))
        LINUX -> listOf(
            "--type", "deb",
            "--linux-menu-group", "Utility;Development;IDE;",
            "--linux-shortcut",
            "--linux-app-category", "database")
        WINDOWS -> listOf(
            "--type", "exe",
            "--win-menu",
            "--win-menu-group", "TypeDB Studio",
            "--win-shortcut")
    }

    if (verboseLoggingEnabled) jpackageScript += "--verbose"

    val env: Map<String, String> = when (os) {
        MAC, LINUX -> mapOf()
        WINDOWS -> mapOf("PATH" to "${File("wixtoolset").absolutePath};${System.getenv("PATH") ?: ""}")
    }
    runShell(script = jpackageScript, env = env)
    if (os != MAC) {
        val distFile = File("dist").listFiles()!![0]
        distFile.renameTo(File(distFile.path.replace(shortVersion, version)))
    }

    if (os == MAC) {
        if (appleCodeSigningPassword != null) {
            signFile(File("dist/$applicationFilename.app/Contents/runtime"), keychainName, replaceExisting = true)
            signFile(File("dist/$applicationFilename.app"), keychainName, replaceExisting = true)
        }

        runShell(listOf(
            jpackage.path,
            "--name", applicationFilename,
            "--app-version", shortVersion,
            "--description", config["description"] ?: "",
            "--vendor", config["vendor"] ?: "",
            "--copyright", config["copyrightNotice"] ?: "",
            "--license-file", "dist/$applicationFilename.app/Contents/app/LICENSE",
            "--type", "dmg",
            "--app-image", "dist/$applicationFilename.app",
            "-d", "dist"))

        File("dist/$applicationFilename.app").deleteRecursively()
        val distFile = File("dist").listFiles()!![0]
        distFile.renameTo(File(distFile.path.replace(shortVersion, version)))

        if (appleCodeSigningPassword == null) {
            if (verboseLoggingEnabled) {
                println("Skipping notarizing step: variable APPLE_CODE_SIGNING_PASSWORD is not set")
            }
        } else {
            val dmgFilename = "$applicationFilename-$version.dmg"
            val dmgFilePath = "dist/$dmgFilename"
            signFile(File(dmgFilePath), keychainName)

            val appleID = privateConfig.require("appleId")
            val appleIDPassword = privateConfig.require("appleIdPassword")

            // TODO: xcrun altool --notarize-app is being deprecated in Xcode 13: see
            //       https://developer.apple.com/documentation/security/notarizing_macos_software_before_distribution/customizing_the_notarization_workflow?preferredLanguage=occ
            val notarizeAppProcess = runShell(listOf(
                "xcrun", "altool", "--notarize-app",
                "--primary-bundle-id", "com.vaticle.typedb.studio",
                "--username", appleID,
                "--password", appleIDPassword,
                "--file", dmgFilePath))
            val notarizeAppResult = notarizeAppProcess.outputString()
            val requestUUID = Regex("RequestUUID = ([a-z0-9\\-]{36})").find(notarizeAppResult)?.groupValues?.get(1)
                ?: throw IllegalStateException("Notarization failed: the response $notarizeAppResult from " +
                        "'xcrun altool --notarize-app' does not contain a valid RequestUUID")
            println("Notarization request UUID: $requestUUID")

            var retries = 0
            val maxRetries = 30 /* 15 minutes */
            while (retries < maxRetries) {
                Thread.sleep(30000)

                val infoProcess = runShell(listOf(
                    "xcrun", "altool", "--notarization-info",
                    requestUUID,
                    "--username", appleID,
                    "--password", appleIDPassword))
                val info = infoProcess.outputString()

                if ("Status Message: Package Approved" in info) {
                    println("$dmgFilename was APPROVED by the Apple notarization service")
                    break
                }

                // Apple log file is generated a few seconds after a validation success/error. So we should wait
                // until it has been generated.
                if ("LogFileURL" in info) { // and package is not approved
                    throw IllegalStateException("$dmgFilename was REJECTED by the Apple notarization service\n$info")
                }

                retries++
            }

            runShell(listOf("xcrun", "stapler", "staple", dmgFilePath))
        }
    }

    runShell(script = listOf("jar", "cMf", Path.of("..", config.require("outFilename")).toString(), "."),
        baseDir = Path.of("dist"))
}

fun parseConfig(config: String, verboseLoggingEnabled: Boolean = false, private: Boolean = false): Config {
    val parsedConfig: Map<String, String> = config.lines()
        .filter { line -> ":" in line && !line.startsWith("#") }
        .associate { line ->
            val components = line.split(":", limit = 2)
            return@associate components[0].trim() to components[1].trim()
        }
    if (parsedConfig.isNotEmpty() && (verboseLoggingEnabled || parsedConfig["verbose"].toBoolean())) {
        println()
        println("Parsed configuration object: ")
        parsedConfig.forEach { (key, value) -> println("$key=${if (private) "*******" else value}") }
        println()
    }
    return Config(parsedConfig)
}

data class Config(private val config: Map<String, String>) {

    operator fun contains(key: String) = key in config

    operator fun get(key: String) = config[key]

    fun require(key: String): String {
        val value = config[key]
        if (value.isNullOrBlank()) throw IllegalStateException("Configuration object is missing required property '$key'")
        return value
    }
}

enum class OS {
    WINDOWS,
    MAC,
    LINUX
}

fun getCurrentOS(): OS {
    val osName = System.getProperty("os.name").lowercase(ENGLISH)
    return when {
        "mac" in osName || "darwin" in osName -> MAC
        "win" in osName -> WINDOWS
        else -> LINUX
    }
}

fun File.listFilesRecursively(): Collection<File> {
    if (isFile) return listOf(this)
    if (!isDirectory) return emptyList()
    return listFiles()!!.flatMap { it.listFilesRecursively() }
}
