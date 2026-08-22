# UUID Swap

[English](README.md) | **简体中文**

[![构建所有 Minecraft 版本](https://github.com/Bihrys/UUID/actions/workflows/build.yml/badge.svg)](https://github.com/Bihrys/UUID/actions/workflows/build.yml)

UUID Swap 可以按照 UUID 替换 Minecraft 玩家存档数据。Fabric 与 Forge 模块共享跨版本的文件交换逻辑。

## 兼容性

| Minecraft | Java | 模块 |
|---|---:|---|
| `1.21.11` | 21+ | `fabric-1.21.11` |
| `26.1` | 25+ | `fabric-26.1` |
| `26.1.1` | 25+ | `fabric-26.1.1` |
| `26.1.2` | 25+ | `fabric-26.1.2` |
| `26.2` | 25+ | `fabric-26.2` |
| `1.20.1` | 17+ | `forge-1.20.1`（Forge 47.4.23） |

Fabric 版本使用 Fabric Loader `0.19.3` 或更高版本；Forge 版本使用 Forge 47.4.23。Mod ID 均为 `uuid`。

## 功能

### 玩家数据替换

模组可以把数据来源玩家的存档复制到目标 UUID。处理的文件包括：

- `playerdata/<uuid>.dat`：背包、经验、生命值、位置、维度、末影箱以及其他玩家数据
- `stats/<uuid>.json`：统计数据
- `advancements/<uuid>.json`：进度/成就

当 `transferPetsAutomatically=true` 时，当前已加载世界中属于数据来源玩家的驯服动物会自动改为目标 UUID 的主人。

> 自动转移只处理当前已加载世界中的实体。未加载区块里的宠物需要在区块加载后再处理。

### 玩家选择 GUI

执行 `/uuidswap` 会打开支持分页的箱子 GUI。每个玩家会显示为玩家头颅，并显示名称、UUID、皮肤以及在线/离线状态。

### 宠物主人替换

手持红石粉，潜行并右键已驯服的动物，然后在玩家头颅 GUI 中选择新的主人。

## 指令

```text
/uuidswap
```

打开 GUI，把自己的数据替换为所选玩家的数据。

```text
/uuidswap list
```

列出已知玩家、UUID 和在线状态。

```text
/uuidswap <targetUuid>
```

把自己的数据替换为 `targetUuid` 对应的数据。

```text
/uuidswap apply <sourceUuid> <targetUuid>
```

把 `sourceUuid` 对应的数据替换为 `targetUuid` 对应的数据，适用于 OP 或服务器控制台。

## 在线玩家替换逻辑

如果数据来源玩家在线，模组会先断开该玩家，等待 Minecraft 完成最终保存，然后在下一 tick 执行替换。玩家重新进入服务器后即可加载新数据。

## 配置文件

首次启动后会生成 `config/uuid-config.json`：

```json
{
  "playerSwapPermissionLevel": 4,
  "petOwnerSwapPermissionLevel": 4,
  "backupBeforeOverwrite": true,
  "transferPetsAutomatically": true,
  "enablePetOwnerSwap": true
}
```

权限等级为：`0`（所有人）、`1`（moderators）、`2`（gamemasters）、`3`（admins）、`4`（owners/最高 OP）。除非你完全了解风险，否则建议保持默认的 `4`。

## 备份

当 `backupBeforeOverwrite=true` 时，替换前会备份数据来源玩家的原始文件：

```text
<world>/uuid_backups/<时间>_source-<sourceUuid>_from-<targetUuid>/
```

## 构建

使用 Java 25 一次构建全部支持版本：

```bash
./gradlew buildAll
```

Windows：

```bat
gradlew.bat buildAll
```

也可以只构建一个版本，例如：

```bash
./gradlew :fabric-26.1.2:build
```

每个 Jar 会生成在对应模块的 `build/libs/` 中。不要把某个 Minecraft 版本的 Jar 用在其他版本上。

### GitHub 自动构建

GitHub Actions 会在每次推送到 `master`、提交 Pull Request、手动触发以及每周日 UTC 00:00（北京时间 08:00）时构建全部支持版本。每次运行都会把 Jar 作为 Actions Artifact 保留 30 天；推送到 `master`、定时构建和手动运行还会更新滚动的 [`automated-build`](https://github.com/Bihrys/UUID/releases/tag/automated-build) 预发布页面。

## 项目结构

```text
uuid-mod/
├─ common/             公共玩家数据文件操作和数据模型
├─ fabric-1.21.11/     使用 Yarn 的 Minecraft 1.21.11 实现
├─ fabric-26.1/        Minecraft 26.1 实现
├─ fabric-26.1.1/      Minecraft 26.1.1 实现
├─ fabric-26.1.2/      Minecraft 26.1.2 实现
├─ fabric-26.2/        Minecraft 26.2 实现
└─ settings.gradle     多模块定义
```

Minecraft/Fabric API 接入代码保留在各版本模块中，可复用的文件替换、备份逻辑和玩家信息模型位于 `common`。

## 安全建议

正式世界使用前请先复制世界进行测试，并保持 `backupBeforeOverwrite=true`。关闭备份时，存档替换可能造成不可逆的数据覆盖。

## 许可证

保留所有权利。详见 [LICENSE.txt](LICENSE.txt)。
