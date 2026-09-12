[English](#English)

# 架构总览

> 本文档作为项目架构的导航索引

## 项目结构

基于`dev.xcolorful.cgccompat.packloader.core`顶层包的模块划分

### 资源
> _./core/resource_

| 类 | 功能 |
| --- | --- |
| `ExtraPackConfig` | 读写列出额外包目录的配置文件 `cgccpackloader.json`，文件缺失时写入默认目录 |
| `ExtraPackRepositorySource` | 把配置目录的每个直接子项（文件夹 / `.zip`）注册为包的 `RepositorySource` |
| `FallbackMetadataPackResources` | 代理真实包，在缺少 `pack.mcmeta` 时补上以目录名为描述、`pack_format` 为 15 的元数据 |
| `ExtraPackFinder` | 数据包侧的实现入口，接收平台侧的注册回调并注册 `SERVER_DATA` finder |

### 平台侧
> _./forge/resource_

| 类 | 功能 |
| --- | --- |
| `ForgeDataPackListener` | 订阅 `AddPackFindersEvent`，过滤出 `SERVER_DATA` 后转交 `ExtraPackFinder` |

> 模组主类 `CgccPackLoader` 位于 `dev.xcolorful.cgccompat.packloader`，保存平台侧传入的游戏目录与配置文件路径并提供 getter。

# English

> This document serves as a navigation index for the project architecture

## Project Structure

Module division based on the `dev.xcolorful.cgccompat.packloader.core` top-level package

### Resource
> _./core/resource_

| Class | Purpose |
| --- | --- |
| `ExtraPackConfig` | Reads and writes `cgccpackloader.json`, the config listing extra pack directories; writes the default entry when the file is absent |
| `ExtraPackRepositorySource` | `RepositorySource` contributing every direct child of a configured directory (folder / `.zip`) as a pack |
| `FallbackMetadataPackResources` | Wraps a real pack and supplies metadata described by the directory name with `pack_format` 15 when `pack.mcmeta` is missing |
| `ExtraPackFinder` | Data pack entry point; takes the platform registration callback and registers the `SERVER_DATA` finder |

### Platform
> _./forge/resource_

| Class | Purpose |
| --- | --- |
| `ForgeDataPackListener` | Subscribes to `AddPackFindersEvent`, filters for `SERVER_DATA` and delegates to `ExtraPackFinder` |

> The mod main class `CgccPackLoader` lives in `dev.xcolorful.cgccompat.packloader` and stores the game directory and config file path supplied by the platform, exposing them through getters.
