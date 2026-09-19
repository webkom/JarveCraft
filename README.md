# JarveCraft

> A custom Minecraft (Paper 1.21.4) Plugin with IoT integration for the Webkom office, customized for a great Dragon Takedown Thursday experience

---

## Features

- **Jägermeister Splash Potions**:

  - Custom shaped recipe: 1 Glass Bottle surrounded by 3 Diamonds.


    |         |              |         |
    | ------- | ------------ | ------- |
    |         | Diamond      |         |
    | Diamond | Glass bottle | Diamond |
    |         |              |         |
  - Increments the player's shot counter on the sidebar
- **Live Shots Scoreboard**:

  - Automatically displayed on the sidebar upon joining.
  - Increments when drinking/splashing Jägermeister, respawning from death, or hitting specific milestones.
- **Real-World WLED Room Lighting**:

  - Synchronizes your real room's LED strip with in-game dimensions:
    - **Overworld**: Ambient baseline theme.
    - **Nether**: Intense fiery red/orange strobe effect.
    - **The End**: Purple and golden glow.
  - **Death Alert**: Flashes the entire room blood-red whenever a player dies, then restores previous ambient lighting.
- **Automated Paintings Pipeline**:

  - Drop custom photos into `paintings/` (JPEG, PNG, etc.).
  - Gradle automatically inspects aspect ratios, resizes/optimizes them, maps them to vanilla painting slots, and writes a manifest for the resource pack.
  - `PaintingRandomizer` guarantees that placing a painting in-world only selects from your custom images

---

## Requirements

