import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

plugins {
    kotlin("jvm") version "2.0.21"
    id("com.gradleup.shadow") version "8.3.5"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "dev.webkom.jarveCraft"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    runServer {
        val localJar = file("server/paper-server.jar")
        if (localJar.exists()) {
            serverJar(localJar)
        }
        minecraftVersion("1.21.4")
        jvmArgs("-Xms3G", "-Xmx4G", "-XX:+UseG1GC")
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}

abstract class SyncPaintingsTask : DefaultTask() {
    @get:InputDirectory
    @get:Optional
    abstract val paintingsDir: DirectoryProperty

    @get:OutputDirectory
    abstract val targetDir: DirectoryProperty

    @get:OutputFile
    abstract val manifestFile: RegularFileProperty

    @TaskAction
    fun sync() {
        val target = targetDir.get().asFile
        val manifest = manifestFile.get().asFile
        val paintings = paintingsDir.orNull?.asFile

        target.mkdirs()

        val squareSlots = mutableListOf(
            "bust", "match", "skull_and_roses", "stage", "void", "wither",
            "baroque", "humble", "unpacked", "bouquet", "cavebird", "cotoni",
            "endfern", "finding", "lowmist", "orb", "passage", "pond",
            "sunflowers", "tides", "water", "wind",
            "kebab", "aztec", "alban", "aztec2", "bomb", "plant", "wasteland", "meditative",
            "skeleton", "burning_skull", "donkey_kong"
        )
        val landscape2x1Slots = mutableListOf(
            "pool", "courbet", "sea", "sunset", "creebet", "fighters", "changing"
        )
        val landscape4x3Slots = mutableListOf(
            "pointer", "pigscene"
        )
        val portraitSlots = mutableListOf(
            "wanderer", "graham", "prairie_ride"
        )

        val validExtensions = setOf("png", "jpg", "jpeg", "bmp", "gif")
        val activePaintings = mutableSetOf<String>()

        // If any raw images were dropped directly into target, move them to paintings/
        if (paintings != null && paintings.exists()) {
            target.listFiles()?.filter {
                val isVanillaSlot = it.extension.equals("png", ignoreCase = true) &&
                    (it.nameWithoutExtension.lowercase() in squareSlots ||
                     it.nameWithoutExtension.lowercase() in landscape2x1Slots ||
                     it.nameWithoutExtension.lowercase() in landscape4x3Slots ||
                     it.nameWithoutExtension.lowercase() in portraitSlots)
                !isVanillaSlot && it.isFile && it.extension.lowercase() in validExtensions
            }?.forEach { rawFile ->
                val dest = File(paintings, rawFile.name)
                rawFile.renameTo(dest)
                println("Moved raw image '${rawFile.name}' from resourcepack to paintings/ for processing")
            }
        }

        // Clean target directory so only active custom paintings exist
        target.listFiles()?.forEach { it.delete() }

        if (paintings != null && paintings.exists() && paintings.isDirectory) {
            val imageFiles = paintings.walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in validExtensions }
                .sortedBy { it.name.lowercase() }
                .toList()

            for (imgFile in imageFiles) {
                val baseName = imgFile.nameWithoutExtension.lowercase()
                val parentDir = imgFile.parentFile.name.lowercase()

                val targetSlot: String? = when {
                    baseName in squareSlots || baseName in landscape2x1Slots ||
                    baseName in landscape4x3Slots || baseName in portraitSlots ||
                    activePaintings.contains(baseName.uppercase()) -> baseName

                    parentDir == "square" -> squareSlots.removeFirstOrNull()
                    parentDir == "landscape" -> landscape2x1Slots.removeFirstOrNull() ?: landscape4x3Slots.removeFirstOrNull()
                    parentDir == "portrait" -> portraitSlots.removeFirstOrNull()

                    else -> {
                        try {
                            val img = javax.imageio.ImageIO.read(imgFile)
                            if (img != null) {
                                val ratio = img.width.toDouble() / img.height.toDouble()
                                when {
                                    ratio >= 1.5 -> landscape2x1Slots.removeFirstOrNull() ?: squareSlots.removeFirstOrNull()
                                    ratio >= 1.15 && ratio < 1.5 -> landscape4x3Slots.removeFirstOrNull() ?: landscape2x1Slots.removeFirstOrNull() ?: squareSlots.removeFirstOrNull()
                                    ratio <= 0.85 -> portraitSlots.removeFirstOrNull() ?: squareSlots.removeFirstOrNull()
                                    else -> squareSlots.removeFirstOrNull()
                                }
                            } else squareSlots.removeFirstOrNull()
                        } catch (e: Exception) {
                            squareSlots.removeFirstOrNull()
                        }
                    }
                }

                if (targetSlot != null) {
                    squareSlots.remove(targetSlot)
                    landscape2x1Slots.remove(targetSlot)
                    landscape4x3Slots.remove(targetSlot)
                    portraitSlots.remove(targetSlot)

                    val destFile = File(target, "$targetSlot.png")
                    val rawImg = javax.imageio.ImageIO.read(imgFile)
                    if (rawImg != null) {
                        val maxDim = 1024
                        val (targetW, targetH) = if (rawImg.width > maxDim || rawImg.height > maxDim) {
                            if (rawImg.width >= rawImg.height) {
                                maxDim to (rawImg.height * maxDim / rawImg.width)
                            } else {
                                (rawImg.width * maxDim / rawImg.height) to maxDim
                            }
                        } else {
                            rawImg.width to rawImg.height
                        }

                        val processedImg = BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB)
                        val g = processedImg.createGraphics()
                        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                        g.drawImage(rawImg, 0, 0, targetW, targetH, null)
                        g.dispose()

                        ImageIO.write(processedImg, "png", destFile)
                        activePaintings.add(targetSlot.uppercase())
                        println("  [Painting] Mapped '${imgFile.name}' (${rawImg.width}x${rawImg.height}) -> $targetSlot.png (${targetW}x${targetH})")
                    }
                } else {
                    println("  [Warning] No available painting slots left for '${imgFile.name}'!")
                }
            }
        }

        manifest.parentFile.mkdirs()
        manifest.writeText(activePaintings.sorted().joinToString("\n"))
        println("syncPaintings: Registered ${activePaintings.size} custom painting(s) in custom_paintings.txt")
    }
}

val syncPaintings = tasks.register<SyncPaintingsTask>("syncPaintings") {
    group = "jarvecraft"
    description = "Syncs and maps images from paintings/ folder into the resource pack"
    paintingsDir.set(layout.projectDirectory.dir("paintings"))
    targetDir.set(layout.projectDirectory.dir("src/main/resources/resourcepack/assets/minecraft/textures/painting"))
    manifestFile.set(layout.projectDirectory.file("src/main/resources/custom_paintings.txt"))
}

val packageResourcePack = tasks.register<Zip>("packageResourcePack") {
    archiveFileName.set("pack.zip")
    from("src/main/resources/resourcepack")
    dependsOn(syncPaintings)
}

tasks.processResources {
    dependsOn(syncPaintings)
}