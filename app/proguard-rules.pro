# FoliaXiaomi ProGuard 规则
# 在线搜索走 OkHttp + org.json，暂无反射需求；如需体积优化可在 release 开启 minify。

# Media3 提醒：若开启 minify，需保留 media3 的相关类，此处预置常见规则
-keep class androidx.media3.** { *; }
-dontwarn org.slf4j.**