- **Java**: JDK 21+
- **Minecraft**: Paper 1.21.4
- **WLED** *(Optional)*: An ESP8266/ESP32 running [WLED](https://kno.wled.ge/) on your local Wi-Fi. If you are in the office, connect to the office network and you should be able to reach it via disco.local or ip 192.168.1.67

---

## Quick Start

### 1. Build the Plugin

```bash
./gradlew build
```

The compiled shadow JAR will be generated at `build/libs/JarveCraft-1.0.0-all.jar`.

### 2. Run the Local Test Server

```bash
./gradlew runServer
```

Powered by the `run-paper` plugin. If `server/paper-server.jar` is present, it will use that server JAR; otherwise, it will automatically download the latest Paper 1.21.4 release.

### 3. Package the Resource Pack

```bash
./gradlew packageResourcePack
```

This runs the `syncPaintings` task and archives `src/main/resources/resourcepack` into `build/distributions/pack.zip`. See the [Resource Pack Setup (Two Options)](#resource-pack-setup-two-options) section at the bottom for installation commands and server hosting steps.

---

## Configuration (`config.yml`)

The plugin creates a default `config.yml` on first launch:

```yaml
# Hardware WLED light synchronization
wled:
  enabled: true
  ip: "192.168.1.67"
  timeout-seconds: 2

# Automatic resource pack delivery to joining players
resource-pack:
  enabled: false
  url: "https://your-domain.com/pack.zip"
  hash: "" # Optional SHA-1 hex hash
  prompt: "Installer JarveCraft-pakken for custom shots, hatter og malerier!"
  force: false

# Drinking game settings
drinking-game:
  gapple-nausea-duration-ticks: 200
```

To reload configuration changes live in-game without restarting:

```
/wled reload
```

---

## Commands & Permissions


| Command           | Description                                    | Permission         |
| :---------------- | :--------------------------------------------- | :----------------- |
| `/wled overworld` | Manually switch WLED strip to Overworld theme  | `jarvecraft.admin` |
| `/wled nether`    | Manually switch WLED strip to Nether theme     | `jarvecraft.admin` |
| `/wled end`       | Manually switch WLED strip to The End theme    | `jarvecraft.admin` |
| `/wled death`     | Manually trigger the red death flash           | `jarvecraft.admin` |
| `/wled reload`    | Reloads`config.yml` and updates WLED target IP | `jarvecraft.admin` |

---

## How to Add Custom Paintings

1. Place image files (`.jpg`, `.jpeg`, `.png`) inside the `paintings/` directory.
   - *Optional:* Place files inside subfolders `paintings/square/`, `paintings/landscape/`, or `paintings/portrait/` to manually categorize their slot shape. If placed in the root `paintings/` folder, the aspect ratio is detected automatically!
2. Run `./gradlew syncPaintings` (or `./gradlew build`).
3. The build script scales them to a max dimension of 1024px, saves PNGs into `src/main/resources/resourcepack/assets/minecraft/textures/painting/`, and updates `custom_paintings.txt`.

---

## Project Structure

```
JarveCraft/
├── paintings/            # Raw input images for custom in-game paintings
├── src/main/kotlin/      # Plugin source code (Kotlin)
│   └── dev/webkom/jarveCraft/
│       ├── JarveCraft.kt           # Main plugin lifecycle & recipe registration
│       ├── Jager.kt                # Jägermeister potion logic & drinking effects
│       ├── ShotManager.kt          # Scoreboard shots tracker
│       ├── WledController.kt       # Async HTTP client for WLED integration
│       ├── WledCommand.kt          # /wled command executor & tab completion
│       ├── PaintingRandomizer.kt   # Painting place event handler
│       ├── BabyZombieCustomizer.kt # Baby zombie equipment listener
│       └── AchievementGiveaway.kt  # Advancement-based triggers
├── src/main/resources/
│   ├── config.yml        # Plugin configuration file
│   ├── plugin.yml        # Bukkit plugin description
│   └── resourcepack/     # Resource pack textures, models, sounds
├── build.gradle.kts      # Gradle Kotlin DSL build & image sync task
└── lights.sh             # Standalone bash utility to test WLED HTTP API
```

---

## Resource Pack Setup (Two Options)

After generating `build/distributions/pack.zip` with `./gradlew packageResourcePack`, choose one of the following two options to deploy it.

### Option 1: Copy Directly to Minecraft Client (Manual Install)

Use this option if you or players are loading the pack locally in the Minecraft client.

Tip: As the host, you can simply run `./gradlew packageResourcePack`, upload the generated `build/distributions/pack.zip` to a Slack or Discord channel, and tell players to download it and drop it into their Minecraft resourcepacks folder. They do not need to clone the repository, install Java, or run any build commands.

#### Terminal Commands to Copy `pack.zip`:

- **macOS**:

  ```bash
  cp path/to/pack.zip ~/Library/Application\ Support/minecraft/resourcepacks/
  ```
- **Linux**:

  ```bash
  cp path/to/pack.zip ~/.minecraft/resourcepacks/
  ```
- **Windows (PowerShell)**:

  ```powershell
  Copy-Item -Path "path\to\pack.zip" -Destination "$env:APPDATA\.minecraft\resourcepacks\"
  ```

#### Activate in Game:

1. Open Minecraft and navigate to **Options** -> **Resource Packs**.
2. Hover over **JarveCraft** (or `pack.zip`) in the **Available** list and click the arrow to move it to **Selected**.
3. Click **Done**.

---

### Option 2: Host for Automatic Download on Server Join

Use this option so all players joining the server are automatically prompted to download and activate the pack.

#### Step 1: Start a Local HTTP Server (For LAN / Office)

Run this terminal command from your project root:

```bash
python3 -m http.server 8080 --directory build/distributions
```

Find your local IP address:

- **macOS**:
  ```bash
  ipconfig getifaddr en0 || ipconfig getifaddr en1
  ```
- **Linux**:
  ```bash
  hostname -I | awk '{print $1}'
  ```
- **Windows (PowerShell)**:
  ```powershell
  (Get-NetIPAddress -AddressFamily IPv4 -InterfaceAlias "Wi-Fi*","Ethernet*").IPAddress[0]
  ```

Your resource pack URL will be:

```
http://<YOUR_LOCAL_IP>:8080/pack.zip
```

*(Alternatively, upload `pack.zip` to a web server, S3 bucket, or GitHub Releases).*

#### Step 2: Compute the SHA-1 Hash

Run the terminal command for your operating system:

- **macOS / Linux**:
  ```bash
  shasum build/distributions/pack.zip | awk '{print $1}'
  ```
- **Windows (PowerShell)**:
  ```powershell
  (Get-FileHash -Algorithm SHA1 build\distributions\pack.zip).Hash.ToLower()
  ```

#### Step 3: Apply the Configuration

Open `plugins/JarveCraft/config.yml` (or `src/main/resources/config.yml`) and insert the URL and hash:

```yaml
resource-pack:
  enabled: true
  url: "http://<YOUR_LOCAL_IP>:8080/pack.zip"
  hash: "replace_with_computed_sha1_hash"
  prompt: "Installer JarveCraft-pakken for custom shots, hatter og malerier!"
  force: false
```

Reload the plugin in-game:

```
/wled reload
```

*(Alternatively, set `resource-pack` and `resource-pack-sha1` in Paper's `server.properties`).*
