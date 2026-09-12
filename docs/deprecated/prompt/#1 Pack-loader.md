> #1 提示词

# 

```
现在需要完成MC模组，以下是说明。

模组介绍：
- 跨版本模组，当前Gradle结构分离了core和forge-compat，以便于提取平台无关代码及跨平台适配
- core目录下禁止包含forge/neoforge import，需要平台侧调用或在模组初始化时提供接口（例如需要@SubscribeEvent，则可以改成在平台侧监听，调用core实现）

模组需要实现的功能见README.md，即添加数据包/资源包读取目录。要求如下：
- 读取./config/cgccpackloader.json，格式为Json列表，每一项为字符串，表示路径，如果没有这个文件，默认添加./tacz/，并写入json
- 游戏启动后，将列出的路径都作为额外补充的数据包+资源包目录
- 需要查找或参考原版API来实现，如果原版有出处，则需要补充javadoc @see来方便移植到其他版本时知晓如何移植
- 如果不能直接用原版API实现，需要Mixin或者AccessTransformers的话，说明原因并向我报告，我来补充Mixin和AT环境
- 目前认为该模组功能极其简单，可能不需要特别多代码，因此目前文档里只有./resource/目录；如果需要其他辅助且不应属于resource包，则可以新建包
- 模组主类启动方式已经固定（CgccPackLoaderForge、CgccPackLoaderForgeClient、CgccPackLoader、CgccPackLoaderClient），注意服务端需要隔离资源包加载，也就是资源包加载要放在client.resource下实现
- 对于README里提到的缺少pack.mcmeta，实现方式为使用文件夹名称/资源包压缩包名称作为"description"，pack_format固定填15即可；不需要补充缺少的其他文件（如logo.png）；不需要将补充的pack.mcmeta写到文件夹/压缩包里
- 由于你可能不会写平台抽象层，如果实现的时候不知道怎么隔离平台代码的，可以写在core里，我手动实现平台抽象

现在开始执行，完成后通知我检查
```

# 

I'll start by exploring the project structure and documentation to understand what's already in place.

Let me explore the project structure and documentation.

Let me read the documentation and existing source files.

Now let me inspect the Minecraft 1.20.1 sources to find the vanilla APIs. Let me locate the sources jar.

Found the mapped sources jar. Let me extract the relevant vanilla pack classes to inspect exact 1.20.1 signatures.

The `/tmp` path maps differently in Git Bash. Let me get the real path.

Now let me check how Forge's `AddPackFindersEvent` hooks into the client, and inspect the metadata classes.

The client-side hook must be in `ClientModLoader`. Let me check it.

Let me check Forge's own resource pack classes for established patterns, and find all callers of `getMetadataSection`.

`DelegatingPackResources` is exactly the pattern I need. Let me read it.

Let me verify how packs from a finder get auto-enabled, since the packs must actually load without manual selection.

Let me check how Forge handles auto-selecting resource packs and the `required` pack path.

I have the full picture of the vanilla/Forge APIs. One behavioral decision genuinely belongs to you before I write the code.

Decisions noted: `required=false` (discoverable only) and no directory creation. Let me verify pack_format 15 is compatible in 1.20.1.

Confirmed pack_format 15 is correct for both types in 1.20.1. Let me verify the Forge FML classes I'll use exist.

Let me verify two remaining API details before writing code.

Let me verify Gson is on the core module's compile classpath.

I have everything I need. Let me set up tasks and write the code.

Now let me compile to verify.

Compiles successfully. There are two deprecation warnings on `FMLJavaModLoadingContext.get()` — let me check the recommended replacement.

Compilation passes for both modules. Let me check whether a runtime verification is feasible.

No runtime environment exists (no `run/` dir, no asset cache), so I'll verify the config file logic standalone against the real class.

All 13 config checks pass. Let me verify the one remaining assumption in the metadata fallback path.

`"pack"` and `"features"` confirmed — the fallback path is correct. Let me now exercise the actual pack discovery end-to-end using the real compile classpath.

Let me check the shape of the mapped jar so I can build a test classpath directly.

