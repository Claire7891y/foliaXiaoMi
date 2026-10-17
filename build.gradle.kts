// 顶层构建文件：声明用到的插件版本（实际由 settings 中 libs.versions.toml 统一管理）
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
