package com.vaticle.typedb.studio

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
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
import java.lang.IllegalStateException
import java.nio.file.Files
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    val (jdkPath, srcFilename, applicationName, mainJar, mainClass, outFilename) = args
    val jdkArchive = File(jdkPath)
//    println("jdkArchive: ${jdkArchive.canonicalPath}, isFile = ${jdkArchive.isFile}")

    untar(archivePath = jdkPath, outputPath = "jdk")

    val jpackage = findJPackage(File("jdk"))
        ?: throw IllegalStateException("Could not locate 'jpackage' in the provided JDK")
    jpackage.setExecutable(true)

    unzip(archivePath = srcFilename, outputPath = "src-temp")

    // Emulate the behaviour of `tar -xf --strip-components=1`
    val files = File("src-temp").listFiles()
    assert(files?.size == 1)
    val sourcesRoot = files!![0]
    assert(sourcesRoot.isDirectory)
    Files.move(sourcesRoot.toPath(), Path.of("src"))

    println(System.getenv("PATH"))
//    runShell(script = listOf("echo", "\$PATH"))

//    runShell(script = listOf("/usr/bin/hdiutil"))

    runShell(script = listOf("pwd"))

    runShell(
        script = listOf(
        jpackage.path,
        "--name", applicationName,
        "--input", "src",
        "--main-jar", mainJar,
        "--main-class", mainClass,
        "-d", "dist"),

        env = mapOf("PATH" to "/usr/bin")
    )

    zip(inputPath = "dist", archivePath = outFilename)
}

operator fun <T> Array<T>.component6(): T {
    return this[5]
}

fun findJPackage(file: File): File? {
//    println("findJPackage: ${file.canonicalPath} - is file? ${file.isFile} - is directory? ${file.isDirectory}")
    if (file.isFile) return if (file.name == "jpackage") file else null
    if (!file.isDirectory) return null
    for (childFile in file.listFiles()!!) {
        val jpackage = findJPackage(childFile)
        if (jpackage != null) return jpackage
    }
    return null
}

fun runShell(script: List<String>, baseDir: Path = Paths.get("."), env: Map<String, String> = mapOf(), expectExitValueNormal: Boolean = true): ProcessResult {
    var builder = ProcessExecutor(script)
        .readOutput(true)
        .redirectOutput(System.out)
        .redirectError(System.err)
        .directory(baseDir.toFile())
        .environment(env)

    if (expectExitValueNormal) builder = builder.exitValueNormal()
    val execution = builder.execute()
    if (execution.exitValue != 0) {
        println("======================================================================================")
        println("ERROR: Execution of $script finished with status code '${execution.exitValue}'")
        println("======================================================================================")
        println()
    }
    return execution
}

fun unzip(archivePath: String, outputPath: String = ".") {
    ZipInputStream(FileInputStream(archivePath)).use { zipInputStream ->
        var zipEntry: ZipEntry? = zipInputStream.nextEntry
        val destDir = File(outputPath)
        val buffer = ByteArray(1024)
        while (zipEntry != null) {
            val newFile = newFile(destDir, zipEntry.name)
            if (zipEntry.isDirectory) {
                if (!newFile.isDirectory && !newFile.mkdirs()) {
                    throw IOException("Failed to create directory $newFile")
                }
            } else {
                // fix for Windows-created archives
                val parent: File = newFile.parentFile
                if (!parent.isDirectory && !parent.mkdirs()) {
                    throw IOException("Failed to create directory $parent")
                }
//                println("Extracting ${zipEntry.name} to ${newFile.canonicalPath}")
                FileOutputStream(newFile).use {
                    var len = zipInputStream.read(buffer)
                    while (len > 0) {
                        it.write(buffer, 0, len)
                        len = zipInputStream.read(buffer)
                    }
                }
            }
            zipEntry = zipInputStream.nextEntry
        }
        zipInputStream.closeEntry()
    }
}

fun newFile(destinationPath: File, name: String): File {
    val destFile = File(destinationPath, name)
    val destDirPath: String = destinationPath.canonicalPath
    val destFilePath = destFile.canonicalPath

    if (!destFilePath.startsWith(destDirPath + File.separator) && destFilePath != destDirPath) {
        throw IOException("Entry is outside of the target dir: $name")
    }

    return destFile
}

fun zip(inputPath: String, archivePath: String) {
    FileOutputStream(archivePath).use {
        ZipOutputStream(it).use { zipOut ->
            val file = File(inputPath)
            zipFile(file, "", zipOut)
        }
    }
}

fun zipFile(file: File, fileName: String, zipOut: ZipOutputStream) {
    if (file.isHidden) return
    if (file.isDirectory) {
        zipOut.putNextEntry(ZipEntry(if (fileName.endsWith("/")) fileName else "$fileName/"))
        zipOut.closeEntry()
        file.listFiles()!!.forEach { childFile ->
            zipFile(childFile, "$fileName/${childFile.name}", zipOut)
        }
        return
    }
    val fileInputStream = FileInputStream(file)
    zipOut.putNextEntry(ZipEntry(fileName))
    val bytes = ByteArray(1024)
    var length = fileInputStream.read(bytes)
    while (length >= 0) {
        zipOut.write(bytes, 0, length)
        length = fileInputStream.read(bytes)
    }
}

fun untar(archivePath: String, outputPath: String = ".") {
    GzipCompressorInputStream(FileInputStream(archivePath)).use { gzipIn ->
        TarArchiveInputStream(gzipIn).use { tarIn ->
            var entry: TarArchiveEntry? = tarIn.nextTarEntry
            val destDir = File(outputPath)
            val buffer = ByteArray(1024)
            while (entry != null) {
                val newFile = newFile(destDir, entry.name)
                if (entry.isDirectory) {
                    if (!newFile.isDirectory && !newFile.mkdirs()) {
                        throw IOException("Failed to create directory $newFile")
                    }
                } else {
                    // fix for Windows-created archives
                    val parent: File = newFile.parentFile
                    if (!parent.isDirectory && !parent.mkdirs()) {
                        throw IOException("Failed to create directory $parent")
                    }
//                    println("Extracting ${entry.name} to ${newFile.canonicalPath}")
                    FileOutputStream(newFile).use {
                        var len = tarIn.read(buffer)
                        while (len > 0) {
                            it.write(buffer, 0, len)
                            len = tarIn.read(buffer)
                        }
                    }
                }
                entry = tarIn.nextTarEntry
            }
//            var entry: TarArchiveEntry? = tarIn.nextTarEntry
//
//            while (entry != null) {
//                /** If the entry is a directory, create the directory. **/
//                if (entry.isDirectory) {
//                    val f = File(outputPath, entry.name)
//                    if (!f.mkdirs()) {
//                        System.out.printf("Unable to create directory '%s' during extraction of archive contents.\n", f.absolutePath)
//                    }
//                } else {
//                    val data = ByteArray(1024)
//                    FileOutputStream(entry.name, false).use {
//                        BufferedOutputStream(it, 1024).use { dest ->
//                            var count = tarIn.read(data, 0, 1024)
//                            while (count != -1) {
//                                dest.write(data, 0, count)
//                                count = tarIn.read(data, 0, 1024)
//                            }
//                        }
//                    }
//                }
//                entry = tarIn.nextTarEntry
        }
    }
}
