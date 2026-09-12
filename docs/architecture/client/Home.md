[English](#English)

# 架构总览

> 本文档作为项目架构的导航索引

## 项目结构

基于`dev.xcolorful.cgccompat.packloader.client`顶层包的模块划分

### 主类
> _./client_

| 类 | 功能 |
| --- | --- |
| `CgccPackLoaderClient` | 客户端入口，驱动 core 的客户端初始化 |

### 资源
> _./client/resource_

| 类 | 功能 |
| --- | --- |
| `ClientResourcePackFinder` | 资源包侧的实现入口，接收平台侧的注册回调并注册 `CLIENT_RESOURCES` finder |

### 平台侧
> _./forgeclient/resource_

| 类 | 功能 |
| --- | --- |
| `ForgeClientResourcePackListener` | 仅在物理客户端订阅 `AddPackFindersEvent`，过滤出 `CLIENT_RESOURCES` 后转交 `ClientResourcePackFinder` |

# English

> This document serves as a navigation index for the project architecture

## Project Structure

Module division based on the `dev.xcolorful.cgccompat.packloader.client` top-level package

### Main
> _./client_

| Class | Purpose |
| --- | --- |
| `CgccPackLoaderClient` | Client entry point; drives the core client-side initialization |

### Resource
> _./client/resource_

| Class | Purpose |
| --- | --- |
| `ClientResourcePackFinder` | Resource pack entry point; takes the platform registration callback and registers the `CLIENT_RESOURCES` finder |

### Platform
> _./forgeclient/resource_

| Class | Purpose |
| --- | --- |
| `ForgeClientResourcePackListener` | Subscribes to `AddPackFindersEvent` on the physical client only, filters for `CLIENT_RESOURCES` and delegates to `ClientResourcePackFinder` |
