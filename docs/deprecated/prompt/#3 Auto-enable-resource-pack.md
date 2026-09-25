> #3 提示词

# 

```
读README，看下资源包能否改成默认启用，并且如果玩家手动取消选择了资源包，重启游戏后应该有原版机制保留这个选择的
```

# 

```
I'll read the README and explore the resource pack handling.

Now let me inspect the vanilla mechanism through IDEA MCP.

Let me verify how selection is saved and whether `shouldAddAutomatically` is used anywhere.

已确认两侧的原版机制。结论如下。

## 数据包侧：原版已经完整支持你要的两个行为

- **自动启用**：`MinecraftServer.configurePackRepository` 对每个可用包检查 `pack.getPackSource().shouldAddAutomatically()`（`MinecraftServer.java:1375`），`PackSource.DEFAULT` 返回 `true` → 新发现的数据包自动进 Enabled。`ExtraPackFinder` 用的正是 `PackSource.DEFAULT`，这条链是通的。
- **取消后保留**：`getSelectedPacks`（`MinecraftServer.java:1403`）把「可用但未选中」的全部写进 level.dat 的 `DataPacks.Disabled`；下次加载时 `getDisabled().contains(id)`（`MinecraftServer.java:1372`）直接跳过自动启用。这就是原版机制。

## 资源包侧：原版**没有**默认启用机制

- 整个 1.20.1 里只有一处读 `shouldAddAutomatically`，就是上面那个**服务端数据包**分支。客户端资源包完全不看 `PackSource`——换任何 `PackSource` 都不会自动勾选。所以 [ClientResourcePackFinder.java:24](core/src/main/java/dev/xcolorful/cgccompat/packloader/client/resource/ClientResourcePackFinder.java:24) 的注释是准确的。
- 客户端选中列表唯一来源是 options.txt 的 `resourcePacks`：`Minecraft` 构造里 `resourcePackRepository.reload()` → `options.loadSelectedResourcePacks(...)`（`Minecraft.java:460-461`），加载时还会顺手删掉「已不存在」的条目（`Options.java:1273`）。
- **好消息**：「玩家取消后重启仍是取消」这半边原版免费给你了——取消时 `Options.updateResourcePacks`（`Options.java:602`）会把新列表写回 options.txt，重启后就不会被选中。所以只要我们的包 id 进过一次那个列表，要求二自动成立。
- `required = true` 是原版唯一的强制启用标志（Forge 的 `mod_resources` 就这么干），但 `canUnselect()` = `isSelected() && !isRequired()`，且 `PackRepository.rebuildSelected` 会无条件补回，玩家将**永远无法取消**——和要求二直接冲突。

## 唯一缺的环节

「首次默认勾上」。这件事原版帮不了忙，因为它在 options.txt 里分不清**「从没出现过」**和**「玩家主动关掉了」**——两种情况都是「id 不在列表里」。要么自己记一笔状态，要么用 `required` 放弃可取消性。

时序上有个干净的位置：`PackRepository.reload()`（`Minecraft.java:460`）跑在我们 finder 的 `loadPacks` 里，而它早于 `loadSelectedResourcePacks`（461）和 `openAllSelected()`（559）——在那里改 `options.resourcePacks` 不需要额外触发一次资源重载。

需要你定个方向。

Direction noted — a bool in `cgccpackloader.json`. Let me check the remaining call sites and the selection-model behavior before editing.

Plan is clear. Let me track it and implement.
```

# 

```
如果是列表，则默认为true，保存时转为新格式，一个键值用目录，一个键值用bool
```

# 

User confirms the legacy-array → new-format migration, which is what I implemented. Continuing with the source wiring.

While that compiles, updating the docs.

Gradle in this shell can't resolve the MC transitive libs (gson/brigadier missing) — that's environmental, not my change. Let me build through IDEA instead with a longer timeout.

IDEA build passes. Now let me actually exercise the config read/write logic, including the legacy migration, with a throwaway harness.

All seven behaviors check out, including the legacy upgrade. Let me tidy one double-parse in `read()`.

按你的方案实现完了，改动集中在 3 个 Java 文件 + 3 个文档。

## 配置

[ExtraPackConfig.java](core/src/main/java/dev/xcolorful/cgccompat/packloader/core/resource/ExtraPackConfig.java) 从「整个文件是目录数组」改成对象：

```json
{
  "directories": [
    "./tacz/"
  ],
  "resourcePackAlwaysLoaded": true
}
```

- 读到旧数组格式时按默认值 `true` 补上该字段，并**就地改写成对象形式**，用户不用手动迁移
- 字段缺失或类型不对（比如写成 `"yes"`）→ 取默认值并记 error 日志，不做静默转换；JSON 损坏时仍保持原文件不动（沿用原有约定）