The `-I` failure is an environment quirk (default JDK is Java 25; Gradle 8.8 can't compile init scripts under it). The build itself is healthy — verified again without `-I`. Let me add the missing `@see` porting tags the spec asks for.

已完成，代码编译通过。以下是检查清单。

## 新增文件（全部在 core，无 forge/neoforge import）

|文件|职责|
|---|---|
|[ExtraPackConfig.java](app://localhost/epitaxy/core/src/main/java/dev/xcolorful/cgccompat/packloader/core/resource/ExtraPackConfig.java)|读写 `cgccpackloader.json`；缺文件时写入默认 `["./tacz/"]`；相对路径按 gameDir 解析|
|[ExtraPackRepositorySource.java](app://localhost/epitaxy/core/src/main/java/dev/xcolorful/cgccompat/packloader/core/resource/ExtraPackRepositorySource.java)|`RepositorySource` 实现，扫描每个配置目录的直接子项（文件夹 / `.zip`）注册为包|
|[FallbackMetadataPackResources.java](app://localhost/epitaxy/core/src/main/java/dev/xcolorful/cgccompat/packloader/core/resource/FallbackMetadataPackResources.java)|`PackResources` 包装器，原包缺 `pack.mcmeta` 时补合成 metadata|
|[ClientResourcePackFinder.java](app://localhost/epitaxy/core/src/main/java/dev/xcolorful/cgccompat/packloader/client/resource/ClientResourcePackFinder.java)|资源包 finder，唯一引用 `PackType.CLIENT_RESOURCES` 的地方|

## 修改文件

- [CgccPackLoader.java](app://localhost/epitaxy/core/src/main/java/dev/xcolorful/cgccompat/packloader/CgccPackLoader.java) — `init()` 改为 `init(Path gameDir, Path configFile)` 并暴露 `packDirectories()`
- [CgccPackLoaderForge.java](app://localhost/epitaxy/forge-compat/src/main/java/dev/xcolorful/cgccompat/packloader/forge/CgccPackLoaderForge.java) — 注册 `SERVER_DATA` 的 `AddPackFindersEvent`
- [CgccPackLoaderForgeClient.java](app://localhost/epitaxy/forge-compat/src/main/java/dev/xcolorful/cgccompat/packloader/forgeclient/CgccPackLoaderForgeClient.java) — 注册 `CLIENT_RESOURCES` 的 `AddPackFindersEvent`
- `CgccPackLoaderClient` 未动（客户端包路径已由 forgeclient 侧隔离）

## 为什么不需要 Mixin / AT

原版有**两处**独立的硬要求，都在公开扩展点上解决：

1. `FolderRepositorySource.detectPackResources` 对目录强制要求 `<dir>/pack.mcmeta`，否则目录**根本不被识别** → 所以不用它，自己写扫描逻辑。
2. `Pack.readPackInfo` 读到 `PackMetadataSection == null` 就丢弃整包 → 所以用 `PackResources` 包装器，仅在 delegate 返回 null 时补一个 section。

`RepositorySource`、`Pack.ResourcesSupplier`、`PackResources`、`AddPackFindersEvent` 都是公开接口/事件，无一处需要 Mixin 或 AT，**不需要你补环境**。

挂载点：Forge 的 `AddPackFindersEvent`（`SERVER_DATA` 由 `ServerPacksSource.createPackRepository` 抛出，`CLIENT_RESOURCES` 由 `ClientModLoader.begin` 抛出）。已按你的要求补了 `@see` 指向 `FolderRepositorySource`、`Pack#readPackInfo`、`ServerPacksSource#createPackRepository`、`ClientModLoader`。

## 需要你决定 / 注意

1. **`CgccPackLoader.init` 签名变更**：core 不能再 import `FMLPaths`，必须平台侧传入 gameDir + config 路径。如果你希望保持无参 + 手工抽象层，我可以改。
2. **2 个 deprecation warning**：`FMLJavaModLoadingContext.get()` 在 47.4.16 已 `forRemoval`，官方建议构造器注入。我**没改**，因为你声明 `forge_version_range=[47,)`，构造器注入需要较新的 47.x，改了会隐性缩小支持范围——要不要改由你定。
3. `PACK_FORMAT = 15` 硬编码在 `FallbackMetadataPackResources`（javadoc 已注明移植时要改）。
4. 配置只在模组构造时读一次，改配置需重启。
5. 包 id 形如 `cgccpackloader/tacz/<名称>`，避开原版 `file/` 与 Forge `mod/`；多个目录下同名包也靠目录名区分。
6. 按你的选择：`required=false`（仅可发现，需玩家启用；数据包因 `PackSource.DEFAULT` 仍自动启用但可在世界设置禁用）；目录不存在时跳过、**不创建**。
7. `description` 用子项文件名（含 `.zip`），与原版 `nameFromPath` 一致。

## 平台 API 使用报告（按 CLAUDE.md 要求）

- **forge-compat 侧**（平台侧，应属正常）：`AddPackFindersEvent`、`FMLJavaModLoadingContext`、`FMLPaths`、`IEventBus`、`FMLLoader.getDist()`（原有）。这些我**没有**找到项目内的兼容封装，所以直接使用。
- **core 侧**：只用原版 + JDK（`PackResources`/`AbstractPackResources`/`PathPackResources`/`FilePackResources`/`Pack`/`RepositorySource`/`PackSource`/`PackType`/`PackMetadataSection`/`Component`/`IoSupplier` + Gson）。

## 验证情况

- `./gradlew :core:compileJava :forge-compat:compileJava` **通过**。
- `ExtraPackConfig` 对真实类跑了 13 项检查**全过**：缺文件时创建 `["./tacz/"]`、相对/绝对路径解析、空白与 `null` 项跳过、JSON 语法错误 → 返回空且**不覆盖**用户文件、`[]` 返回空。
- **未做游戏内验证**：仓库里没有 `forge-compat/run/`，也没有 MC 资源缓存，无法启动客户端/服务端。所以「包是否真的出现在选择界面」需要你在 IDEA 里实测——这是我这次唯一没能闭环的一点。
- 未向仓库添加任何测试文件；临时校验代码在系统临时目录，与仓库无关。

（另：`docs/architecture/*/Home.md` 的改动是你本次会话开始前就存在的，我没有触碰。）

# 

```
- javadoc写中文
- Forge事件监听改成在平台层的resource包下，添加带@SubscribeEvent和EventSubscriber的类来监听，并在平台曾构建pack类型过滤，调用core侧的onAddPackFinders，从而将平台侧的逻辑变为仅提供事件监听，而实现放在core
- packDirectories改成多个private static final变量，提供public static final getter
- PACK_ID_PREFIX改成CgccPackLoader.MOD_ID + ""
- 修改后需要在两个Home.md列出每个类的功能说明，简短一句话就行
```

# 

Let me check the current docs and how Forge's `@Mod.EventBusSubscriber` handles dist filtering.

The `@Mod.EventBusSubscriber` dist filter in `AutomaticEventSubscriber` confirms `value = Dist.CLIENT` classes are never classloaded on a dedicated server — that's a cleaner isolation mechanism than the current manual holder.

One instruction I can't resolve without guessing, since `private static final` can't hold a runtime-loaded value:

Both answers are clear. Let me implement: platform listeners with annotation-driven registration, and `CgccPackLoader` holding each platform-supplied `Path` in its own field.

Let me compile before updating the docs.

Build is clean now — the two deprecation warnings are gone since the annotation replaces the manual bus lookup. Now the docs.

Let me verify the `@Mod.EventBusSubscriber` annotations are actually retained in the compiled classes — otherwise the listeners would silently never fire.

All annotations retained with the intended values. Let me get the final change summary.

五点修改全部完成，编译通过且无警告。

## 1. javadoc 改中文

所有新增/修改文件的 javadoc 与注释已改写为中文，`@see` 移植提示保留（指向 `FolderRepositorySource`、`Pack#readPackInfo`、`AbstractPackResources#getMetadataSection`、`ServerPacksSource#createPackRepository`、`ClientModLoader`）。

## 2. 事件监听移到平台层 resource 包

平台侧现在只剩「订阅事件 + 过滤 pack 类型 + 转发」，实现全在 core：

|平台侧|core 侧|
|---|---|
|[ForgeDataPackListener.java](app://localhost/epitaxy/forge-compat/src/main/java/dev/xcolorful/cgccompat/packloader/forge/resource/ForgeDataPackListener.java) `@Mod.EventBusSubscriber(bus = MOD)`|[ExtraPackFinder.java](app://localhost/epitaxy/core/src/main/java/dev/xcolorful/cgccompat/packloader/core/resource/ExtraPackFinder.java) `onAddPackFinders(Consumer<RepositorySource>)`|
|[ForgeClientResourcePackListener.java](app://localhost/epitaxy/forge-compat/src/main/java/dev/xcolorful/cgccompat/packloader/forgeclient/resource/ForgeClientResourcePackListener.java) `@Mod.EventBusSubscriber(bus = MOD, value = Dist.CLIENT)`|[ClientResourcePackFinder.java](app://localhost/epitaxy/core/src/main/java/dev/xcolorful/cgccompat/packloader/client/resource/ClientResourcePackFinder.java) `onAddPackFinders(Consumer<RepositorySource>)`|

两个 `CgccPackLoaderForge*` 里的手动 `addListener` 已删除。**副作用：之前那 2 个 `FMLJavaModLoadingContext.get()` deprecation warning 消失了**，现在编译零警告。

隔离机制也升级了：`AutomaticEventSubscriber` 里 `sides.contains(FMLEnvironment.dist)` 在 `Class.forName` **之前**判断，所以 `value = Dist.CLIENT` 的类在专用服务端根本不会被 classload。已用 `javap -v` 确认编译产物里的注解值正确（`bus=MOD`；客户端那个多一个 `value=[Dist.CLIENT]`）。

## 3. CgccPackLoader 改为逐 Path 字段

`packDirectories()` 已删除，改为平台侧传入的多个 `Path` 各自一个字段 + getter：

```java
private static Path gameDirectory;
private static Path configFile;
public static Path gameDirectory() { ... }
public static Path configFile() { ... }
```

配置读取下沉到 core 的 `ExtraPackRepositorySource.fromConfig(PackType, PackSource)`（由两个 finder 调用），因此不再有 `List<Path>` 静态状态。

## 4. PACK_ID_PREFIX

```java
private static final String PACK_ID_PREFIX = CgccPackLoader.MOD_ID + "";
```

id 形如 `cgccpackloader/tacz/<名称>`。

## 5. 两个 Home.md

[core/Home.md](app://localhost/epitaxy/docs/architecture/core/Home.md) 与 [client/Home.md](app://localhost/epitaxy/docs/architecture/client/Home.md) 均按「主类 / 资源 / 平台侧」分节列出每个类，中英双语各一句话。

## 验证

- `./gradlew :core:compileJava :forge-compat:compileJava` **通过、零警告**，11 个类全部产出。
- `ExtraPackConfig` 重写后重跑校验：13 项**全过**（缺文件建默认、相对/绝对解析、空白与 `null` 跳过、语法错误不覆盖用户文件、`[]` 为空）。
- `javap -v` 确认 `@SubscribeEvent` 与 `@Mod.EventBusSubscriber(modid/bus/value)` 均保留在 class 文件中，且回调目标是 `AddPackFindersEvent#addRepositorySource`。
- **仍未做游戏内验证**（仓库无 `run/`、无 MC 资源缓存），需要你在 IDEA 实测包的加载与选择界面显示。

## 两点需你确认

1. 我把平台侧的 `forge.resource` / `forgeclient.resource` 类**并入了两个 Home.md 的「平台侧」小节**，因为仓库里没有 forge 模块的 Home.md。若你想独立建档，我可以拆出去。
2. `CgccPackLoader` 位于 `dev.xcolorful.cgccompat.packloader`（不是 `.core`），我按「core 模块主类」处理，在两个 Home.md 里以引用行说明。若你希望严格按包路径归属，我改。

# 

```
将所有CgccPackLoader#LOGGER调用的日志改成英文log
```

# 

Let me read the current state of all files to find every `CgccPackLoader.LOGGER` call.

8 log calls to translate. Let me read the two files first.

8 处 `CgccPackLoader.LOGGER` 调用的消息已全部改为英文，编译通过且无警告。

|文件|原（中文）|现（英文）|
|---|---|---|
|ExtraPackConfig:61|忽略 {} 中非法的包目录 '{}'|`Ignoring invalid pack directory '{}' in {}`|
|ExtraPackConfig:78|读取 {} 失败，不会加载任何额外包目录|`Failed to read {}; no extra pack directories will be loaded`|
|ExtraPackConfig:91|已创建 {}，默认包目录为 {}|`Created {} with default pack directories {}`|
|ExtraPackConfig:93|创建 {} 失败|`Failed to create {}`|
|ExtraPackRepositorySource:75|额外包目录 {} 不存在，已跳过|`Extra pack directory {} does not exist, skipping`|
|ExtraPackRepositorySource:87|发现非包条目 '{}'，已忽略|`Found non-pack entry '{}', ignoring`|
|ExtraPackRepositorySource:96|列出 {} 中的包失败|`Failed to list packs in {}`|
|ExtraPackRepositorySource:112|读取 '{}' 的属性失败，已忽略|`Failed to read properties of '{}', ignoring`|

两点说明：

1. 第 1 条的**参数顺序**随语序调整了（原文 `configFile, entry` → 现 `entry, configFile`），否则英文句子里的位置会对不上；其余 7 条参数与占位符数量不变。后 3 条（`Found non-pack entry`、`Failed to read properties of`、`Failed to list packs in`）刻意沿用了原版 `FolderRepositorySource` 的同款措辞，方便和原版日志一起 grep。
2. javadoc 仍是中文，未动；只改了日志消息。

另外提一句：你把 `PACK_FORMAT` / `FILE_NAME` 上提到了 `CgccPackLoader`，我上次写的 Home.md 里那条 `CgccPackLoader` 说明只提到「保存游戏目录与配置文件路径」，没提这两个常量——功能描述没写错，只是不全。要不要我补一句？

# 

```
你检查现在文档里是否有表述需要更新的，不得把我删掉的板块加回去
```

# 

Let me read the current state of all docs (they may have changed on disk) and compare against the current code.

Checked all docs against the current code. One entry is factually wrong; let me fix that wording only.

检查完所有文档（`docs/README.md`、`docs/architecture/Home.md`、`core/Home.md`、`client/Home.md`、`docs/deprecated/README.md`、根 `README.md`）与当前代码的对应关系。

## 已改：1 处表述确实错了

[core/Home.md](app://localhost/epitaxy/docs/architecture/core/Home.md) 的 `FallbackMetadataPackResources` 条目：

- `以目录名为描述` → `以文件夹名/压缩包名为描述`（英文半同步改为 `described by the folder or archive name`）

理由：`ExtraPackRepositorySource:136` 是 `Component.literal(entry.getFileName())`，描述取的是**子项名**——子项可能是 `.zip`，那时它是压缩包名而不是目录名。而 zip 恰恰是 TaCZ 包的主要形态，所以这不是抠字眼。这也和根 README 自己的「文件夹名称/资源包压缩包名称」措辞一致。

## 其余检查结果（未改，均与代码一致）

|位置|结论|
|---|---|
|core/Home.md 其余 3 条|一致|
|client/Home.md 唯一一条|一致|
|docs/architecture/Home.md|仅导航，一致|
|docs/README.md、docs/deprecated/README.md|与代码无关|

有一处边界情况说明一下：core/Home.md 里 `ExtraPackConfig` 那条提到 `cgccpackloader.json`，现在这个名字已由 `CgccPackLoader.FILE_NAME` 定义并以参数传入，`ExtraPackConfig` 本身不再认识它。**但你删掉 `CgccPackLoader` 条目后，这里是文档中唯一出现配置文件名的地方**，删掉反而丢信息，所以我保留了。

你删掉的板块（platform 侧的 `forge.resource` / `forgeclient.resource`、client 的`主类`、core 的 `CgccPackLoader` 说明）我都没有加回去。

## 需要你决定（涉及行为，不只是改字）

根 README 写「允许该目录下没有 `pack.mcmeta` 的数据包和资源包正常加载」，但资源包现在是 `required=false`，只会出现在选择界面、需要玩家手动启用才会生效（数据包因为 `PackSource.DEFAULT` 会自动启用）。README 也完全没提 `cgccpackloader.json`。要不要补一句说明？还是你其实想让资源包也自动加载（即改 `required`）？

## 顺带一个代码问题（按你的要求：看着不对就说，不自己改）

[ExtraPackRepositorySource:135](app://localhost/epitaxy/core/src/main/java/dev/xcolorful/cgccompat/packloader/core/resource/ExtraPackRepositorySource.java:135)

```java
String id = PACK_ID_PREFIX + directory.getFileName() + "/" + name;
```

配合 `:39` 的 `private static final String PACK_ID_PREFIX = CgccPackLoader.MOD_ID;`（你同时去掉了原来的 `+ ""` 和紧跟的 `/`），id 现在会拼成 `cgccpackloadertacz/guns.zip`——模组 id 和目录名粘在一起了。不同目录仍能区分，不会实际冲突（除非出现 `a` + `b/x` 与 `ab` + `x` 这种病态组合），只是可读性明显变差。如果是有意为之就忽略。

# 
---

```
模组现在移植到1.20.2
- 修改core, forge-compat目录下代码，直到这两部分编译通过
- core第一个编译不过的地方为dev.xcolorful.cgccompat.packloader.core.resource.ExtraPackRepositorySource#resourcesSupplier
- 当完成forge-compat编译通过后，浏览PR #1，在neoforge-compat完成当前1.20.2forge完成时的版本，对照着实现

移植规范：
- 如果某个现有@Override函数变为非Override，则注释掉@Override这一行，然后在头上一行加上@Deprecated(since = "1.20.2")，而不要把这个函数删掉；除非这个函数仅仅是MC原版改了个函数名
```

# 

I'll start by reading the project docs and the mentioned file.

Now let me inspect the actual 1.20.2 API via IDEA MCP.

Now I have the full API picture. Let me make the core changes.

Now the `createPack` wrapper needs to implement both supplier methods.

Now let me compile core and forge-compat.

Core and forge-compat now compile. Let me update tasks and look at PR #1 and neoforge-compat.

Let me read PR #1's diff, as instructed.

PR #1 is the 1.20.1 implementation (no neoforge), so forge-compat is my only reference. Let me inspect the NeoForge 20.2.93 API.

NeoForge 20.2.93 API confirmed. Now let me create the neoforge-compat sources mirroring forge-compat.

Now let me compile neoforge-compat.

neoforge-compat compiles. Let me verify the Forge-side `@see` pointer I noticed was stale.

Let me verify the full build for all three modules.

移植完成，三个模块都能编译通过。下面是改动清单与验证结果。

## core（2 处编译修复）

1. [ExtraPackRepositorySource.java:107](app://localhost/epitaxy/core/src/main/java/dev/xcolorful/cgccompat/packloader/core/resource/ExtraPackRepositorySource.java:107)
    
    - 1.20.2 把 `Pack.ResourcesSupplier.open(String)` 拆成了 `openPrimary(String)` / `openFull(String, Pack.Info)`，所以返回值的 lambda 换成了匿名类。
    - `new PathPackResources(packId, path, false)` / `new FilePackResources(packId, file, false)` 在 1.20.2 变成了 supplier：改用原版的 `PathPackResources.PathResourcesSupplier` 和 `FilePackResources.FileResourcesSupplier`（`FolderRepositorySource` 内部也是这么做的）。
    - 默认文件系统的判断保留：`FileResourcesSupplier` 内部仍走 `File`。
2. [FallbackMetadataPackResources.java:49](app://localhost/epitaxy/core/src/main/java/dev/xcolorful/cgccompat/packloader/core/resource/FallbackMetadataPackResources.java:49)
    
    - 1.20.2 的 `PackMetadataSection` 变成了 record，多了 `Optional<InclusiveRange<Integer>> supportedFormats`，补 `Optional.empty()`（等价于只声明单一格式）。

## forge-compat（1 处）

`@see ServerPacksSource#createPackRepository(Path)` 里的方法在 1.20.2 已改为 `createPackRepository(Path, DirectoryValidator)`，同步更新了链接（core 的 `ExtraPackFinder` 同样一处也改了）。`ClientModLoader`、`AddPackFindersEvent`、`FMLLoader`、`FMLPaths` 在 1.20.2 都还在，未动。

## neoforge-compat（4 个新文件，对照 forge-compat 实现）

- [CgccPackLoaderNeoForge.java](app://localhost/epitaxy/neoforge-compat/src/main/java/dev/xcolorful/cgccompat/packloader/neoforge/CgccPackLoaderNeoForge.java)
- [NeoForgeDataPackListener.java](app://localhost/epitaxy/neoforge-compat/src/main/java/dev/xcolorful/cgccompat/packloader/neoforge/resource/NeoForgeDataPackListener.java)
- [CgccPackLoaderNeoForgeClient.java](app://localhost/epitaxy/neoforge-compat/src/main/java/dev/xcolorful/cgccompat/packloader/neoforgeclient/CgccPackLoaderNeoForgeClient.java)
- [NeoForgeClientResourcePackListener.java](app://localhost/epitaxy/neoforge-compat/src/main/java/dev/xcolorful/cgccompat/packloader/neoforgeclient/resource/NeoForgeClientResourcePackListener.java)

NeoForge 20.2.93 的差异点：`@Mod` 只有 `value()`（没有 `modid()` 别名）；`Mod.EventBusSubscriber.Bus` 的枚举值是 `FORGE` / `MOD`（默认 `FORGE`，`AddPackFindersEvent` 是 `IModBusEvent`，所以必须显式写 `bus = Bus.MOD`）；`Dist` 在 `net.neoforged.api.distmarker`。资源文件（`mods.toml` / `pack.mcmeta`）继续由 core 统一提供，`neoforge-compat/build.gradle` 里已配好。

## 验证

`./gradlew :core:build :forge-compat:build :neoforge-compat:build` → **BUILD SUCCESSFUL**，无警告。`@Override` 全部仍然有效（编译器会校验），所以移植规范里那条 `@Deprecated(since = "1.20.2")` 的规则本次没有触发。

未做游戏内验证（仓库里没有可启动的运行环境），包是否真的出现在选择界面仍需你实测。也没有提交。

## 一处需要你决定

我查了 1.20.2 内置的 `version.json`：`pack_version` 是 `{"resource": 18, "data": 18}`。但 `CgccPackLoader.PACK_FORMAT` 和 `gradle.properties` 里的 `pack_format` 都还是 **15**（1.20.1 的值）。这不是编译问题，但会影响运行时——回退元数据生成的描述符以及模组自带包的 `pack.mcmeta` 都会被标成与 1.20.2 不兼容。改不改由你定：

明白，`pack_format` 保持 15 不动，我只是报告。

最终状态：

- **core**：`ExtraPackRepositorySource`（supplier 拆成 `openPrimary`/`openFull` + 改用原版两个 `ResourcesSupplier`）、`FallbackMetadataPackResources`（`PackMetadataSection` 多出的 `supportedFormats` 补 `Optional.empty()`）——已编译通过。
- **forge-compat**：仅同步了一个失效的 `@see` 链接（`createPackRepository` 在 1.20.2 多了 `DirectoryValidator` 参数），代码本身无需改动。
- **neoforge-compat**：新增 4 个类，对照 forge-compat 实现，已编译通过。
- 三个模块 `./gradlew ...:build` 全部成功，无警告；未提交，等你 review。

known issue（按你的选择未处理）：`PACK_FORMAT` 与 `gradle.properties` 的 `pack_format` 仍是 1.20.1 的 15，而 1.20.2 是 18。

# 

```
模组现在移植到1.20.4，编译通过能运行，你找一下1.20.4的资源包版本号，并修改到主类里
```

# 

The mod port to 1.20.4 is a different worktree — let me locate it first.

1.20.4's `pack_version` is `{"resource": 22, "data": 26}`. Let me check the current value and gradle.properties.

改完了。1.20.4 的内置 `version.json` 是：

```json
"pack_version": { "resource": 22, "data": 26 }
```

资源包版本号是 **22**，已把 [CgccPackLoader.java:21](app://localhost/epitaxy/core/src/main/java/dev/xcolorful/cgccompat/packloader/CgccPackLoader.java:21) 的 `PACK_FORMAT` 从 18 改成 22（18 是 1.20.2 的值），`:core:compileJava` 通过。

两点相关的情况报一下，我没有动：

1. **1.20.4 起资源包与数据包的格式号分叉了**（22 vs 26）。之前 1.20.1（15/15）、1.20.2（18/18）是一致的，所以单个 `PACK_FORMAT` 常量够用；1.20.4 之后 `ExtraPackRepositorySource` 为 `SERVER_DATA` 和 `CLIENT_RESOURCES` 用的是同一个常量，数据包会被声明成 22 而游戏期望 26，可能在数据包列表里被标成不兼容。要不要按 `PackType` 分开取值，由你定。
2. `gradle.properties:40` 的 `pack_format` 仍是 **15**，它管的是模组自带 `pack.mcmeta`，不是主类里的常量（这点和 1.20.2 那次一样）。

未提交，等你 review。
