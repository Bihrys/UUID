# UUID Swap

**English** | [简体中文](README_zh-CN.md)

[![Build all Minecraft versions](https://github.com/Bihrys/UUID/actions/workflows/build.yml/badge.svg)](https://github.com/Bihrys/UUID/actions/workflows/build.yml)

UUID Swap replaces Minecraft player save data by UUID. Fabric and Forge modules share the common file-transfer logic.

## Compatibility

| Minecraft | Java | Module |
| --- | ---: | --- |
| `1.20` | 17+ | `fabric-1.20` |
| `1.20.1` | 17+ | `forge-1.20.1` (Forge 47.4.23) |
| `1.20.2` | 17+ | `fabric-1.20.2` |
| `1.20.3` | 17+ | `fabric-1.20.3` |
| `1.20.4` | 17+ | `fabric-1.20.4` |
| `1.20.5` | 21+ | `fabric-1.20.5` |
| `1.20.6` | 21+ | `fabric-1.20.6` |
| `1.21` | 21+ | `fabric-1.21` |
| `1.21.1` | 21+ | `fabric-1.21.1` |
| `1.21.2` | 21+ | `fabric-1.21.2` |
| `1.21.3` | 21+ | `fabric-1.21.3` |
| `1.21.4` | 21+ | `fabric-1.21.4` |
| `1.21.5` | 21+ | `fabric-1.21.5` |
| `1.21.6` | 21+ | `fabric-1.21.6` |
| `1.21.7` | 21+ | `fabric-1.21.7` |
| `1.21.8` | 21+ | `fabric-1.21.8` |
| `1.21.9` | 21+ | `fabric-1.21.9` |
| `1.21.10` | 21+ | `fabric-1.21.10` |
| `1.21.11` | 21+ | `fabric-1.21.11` |
| `26.1` | 25+ | `fabric-26.1` |
| `26.1.1` | 25+ | `fabric-26.1.1` |
| `26.1.2` | 25+ | `fabric-26.1.2` |
| `26.2` | 25+ | `fabric-26.2` |
| `26.3` | 25+ | `fabric-26.3` |

All builds use Mod ID `uuid`. Fabric modules for Minecraft 1.20–1.20.4 use Fabric Loader `0.16.14` or newer (Java 17); newer Minecraft versions use Fabric Loader `0.19.3` or newer. Minecraft 26.x uses the new unobfuscated development setup.

## Features

### Player data replacement

The mod can copy a donor player's saved data to a target UUID. The following files are handled:

- `playerdata/<uuid>.dat` — inventory, experience, health, position, dimension, ender chest, and other player data
- `stats/<uuid>.json` — statistics
- `advancements/<uuid>.json` — advancements

When `transferPetsAutomatically=true`, loaded tamed animals owned by the donor are reassigned to the target UUID.

> Only entities in currently loaded worlds can be transferred automatically. Pets in unloaded chunks must be handled after their chunks are loaded.

### Player selection GUI

Running `/uuidswap` opens a paginated chest GUI. Each player is shown as a player head with their name, UUID, skin, and online/offline status.

### Pet owner replacement

Hold redstone dust, sneak, and right-click a tamed animal. Select the new owner from the player-head GUI.

## Commands

```text
/uuidswap
```

Open the GUI and replace your own data with the selected player's data.

```text
/uuidswap list
```

List known players, UUIDs, and online status.

```text
/uuidswap <targetUuid>
```

Replace your own data with the data stored under `targetUuid`.

```text
/uuidswap apply <sourceUuid> <targetUuid>
```

Replace the data stored under `sourceUuid` with the data stored under `targetUuid`. This form is intended for operators and the server console.

## Online replacement behavior

If the source player is online, the mod disconnects them first, waits for the final save, and applies the replacement on the next server tick. The player can then reconnect and load the new data.

## Configuration

The file `config/uuid-config.json` is created on first launch:

```json
{
  "playerSwapPermissionLevel": 4,
  "petOwnerSwapPermissionLevel": 4,
  "backupBeforeOverwrite": true,
  "transferPetsAutomatically": true,
  "enablePetOwnerSwap": true
}
```

Permission levels are `0` (everyone), `1` (moderators), `2` (gamemasters), `3` (admins), and `4` (owners/full OP). Keep the default level `4` unless you fully understand the risk of allowing save-file replacement.

## Backups

With `backupBeforeOverwrite=true`, the source player's original files are backed up before replacement:

```text
<world>/uuid_backups/<timestamp>_source-<sourceUuid>_from-<targetUuid>/
```

## Build

Build every supported version with Java 25:

```bash
./gradlew buildAll
```

Forge 1.20.1 is an independent ForgeGradle project and uses Java 17: `cd forge-1.20.1 && ./gradlew build`.

Windows:

```bat
gradlew.bat buildAll
```

Build only one target, for example:

```bash
./gradlew :fabric-26.1.2:build
```

Each jar is generated under its module's `build/libs/` directory. Do not use a jar with a different Minecraft version.

### Automated GitHub builds

GitHub Actions builds Fabric and Forge on pushes, pull requests, manual runs, and every Sunday. Download the rolling prerelease from [automated-build](https://github.com/Bihrys/UUID/releases/tag/automated-build).

GitHub Actions builds all supported versions on every push to `master`, for pull requests, on manual dispatch, and every Sunday at 00:00 UTC. Every run uploads the jars as a workflow artifact for 30 days. Pushes to `master`, scheduled builds, and manually dispatched runs also refresh the rolling [`automated-build`](https://github.com/Bihrys/UUID/releases/tag/automated-build) prerelease.

## Project structure

```text
uuid-mod/
├─ common/             Shared player-data file operations and data models
├─ fabric-1.20*/       Yarn-based implementations for Minecraft 1.20–1.20.4 (Java 17, NBT items)
├─ fabric-1.20.5*/     Yarn-based implementations for Minecraft 1.20.5–1.21.11 (Java 21, item components)
├─ fabric-26.*/        Unobfuscated implementations for Minecraft 26.1–26.3 (Java 25)
├─ forge-1.20.1/       Independent ForgeGradle project for Minecraft 1.20.1
└─ settings.gradle     Multi-module definition
```

Minecraft/Fabric API integration remains version-specific, while reusable file replacement, backup logic, and player information models live in `common`.

## Safety

Always test on a copy of the world and keep `backupBeforeOverwrite=true`. Save-data replacement is destructive if backups are disabled.

## License

MIT License. See [LICENSE.txt](LICENSE.txt).

You are free to view, modify, and redistribute this project (including modified versions), as long as the copyright notice above is preserved and **Bihrys** is credited as the original author.
