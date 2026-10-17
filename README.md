# FoliaXiaomi · 辞曲 · 适配小米的沉浸式歌词播放器

在 GitHub 开源项目 [chthollyphile/folia-major](https://github.com/chthollyphile/folia-major)（Folia，AGPL-3.0）的**产品理念**基础上，用 **Kotlin + Jetpack Compose + Media3** 从零实现的 **Android 原生播放器**，专门针对**小米手机（HyperOS / MIUI）**做了适配。

> 本项目为独立的原创 Android 工程，并不包含 Folia 的代码（避免 AGPL 传染）；只沿用其“全屏沉浸式歌词 + 多音源”的产品思路。

## 功能一览

- **沉浸式全屏歌词**：以专辑封面主色调生成渐变背景，歌词随节拍滚动，支持逐字卡拉 OK 高亮（LDDC 增强格式）
- **三种歌词动画模式**：浮动 / 逐字 / 倾诉
- **本地音乐**：MediaStore 扫描设备音频（mp3/flac 等），读取同目录同名 `.lrc` 歌词
- **在线搜索**（演示）：兼容 NeteaseCloudMusicApiEnhanced 返回结构的音源 API，搜索 + 解析播放地址 + 拉取歌词
- **前台媒体服务**：Media3 `MediaSessionService`，小米锁屏 / 控制中心可控制播放，耳机与蓝牙按键可用
- **小米专项适配**：全面屏刘海/挖孔、手势导航条、状态栏/导航栏图标随深浅色切换、沉浸式隐藏系统栏、电池优化引导

## 技术栈

| 层 | 选型 |
|---|---|
| 语言 | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.09.03) + Material3 |
| 播放 | Media3 ExoPlayer 1.4.1 + MediaSessionService |
| 图片 | Coil 2.7.0 |
| 网络 | OkHttp 4.12.0（在线音源） |
| 构建 | AGP 8.5.2，minSdk 26 / targetSdk 34 |

## 目录结构

```
FoliaXiaomi/
├── settings.gradle.kts / build.gradle.kts / gradle.properties
├── gradle/libs.versions.toml            # 统一版本目录
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/folia/xiaomi/
        │   ├── FoliaApp.kt              # 全局 ExoPlayer + 通知渠道
        │   ├── MainActivity.kt          # 入口 + 媒体权限
        │   ├── adapt/XiaomiAdapt.kt     # ★ 小米专项适配
        │   ├── data/
        │   │   ├── model/Song.kt
        │   │   ├── lyrics/LrcParser.kt  # LRC + 逐字增强解析
        │   │   └── repository/          # 本地/在线/歌词仓库
        │   ├── player/
        │   │   ├── PlaybackService.kt   # 前台媒体服务
        │   │   └── PlayerManager.kt     # 播放状态中枢(StateFlow)
        │   └── ui/
        │       ├── MainScreen.kt / MainViewModel.kt
        │       ├── playlist/            # 本地/在线曲库
        │       ├── player/              # ★ 沉浸式歌词 + 迷你播放条
        │       └── common/ theme/
        └── res/
```

## 如何构建（Android Studio）

本项目在编写环境中缺少 Android SDK，**无法在本机编译**；请在装有 Android Studio（自带 JDK 17 与 Gradle）的电脑上打开：

1. `Android Studio → Open → 选择 FoliaXiaomi 目录`
2. 等待 Gradle 首次同步下载依赖（需联网）
3. 连接小米手机（开启开发者模式与 USB 调试），`Run ▶`
4. 或 `Build → Build APK(s)` 生成安装包后传到手机安装

> 若提示 SDK 版本，请在 `SDK Manager` 安装 **Android SDK Platform 34**。

## 小米手机适配说明（`XiaomiAdapt.kt`）

- **全面屏/刘海挖孔**：`edgeToEdge()` 让内容延伸到系统栏，`windowLayoutInDisplayCutoutMode = shortEdges` 布局避让挖孔
- **手势导航条**：进入歌词页用 `enterImmersive()` 隐藏系统栏，下滑可临时唤出
- **深浅色**：状态栏/导航栏图标明暗随 `isSystemInDarkTheme()` 切换（`applySystemBarIcons`）
- **电池优化**：`requestIgnoreBatteryOptimizations()` 引导用户将应用加入小米“不省电”白名单，保证锁屏播放不被后台清理
- **长屏/高密度屏**：全程 dp 布局 + 自适应权重，适配 1080p 到 2K+ 不同小米机型

## 在线音源配置（可选，仅供学习）

Folia 桌面版支持网易云/酷狗/Navidrome。本安卓版内置了**兼容 NeteaseCloudMusicApiEnhanced** 的在线搜索演示：

1. 自建/找一个 `NeteaseCloudMusicApiEnhanced` 实例（如本地 Docker 或 Vercel 部署）
2. 在 App「在线」页点右上角 **「音源」**，填入 API 基址，如 `https://your-api.example.com`
3. 返回搜索即可播放

```
在线搜索   GET {base}/search?keywords=xxx&limit=30  -> result.songs[{id,name,ar,al,dt}]
播放地址   GET {base}/song/url?id=xxx&br=320000     -> data[0].url
歌词       GET {base}/lyric?id=xxx                   -> lrc.lyric / tlyric.lyric
```

> ⚠️ 在线曲库、歌词与封面版权归原平台所有，**仅用于个人学习与技术演示**。请通过官方平台支持正版音乐。未配置音源时，在线页会提示，不影响本地播放。

## 已知边界

- 未内置 FFmpeg 扩展解码器（`EXTENSION_RENDERER_MODE_PREFER` 会优先硬件解码，缺扩展时自动回退）
- 在线接口依赖第三方 API 的稳定性，若地址失效需自行更换
- 未做通知栏“上一首/下一首”与歌词页同步的完整交互测试，建议真机验证
- 本地歌词仅读取音频**同目录同名 `.lrc`**，未做内嵌歌词解析

## 许可证

本工程为原创代码，仅供学习交流；涉及在线内容版权归原权利人所有，请遵守 AGPL-3.0 及平台规则。
