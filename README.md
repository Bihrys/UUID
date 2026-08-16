# UUID Swap

**English** | [简体中文](README_zh-CN.md)

[![Build all Minecraft versions](https://github.com/Bihrys/UUID/actions/workflows/build.yml/badge.svg)](https://github.com/Bihrys/UUID/actions/workflows/build.yml)

UUID Swap is a Fabric mod that replaces Minecraft player save data by UUID and provides an in-game GUI for transferring ownership of tamed pets.

## Compatibility

| Minecraft | Java | Module |
|---|---:|---|
| `1.21.11` | 21+ | `fabric-1.21.11` |
| `26.1` | 25+ | `fabric-26.1` |
| `26.1.1` | 25+ | `fabric-26.1.1` |
| `26.1.2` | 25+ | `fabric-26.1.2` |
| `26.2` | 25+ | `fabric-26.2` |

All builds use Fabric Loader `0.19.3` or newer and Mod ID `uuid`. Minecraft 26.x uses the new unobfuscated development setup.

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

GitHub Actions builds all supported versions on every push to `master`, for pull requests, on manual dispatch, and every Sunday at 00:00 UTC. Every run uploads the jars as a workflow artifact for 30 days. Scheduled and manually dispatched runs also refresh the rolling [`automated-build`](https://github.com/Bihrys/UUID/releases/tag/automated-build) prerelease.

## Project structure

```text
uuid-mod/
├─ common/             Shared player-data file operations and data models
├─ fabric-1.21.11/     Yarn-based Minecraft 1.21.11 implementation
├─ fabric-26.1/        Minecraft 26.1 implementation
├─ fabric-26.1.1/      Minecraft 26.1.1 implementation
├─ fabric-26.1.2/      Minecraft 26.1.2 implementation
├─ fabric-26.2/        Minecraft 26.2 implementation
└─ settings.gradle     Multi-module definition
```

Minecraft/Fabric API integration remains version-specific, while reusable file replacement, backup logic, and player information models live in `common`.

## Safety

Always test on a copy of the world and keep `backupBeforeOverwrite=true`. Save-data replacement is destructive if backups are disabled.

## License

All rights reserved. See [LICENSE.txt](LICENSE.txt).
