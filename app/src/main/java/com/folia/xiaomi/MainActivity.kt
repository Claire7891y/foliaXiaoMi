package com.folia.xiaomi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.folia.xiaomi.adapt.XiaomiAdapt
import com.folia.xiaomi.player.PlayerManager
import com.folia.xiaomi.ui.MainScreen
import com.folia.xiaomi.ui.MainViewModel
import com.folia.xiaomi.ui.theme.FoliaTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private var permissionGranted by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshPermissionAndScan()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 小米适配：内容延伸到状态栏/导航栏区域，刘海与手势条交给 Compose 处理
        XiaomiAdapt.edgeToEdge(this)

        // 初始化全局播放器（与前台媒体服务共享）
        PlayerManager.init(applicationContext)
        vm.refreshOnlineStatus()

        permissionGranted = hasAudioPermission()
        if (permissionGranted) vm.scanLocal()

        setContent {
            FoliaTheme {
                MainScreen(
                    vm = vm,
                    hasPermission = permissionGranted,
                    onGrantPermission = {
                        permissionLauncher.launch(audioPermissions())
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 用户可能从系统设置页返回，刷新权限状态
        refreshPermissionAndScan()
    }

    private fun refreshPermissionAndScan() {
        val granted = hasAudioPermission()
        if (granted && !permissionGranted) vm.scanLocal()
        permissionGranted = granted
    }

    private fun hasAudioPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }

    private fun audioPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
}
