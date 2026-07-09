# UUID Swap

Fabric 1.21.11 模组。功能是按 UUID 复制/替换玩家存档数据，并提供宠物主人替换功能。

## 基本信息

- Minecraft: `1.21.11`
- Loader: Fabric Loader `0.19.3`
- Yarn: `1.21.11+build.6`
- Fabric API: `0.141.4+1.21.11`
- Java: `21`
- Mod ID: `uuid`

## 功能

### 1. 玩家数据替换

玩家或管理员可以选择一个已有玩家的数据来源，把另一个玩家的保存数据替换成该数据来源。

替换范围包括：

- `playerdata/<uuid>.dat`
  - 背包
  - 经验
  - 血量
  - 位置
  - 维度
  - 末影箱等保存在玩家 `.dat` 文件中的数据
- `stats/<uuid>.json`
  - 游戏时间
  - 统计信息
- `advancements/<uuid>.json`
  - 成就/进度

如果配置项 `transferPetsAutomatically=true`，模组还会把当前已加载区块中的已驯服宠物主人从目标玩家 UUID 改为源玩家 UUID。

> 注意：Minecraft 没有在运行时一次性枚举全世界所有未加载区块实体的安全公共 API。本模组自动转移的是当前服务器已加载世界中的 `TameableEntity`。未加载区块里的宠物需要加载区块后再用红石粉右键功能单独处理，或者让区块加载后再次执行替换。

### 2. 玩家选择 GUI

输入 `/uuidswap` 后会打开箱子 GUI：

- 每个玩家显示为玩家头颅。
- 头颅显示玩家皮肤头像。
- 名称显示玩家名。
- Lore 显示 UUID 和在线/离线状态。
- 支持分页。
- 点击头像即可选择该玩家作为数据来源。

这样可以满足“列出全部玩家 UUID 和名字，并点击选择”的需求，同时不要求客户端安装额外 UI 模组。

### 3. 宠物主人替换

操作方式：

1. 手持红石粉。
2. 潜行/蹲下。
3. 右键一个已驯服宠物。
4. 打开玩家选择 GUI。
5. 点击玩家头像，把宠物主人替换为该玩家。

适用对象：所有继承 `TameableEntity` 的驯服型实体，例如狼、猫、鹦鹉等。

## 指令

### 打开玩家选择界面

```mcfunction
/uuidswap
```

仅玩家可用。打开 GUI，点击目标玩家后，会把自己的数据替换为该目标玩家的数据。

### 列出已有玩家

```mcfunction
/uuidswap list
```

显示服务器/存档中已有玩家的名字、UUID、在线状态。

### 玩家把自己替换成指定 UUID 的数据

```mcfunction
/uuidswap <targetUuid>
```

示例：

```mcfunction
/uuidswap 00000000-0000-0000-0000-000000000000
```

### OP 或控制台指定替换

```mcfunction
/uuidswap apply <sourceUuid> <targetUuid>
```

含义：把 `sourceUuid` 对应玩家的数据替换成 `targetUuid` 对应玩家的数据。

示例：

```mcfunction
/uuidswap apply aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb
```

## 在线玩家替换逻辑

如果 `sourceUuid` 对应玩家在线，模组不会直接热替换。流程如下：

1. 记录待替换任务。
2. 踢出该玩家。
3. 等服务器完成断开连接后的最终保存。
4. 下一 tick 替换文件。
5. 玩家重新进入世界后加载新数据。

这样做是为了避免 Minecraft 在玩家离线保存时把刚替换的数据覆盖掉。

## 配置文件

第一次启动后生成：

```text
config/uuid-config.json
```

默认配置：

```json
{
  "playerSwapPermissionLevel": 4,
  "petOwnerSwapPermissionLevel": 4,
  "backupBeforeOverwrite": true,
  "transferPetsAutomatically": true,
  "enablePetOwnerSwap": true
}
```

### 权限等级

Minecraft 1.21.11 权限等级：

| 值 | 含义 |
|---:|---|
| 0 | 所有人 |
| 1 | moderators |
| 2 | gamemasters |
| 3 | admins |
| 4 | owners / 最高 OP |

建议保持默认 `4`。这个模组会直接覆盖玩家存档，权限不应下放给普通玩家。

## 备份

如果 `backupBeforeOverwrite=true`，覆盖前会自动备份源玩家原始数据。

备份目录：

```text
<world>/uuid_backups/<时间>_source-<sourceUuid>_from-<targetUuid>/
```

备份内容：

```text
playerdata/<sourceUuid>.dat
stats/<sourceUuid>.json
advancements/<sourceUuid>.json
README.txt
```

## 构建

在项目根目录执行：

```bash
./gradlew build
```

Windows：

```bat
gradlew.bat build
```

构建产物在：

```text
build/libs/
```

把生成的 jar 放入客户端或服务端的 `mods` 文件夹。

## 使用建议

1. 在正式世界使用前，先复制一份世界存档测试。
2. 保持 `backupBeforeOverwrite=true`。
3. 如果要替换在线玩家，执行后让玩家按提示重新进入世界。
4. 宠物自动转移只处理已加载实体；未加载区块中的宠物需加载后用红石粉右键功能处理。