## 行为接线

[ExtraPackRepositorySource.java:76](core/src/main/java/dev/xcolorful/cgccompat/packloader/core/resource/ExtraPackRepositorySource.java:76) 的 `fromConfig` 推导 `required`：

```java
boolean required = packType == PackType.CLIENT_RESOURCES && settings.resourcePackAlwaysLoaded();
```

`true` 时资源包以 `required=true` 注册进 `Pack.readMetaAndCreate`，原版 `PackRepository.rebuildSelected` 会强制把它留在已启用列表里。**数据包刻意不受这个字段影响**——它们本来就走原版 `shouldAddAutomatically` 自动启用、玩家关闭后记进存档，改成 `required` 反而会让玩家无法关闭，那是回退。

## 两个你需要确认的点

1. **`true` 时资源包在界面上无法取消**（`canUnselect()` = `isSelected() && !isRequired()`），只能改配置关掉。这是「始终加载」的必然代价，和你说的「手动设 false 即视为知道这个问题」一致，但和最初那句「玩家取消后重启保留」在 `true` 下是互斥的——`false` 时才回到原版行为（options.txt 记录并保留）。如果你要的是「默认勾上但仍能在界面里取消」，这版就不是，需要换成一次性注入方案。
2. 配置的迁移写发生在**首次读取**时（不是退出保存时）。我按你说的「保存时转为新格式」理解为「让文件最终变成新格式」，所以选择在读到旧格式的那一刻就改写。如果你只想在别的地方触发写入，告诉我。

## 验证

- IDEA 构建通过，无 problems
- 用一个临时 harness 实际跑了 `ExtraPackConfig.read` 的 7 种输入：文件缺失、旧数组（确认读对且升级成对象）、`false`、缺字段、字段类型错误、JSON 损坏（确认文件未被覆盖）、相对/绝对路径解析——结果全部符合预期

未新增平台 API 使用（只是把已有的 `required` 参数从 `false` 改成变量），新引入的只有 Gson 的 `JsonElement`/`JsonObject`/`JsonNull`，与该类原本就在用的 Gson 是同一个依赖。没有动版本号和 changelog，等你决定要不要发版。**未提交**，等你 review。

# 

```
换成一次性注入方案，得在config true的时候能手动取消
```

# 

Switching to one-time injection. First I need to confirm how many times Forge fires the CLIENT_RESOURCES finder event, since that decides where the injection can safely live.

