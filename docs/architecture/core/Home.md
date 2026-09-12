[English](#English)

# 架构总览

> 本文档作为项目架构的导航索引

## 项目结构

基于`dev.xcolorful.cgccompat.packloader.core`顶层包的模块划分

### 资源
> _./core/resource_

- ExtraPackConfig：读写列出额外包目录的配置文件`cgccpackloader.json`，文件缺失时写入默认目录
- ExtraPackRepositorySource：把配置目录的每个直接子项（文件夹/`.zip`）注册为包的`RepositorySource`
- FallbackMetadataPackResources：代理真实包，在缺少`pack.mcmeta`时补上以文件夹名/压缩包名为描述、`pack_format`为 15 的元数据
- ExtraPackFinder：数据包侧的实现入口，接收平台侧的注册回调并注册`SERVER_DATA` finder

# English

> This document serves as a navigation index for the project architecture

## Project Structure

Module division based on the `dev.xcolorful.cgccompat.packloader.core` top-level package

### Resource
> _./core/resource_

- ExtraPackConfig: Reads and writes `cgccpackloader.json`, the config listing extra pack directories; writes the default entry when the file is absent
- ExtraPackRepositorySource: `RepositorySource` contributing every direct child of a configured directory (folder / `.zip`) as a pack
- FallbackMetadataPackResources: Wraps a real pack and supplies metadata described by the folder or archive name with `pack_format` 15 when `pack.mcmeta` is missing
- ExtraPackFinder: Data pack entry point; takes the platform registration callback and registers the `SERVER_DATA` finder
