# 自定义枪械永续兼容：包加载 | CGC Compat: PackLoader

[中文](#自定义枪械永续兼容包加载) | [English](#cgc-compat-packloader)

# 自定义枪械永续兼容：包加载

添加额外的数据包和资源包目录。

---

`该模组可选择安装在服务端、客户端或双端`

## 主要特色

这个模组一共做三件事情：
1. 将`./config/cgccpackloader.json`中的目录列表作为额外的数据包和资源包目录，默认添加 _./tacz/_ 目录
2. 新发现的资源包会被默认勾选，玩家也能像原版资源包那样正常取消
3. 允许该目录下没有`pack.mcmeta`的数据包和资源包正常加载

### 配置

`./config/cgccpackloader.json`是额外包目录的列表，相对路径以游戏目录为基准解析：

```json
[
  "./tacz/"
]
```

`./config/cgccpackloader-client.json`只在客户端生成，装着上面第 2 条的开关和记录：

```json
{
  "resourcePackEnabledByDefault": true,
  "defaultEnabled": []
}
```

- `resourcePackEnabledByDefault`：为`true`（默认值）时按上面第 2 条默认勾选一次；设为`false`则完全不插手，全由玩家在资源包界面里决定
- `defaultEnabled`：模组自己维护，记录已经默认勾选过的包 id，用来区分「还没默认勾过」和「玩家已经取消」——玩家取消过的包不会被重新勾上。删掉这一项（或整个文件）会当作全部没勾过，重新默认勾选一次

`resourcePackEnabledByDefault`只作用于资源包。数据包由游戏自身处理：新发现的数据包会自动启用，而在某个存档里手动关闭后会记进该存档，重启后保持关闭。

## 内容披露

### 衍生内容

本模组未使用其他模组的代码

## 许可证

- 代码：[GPL-3.0-only](https://www.gnu.org/licenses/gpl-3.0.txt)

# CGC Compat: PackLoader

Add additional data pack and resource pack directories.

---

`This mod can be installed on the server, client, or both.`

## Main Features

This mod does three things:
1. Uses the directory list in `./config/cgccpackloader.json` as additional data pack and resource pack directories, with _./tacz/_ added by default
2. Newly found resource packs are enabled by default, and can be turned off like any vanilla resource pack
3. Allows data packs and resource packs in this directory without `pack.mcmeta` to load normally

### Configuration

`./config/cgccpackloader.json` is the list of extra pack directories; relative paths resolve against the game directory:

```json
[
  "./tacz/"
]
```

`./config/cgccpackloader-client.json` is only created on the client, and holds the switch and the record for feature 2:

```json
{
  "resourcePackEnabledByDefault": true,
  "defaultEnabled": []
}
```

- `resourcePackEnabledByDefault`: when `true` (the default) it enables packs once as described above; set it to `false` to stay out of the way entirely, leaving everything to the player in the resource pack screen
- `defaultEnabled`: maintained by the mod; it records the ids that have already been enabled by default, which is how "not offered yet" is told apart from "the player turned it off" — a pack the player disabled is never re-enabled. Removing this entry (or the whole file) makes every pack count as unseen, so they are all enabled by default once again

`resourcePackEnabledByDefault` only affects resource packs. Data packs are handled by the game itself: newly found data packs are enabled automatically, and turning one off in a world is recorded in that world, so it stays off after a restart.

## Content disclosures

### Derivative content

This mod does not use code from other mods

## License

- Code: [GPL-3.0-only](https://www.gnu.org/licenses/gpl-3.0.txt)