Confirmed: `CLIENT_RESOURCES` is posted exactly once (into Minecraft's own repository), so injecting from our source's `loadPacks` lands before vanilla reads options.txt. Implementing now.

Now the new client-side defaults class.

Build is clean. Now syncing the docs to the new semantics.

Now verifying the new code paths with a harness (the client-only record file via reflection, plus the renamed config key).

Let me verify `Options.save()` is safe to call this early in the Minecraft constructor, since I call it from inside `loadPacks`.

已改成一次性注入，`required` 那条路完全回退了。

## 现在的行为

- `resourcePackEnabledByDefault: true`（默认）：新发现的资源包被**默认勾选一次**。玩家仍可在资源包界面正常取消（包不再是 `required`，`canUnselect()` 为 true），取消结果由原版 `Options.updateResourcePacks` 写回 `options.txt`，重启不会被重新勾上。
- `false`：完全不插手，连记录都不写。

实现核心在新文件 [ClientPackDefaults.java](core/src/main/java/dev/xcolorful/cgccompat/packloader/client/resource/ClientPackDefaults.java)：代理 `ExtraPackRepositorySource`，在 `loadPacks` 枚举完之后把新见到的 id 写进 `options.resourcePacks` 并落盘。

**这个时机是故意的**：`loadPacks` 由 `PackRepository.reload()` 调用，而 `Minecraft` 构造里是 `reload()` → `Options.loadSelectedResourcePacks(...)` → `openAllSelected()`，所以补上的选项会被**同一次**资源加载采纳，不需要额外触发一次重载（否则要付一次全量贴图重载的启动开销）。我确认过 Forge 只在一处注册 `CLIENT_RESOURCES` finder（`ClientModLoader.begin`，用的是 `Minecraft` 自己的包仓库），所以这里改的选项就是客户端实际用的那份。

## 取消怎么被记住

`config/cgccpackloader-client.json` 记录见过的包 id：

```json
{ "defaultEnabled": [ "cgccpackloader/tacz/xxx" ] }
```

- 没记录且没勾上 → 记录 + 勾上
- 没记录但已勾上（玩家自己先勾的）→ 只记录不重复勾，这样他之后取消也作数
- 记录过 → 不插手

顺带两处一致性处理：先 `options.save()` 再写记录（记录写失败时，下次启动这些 id 已在 `options.txt` 里，会被当成"已见过"补记，不会重复勾）；`applied` 一次性标志保证 F3+T / 关闭资源包界面触发的重载不会把刚取消的包翻回来（代价是中途丢进目录的新包要下次启动才默认勾选）。

## 两个顺带决定

1. 配置键从 `resourcePackAlwaysLoaded` **改名**为 `resourcePackEnabledByDefault`——旧名字描述的是已经废弃的"始终加载"语义。
2. 客户端侧自己读配置（`ExtraPackRepositorySource` 只取 `directories`，加了一行 `.directories()`），避免同一个文件为拿两个字段读两遍；`ExtraPackFinder`（数据包侧）一行未动。

## 验证

- IDEA 构建通过，无 problems
- harness 实跑：配置缺文件/旧数组升级/`false` 三种读法，以及记录文件的空读、写入 `{"defaultEnabled":[...]}`、回读一致、损坏文件不抛异常——全部符合预期
- 另外确认了 `Options.save()` 可以在这个时间点调用：它读 `minecraft.getWindow()`（构造第 440 行已赋值），而 `broadcastOptions()` 被 Forge 的 `ClientModLoader.isLoading()` 挡住直接返回

**没验证到的**：真实游戏内的表现（取消后重启、界面里能取消）——这里没法启动客户端，只能靠上面的时序推演。这点请你实机确认一次。另外按老规矩没动版本号和 changelog，**未提交**。

# 

```
那原本的.json就还是保持列表，新的json包含bool和defaultEnabled
```

# 

收到 — 把配置拆开：`cgccpackloader.json` 回到纯列表，bool 和 `defaultEnabled` 都放进新的客户端文件。这样 `ExtraPackConfig` 和 `ExtraPackRepositorySource` 可以整个回退到原样。

已按这个拆法重做，改动比上一版小很多。

## 两个文件

`./config/cgccpackloader.json` —— **回到纯列表**，schema 没有变化，`ExtraPackConfig` 和 `ExtraPackRepositorySource` 整个文件回退到原样（`git status` 里已经没有它们了）。

`./config/cgccpackloader-client.json` —— 新文件，只在物理客户端生成：

```json
{
  "resourcePackEnabledByDefault": true,
  "defaultEnabled": []
}
```

- `resourcePackEnabledByDefault`：给用户改的开关，`true`（默认）时默认勾选一次
- `defaultEnabled`：模组自己维护，记录已默认勾选过的包 id，用来区分「还没默认勾过」和「玩家已经取消」

## 改动清单

- 新增 [ClientPackDefaults.java](core/src/main/java/dev/xcolorful/cgccompat/packloader/client/resource/ClientPackDefaults.java)：读客户端文件、代理 finder、在 `loadPacks` 末尾把新包注入 `options.resourcePacks` 并落盘。开关为 `false` 时直接把原 finder 返回，什么都不做
- [ClientResourcePackFinder.java:36](core/src/main/java/dev/xcolorful/cgccompat/packloader/client/resource/ClientResourcePackFinder.java:36)：改成 `ClientPackDefaults.decorate(ExtraPackRepositorySource.fromConfig(...))`，两行
- [CgccPackLoader.java](core/src/main/java/dev/xcolorful/cgccompat/packloader/CgccPackLoader.java)：加 `CLIENT_FILE_NAME` / `clientFile()`，与 `configFile()` 并列
- README + 两个架构文档

`ExtraPackConfig`、`ExtraPackRepositorySource`、`ExtraPackFinder` 一律未改。客户端文件缺失时会按默认值写出来，这样用户才知道有这个开关。

## 验证

- IDEA 构建通过，无 problems
- harness 实跑客户端文件 8 种情况：缺失（自动创建两个键）、开关 `false`、开关类型写成 `"no"`（回默认 `true` 并报错）、读记录、写回时两个键都保留、缺 `defaultEnabled`、JSON 损坏、根节点不是对象——全部符合预期

**仍未验证**：游戏内实际效果（默认勾上、界面能取消、取消后重启保持），这里没法启动客户端。另外没动版本号和 changelog，**未提交**。

# 

```
README第二条简化一下，只说功能即可
```

# 

README 第二条（中英）已简化为只描述功能：

- 中：`新发现的资源包会被默认勾选，玩家也能像原版资源包那样正常取消`
- 英：`Newly found resource packs are enabled by default, and can be turned off like any vanilla resource pack`

具体行为（只勾一次、取消后不会被重新勾上、开关怎么改）留在下面「配置」那节，主要特色里不再展开。只改了 README，代码未动，仍未提交。
