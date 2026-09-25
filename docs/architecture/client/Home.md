[English](#English)

# 架构总览

> 本文档作为项目架构的导航索引

## 项目结构

基于`dev.xcolorful.cgccompat.packloader.client`顶层包的模块划分

### 资源
> _./client/resource_

- ClientResourcePackFinder：资源包侧的实现入口，接收平台侧的注册回调并注册`CLIENT_RESOURCES` finder
- ClientPackDefaults：按客户端文件`cgccpackloader-client.json`里的`resourcePackEnabledByDefault`把新发现的资源包默认勾选一次，并把已勾过的 id 记在同一文件的`defaultEnabled`里；玩家此后的取消由原版`options.txt`保留，不会再被勾回来

# English

> This document serves as a navigation index for the project architecture

## Project Structure

Module division based on the `dev.xcolorful.cgccompat.packloader.client` top-level package

### Resource
> _./client/resource_

- ClientResourcePackFinder: Resource pack entry point; takes the platform registration callback and registers the `CLIENT_RESOURCES` finder
- ClientPackDefaults: According to `resourcePackEnabledByDefault` in its own client file `cgccpackloader-client.json`, enables newly found resource packs once by default and records the ids it has seen under `defaultEnabled` in that same file; a later opt-out is kept by vanilla `options.txt` and never re-enabled
