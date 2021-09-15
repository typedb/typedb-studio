package com.vaticle.typedb.studio

import com.vaticle.typedb.studio.OS.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.ProcessResult
import java.nio.file.Files
import java.util.*
import java.util.Locale.ENGLISH

fun main(args: Array<String>) {
    val config = parseConfig(args[0])
    val verboseLoggingEnabled = config["verbose"].toBoolean()

    val privateConfig = parseConfig(args[1], verboseLoggingEnabled = verboseLoggingEnabled, private = true)
    val applicationFilename = config.require("applicationFilename")
    val version = File(config.require("versionFilePath")).readLines()[0]
    val appleCodeSigningCertURL = privateConfig["appleCodeSigningCertificateUrl"]

    val os = OS.current

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
            "codesign", "-s", "Developer ID Application: Grakn Labs Limited (RHKH8FP9SX)",
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

    val jpackageBinaryName = if (os == WINDOWS) "jpackage.bat" else "jpackage"
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
        if (appleCodeSigningCertURL == null) {
            println("Skipping MacOS code signing step: environment variable APPLE_CODE_SIGNING_CERTIFICATE_URL is not set " +
                    "(it should only be set when deploying a distribution)")
        } else {
            val appleCodeSigningPassword = privateConfig.require("appleCodeSigningPassword")
            val keychainPassword = UUID.randomUUID().toString()
            runShell(listOf("curl", "-o", "code-signing-cert.p12", appleCodeSigningCertURL), printParamsEndIndex = 3)

            // These checks ensure the script doesn't fail if run twice on the same machine, e.g in local testing
            val keychainListInfo = runShell(listOf("security", "list-keychains")).outputString()
            if (keychainName in keychainListInfo) runShell(listOf("security", "delete-keychain", keychainName))
            runShell(listOf("security", "create-keychain", "-p", keychainPassword, keychainName), printParamsEndIndex = 2)
            runShell(listOf("security", "default-keychain", "-s", keychainName))
            runShell(listOf("security", "list-keychains", "-d", "user", "-s", "login.keychain", keychainName))
            runShell(listOf("security", "unlock-keychain", "-p", keychainPassword, keychainName), printParamsEndIndex = 2)
            runShell(listOf("security", "import", "code-signing-cert.p12", "-k", keychainName, "-P", appleCodeSigningPassword, "-T", "/usr/bin/codesign"), printParamsEndIndex = 5)
            runShell(listOf("security", "set-key-partition-list", "-S", "apple-tool:,apple:,codesign:", "-s", "-k", keychainPassword, keychainName), printParamsEndIndex = 4)

            for (file in File("src").listFilesRecursively()) {
                if (!file.isFile) continue

                // Some JARs contain unsigned `.jnilib` files, which we can extract, sign and repackage
                if (file.extension == "jar" && file.name.startsWith("io-netty-netty-")) {
                    var containsJnilib = false
                    val tmpDir = Path.of("tmp")
                    Files.createDirectory(tmpDir)
                    runShell(listOf("jar", "xf", "../${file.path}"), baseDir = tmpDir).outputString()

                    val jarContents = File("tmp").listFilesRecursively()
                    for (jarEntry: File in jarContents) {
                        if (jarEntry.extension == "jnilib") {
                            containsJnilib = true
                            signFile(jarEntry, keychainName)
                        }
                    }

                    if (containsJnilib) {
                        file.setWritable(true)
                        file.delete()
                        runShell(listOf("jar", "cf", file.name, "tmp"))
                    }

                    File("tmp").deleteRecursively()
                }
            }
        }
    }

    // TODO: what about Windows? Can we make the filename nicer, or do we need to build an MSI for that?
    val jpackageScript = mutableListOf(
        jpackage.path,
        "--name", applicationFilename,
        "--app-version", version,
        "--description", "TypeDB's Integrated Development Environment",
        "--vendor", "Vaticle Ltd",
        "--copyright", config["copyrightNotice"] ?: "",
        "--input", "src",
        "--main-jar", config.require("mainJar"),
        "--main-class", config.require("mainClass"),
        "-d", "dist")

    if (os != MAC) {
        // On MacOS, this gets added later, at the DMG step
        jpackageScript += listOf("--license-file", Path.of("src", "LICENSE").toString())
    }

    jpackageScript += when (os) {
        MAC -> listOf(
            "--type", "app-image",
            "--mac-package-name", config.require("applicationName"))
        LINUX -> listOf(
            "--linux-menu-group", "Utility;Development;IDE;",
            "--linux-shortcut",
            "--linux-app-category", "database")
        WINDOWS -> listOf(
            "--win-menu",
            "--win-menu-group", "TypeDB Studio",
            "--win-shortcut")
    }

    if (verboseLoggingEnabled) jpackageScript += "--verbose"

    runShell(jpackageScript)

    if (os == MAC) {
        if (appleCodeSigningCertURL != null) {
            signFile(File("dist/$applicationFilename.app/Contents/runtime"), keychainName, replaceExisting = true)
            signFile(File("dist/$applicationFilename.app"), keychainName, replaceExisting = true)
        }

        runShell(listOf(
            jpackage.path,
            "--name", applicationFilename,
            "--app-version", version,
            "--description", config["description"] ?: "",
            "--vendor", config["vendor"] ?: "",
            "--copyright", config["copyrightNotice"] ?: "",
            "--license-file", "dist/$applicationFilename.app/Contents/app/LICENSE",
            "--type", "dmg",
            "--app-image", "dist/$applicationFilename.app",
            "-d", "dist"))

        File("dist/$applicationFilename.app").deleteRecursively()

        if (appleCodeSigningCertURL == null) {
            if (verboseLoggingEnabled) {
                println("Skipping notarizing step: environment variable APPLE_CODE_SIGNING_CERTIFICATE_URL is not set")
            }
        } else {
            val dmgFilename = "$applicationFilename-$version.dmg"
            val appleID = privateConfig.require("appleId")
            val appleIDPassword = privateConfig.require("appleIdPassword")

            // TODO: xcrun altool --notarize-app is being deprecated in Xcode 13: see
            //       https://developer.apple.com/documentation/security/notarizing_macos_software_before_distribution/customizing_the_notarization_workflow?preferredLanguage=occ
            val notarizeAppProcess = runShell(listOf(
                "xcrun", "altool", "--notarize-app",
                "--primary-bundle-id", "com.vaticle.typedb.studio",
                "--username", appleID,
                "--password", appleIDPassword,
                "--file", "dist/$dmgFilename"))
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

            runShell(listOf("xcrun", "stapler", "staple", "dist/$dmgFilename"))
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

    operator fun get(key: String): String? {
        return config[key]
    }

    fun require(key: String): String {
        val value = config[key]
        if (value.isNullOrBlank()) throw IllegalStateException("Configuration object is missing required property '$key'")
        return value
    }
}

enum class OS {
    WINDOWS,
    MAC,
    LINUX;

    companion object {
        val current: OS
        get() {
            val osName = System.getProperty("os.name").lowercase(ENGLISH)
            return when {
                "mac" in osName || "darwin" in osName -> MAC
                "win" in osName -> WINDOWS
                else -> LINUX
            }
        }
    }
}

fun File.listFilesRecursively(): Collection<File> {
    if (isFile) return listOf(this)
    if (!isDirectory) return emptyList()
    return listFiles()!!.flatMap { it.listFilesRecursively() }
}

// TODO: delete unzip and zip if jar is proven to work on all OSes
//fun unzip(archivePath: String, outputPath: String = ".") {
//    ZipInputStream(FileInputStream(archivePath)).use { zipInputStream ->
//        var zipEntry: ZipEntry? = zipInputStream.nextEntry
//        val destDir = File(outputPath)
//        val buffer = ByteArray(1024)
//        while (zipEntry != null) {
//            val newFile = newFile(destDir, zipEntry.name)
//            if (zipEntry.isDirectory) {
//                if (!newFile.isDirectory && !newFile.mkdirs()) {
//                    throw IOException("Failed to create directory $newFile")
//                }
//            } else {
//                // fix for Windows-created archives
//                val parent: File = newFile.parentFile
//                if (!parent.isDirectory && !parent.mkdirs()) {
//                    throw IOException("Failed to create directory $parent")
//                }
//                FileOutputStream(newFile).use {
//                    var len = zipInputStream.read(buffer)
//                    while (len > 0) {
//                        it.write(buffer, 0, len)
//                        len = zipInputStream.read(buffer)
//                    }
//                }
//            }
//            zipEntry = zipInputStream.nextEntry
//        }
//        zipInputStream.closeEntry()
//    }
//}
//
//fun newFile(destinationPath: File, name: String): File {
//    val destFile = File(destinationPath, name)
//    val destDirPath: String = destinationPath.canonicalPath
//    val destFilePath = destFile.canonicalPath
//
//    if (!destFilePath.startsWith(destDirPath + File.separator) && destFilePath != destDirPath) {
//        throw IOException("Entry is outside of the target dir: $name")
//    }
//
//    return destFile
//}
//
//fun zip(inputPath: String, archivePath: String) {
//    FileOutputStream(archivePath).use {
//        ZipOutputStream(it).use { zipOut ->
//            val file = File(inputPath)
//            zipFile(file, "", zipOut)
//        }
//    }
//}
//
//fun zipFile(file: File, fileName: String, zipOut: ZipOutputStream) {
//    if (file.isHidden) return
//    if (file.isDirectory) {
//        zipOut.putNextEntry(ZipEntry(if (fileName.endsWith("/")) fileName else "$fileName/"))
//        zipOut.closeEntry()
//        file.listFiles()!!.forEach { childFile ->
//            zipFile(childFile, "$fileName/${childFile.name}", zipOut)
//        }
//        return
//    }
//    val fileInputStream = FileInputStream(file)
//    zipOut.putNextEntry(ZipEntry(fileName))
//    val bytes = ByteArray(1024)
//    var length = fileInputStream.read(bytes)
//    while (length >= 0) {
//        zipOut.write(bytes, 0, length)
//        length = fileInputStream.read(bytes)
//    }
//}
