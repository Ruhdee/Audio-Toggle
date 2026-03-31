package com.example.audiotoggle

import android.app.Activity
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import rikka.shizuku.Shizuku

class MainActivity : Activity() {

    companion object {
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
        private const val KEY_MASTER_MONO = "master_mono"
        private const val KEY_MASTER_BALANCE = "master_balance"
        private const val ALIAS_MONO_ON = "com.example.audiotoggle.MainActivityMonoOn"
        private const val ALIAS_MONO_OFF = "com.example.audiotoggle.MainActivityMonoOff"
    }

    private var started = false

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode != SHIZUKU_PERMISSION_REQUEST_CODE) {
            return@OnRequestPermissionResultListener
        }
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            runToggle()
        } else {
            finish()
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val remote = IShizukuShellService.Stub.asInterface(service)
            runCommands(remote)
            unbindUserService()
            finish()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        runToggle()
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishing) {
            finish()
        }
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        super.onDestroy()
    }

    private fun runToggle() {
        if (started) return
        started = true

        if (!ensureShizukuReady()) {
            finish()
            return
        }

        val args = Shizuku.UserServiceArgs(
            ComponentName(packageName, ShizukuShellService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("shizuku")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
            .tag("audio-toggle-shell")

        try {
            Shizuku.bindUserService(args, serviceConnection)
        } catch (_: Throwable) {
            finish()
        }
    }

    private fun unbindUserService() {
        val args = Shizuku.UserServiceArgs(
            ComponentName(packageName, ShizukuShellService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("shizuku")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
            .tag("audio-toggle-shell")

        Shizuku.unbindUserService(args, serviceConnection, true)
    }

    private fun ensureShizukuReady(): Boolean {
        if (!Shizuku.pingBinder()) {
            return false
        }

        if (Shizuku.isPreV11()) {
            return false
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            return true
        }

        if (Shizuku.shouldShowRequestPermissionRationale()) {
            return false
        }

        Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
        return false
    }

    private fun runCommands(service: IShizukuShellService): Boolean {
        val currentMono = Settings.System.getInt(contentResolver, KEY_MASTER_MONO, 0)
        val targetMono = if (currentMono == 0) 1 else 0
        val currentBalance = readBalance(service)
        val targetBalance = if (targetMono == 1) -1.0f else 0.0f

        val monoExit = service.exec("settings put system master_mono $targetMono")
        if (monoExit != 0) return false

        if (currentBalance != targetBalance) {
            val balanceExit = service.exec("settings put system master_balance $targetBalance")
            if (balanceExit != 0) return false
        }

        updateLauncherIcon(targetMono == 1)
        return true
    }

    private fun readBalance(service: IShizukuShellService): Float {
        val output = service.execReadLine("settings get system $KEY_MASTER_BALANCE") ?: ""
        return output.trim().toFloatOrNull() ?: 0.0f
    }

    private fun updateLauncherIcon(isMonoOn: Boolean) {
        val enableAlias = if (isMonoOn) ALIAS_MONO_ON else ALIAS_MONO_OFF
        val disableAlias = if (isMonoOn) ALIAS_MONO_OFF else ALIAS_MONO_ON

        packageManager.setComponentEnabledSetting(
            ComponentName(this, enableAlias),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        packageManager.setComponentEnabledSetting(
            ComponentName(this, disableAlias),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
