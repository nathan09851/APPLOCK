package com.nathan.applock.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import com.nathan.applock.LockActivity
import com.nathan.applock.data.AppLockerModule
import com.nathan.applock.data.RelockPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class AppLockAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val lockRepo = AppLockerModule.lockRepository

    private var lockedPackagesCache: Set<String> = emptySet()
    private var hiddenPackagesCache: Set<String> = emptySet()
    private var relockPolicyCache: RelockPolicy = RelockPolicy.IMMEDIATELY

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                unlockedPackages.clear()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        serviceScope.launch {
            lockRepo.lockedPackages.collect { lockedPackagesCache = it }
        }
        serviceScope.launch {
            lockRepo.hiddenPackages.collect { hiddenPackagesCache = it }
        }
        serviceScope.launch {
            lockRepo.relockPolicy.collect { relockPolicyCache = it }
        }
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return

        if (packageName == this.packageName || isSystemPackage(packageName)) return

        if (lockedPackagesCache.contains(packageName)) {
            val unlockTime = unlockedPackages[packageName]
            val isExpired = if (unlockTime == null) {
                true
            } else if (relockPolicyCache != RelockPolicy.IMMEDIATELY) {
                (System.currentTimeMillis() - unlockTime) > relockPolicyCache.delayMillis
            } else {
                false
            }

            if (isExpired) {
                unlockedPackages.remove(packageName)
                launchLockActivity(packageName, hiddenPackagesCache.contains(packageName))
            }
        } else {
            if (relockPolicyCache == RelockPolicy.IMMEDIATELY && currentForegroundPackage != null && currentForegroundPackage != packageName && currentForegroundPackage != this.packageName) {
                unlockedPackages.remove(currentForegroundPackage)
            }
        }
        currentForegroundPackage = packageName
    }

    private fun launchLockActivity(packageName: String, isHidden: Boolean) {
        val intent = Intent(this, LockActivity::class.java).apply {
            putExtra(LockActivity.EXTRA_LOCKED_PACKAGE, packageName)
            putExtra(LockActivity.EXTRA_IS_HIDDEN, isHidden)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivity(intent)
    }

    private fun isSystemPackage(pkg: String): Boolean {
        return pkg == "com.android.systemui" ||
               pkg == "com.google.android.inputmethod.latin" ||
               pkg == "com.samsung.android.honeyboard" ||
               pkg == "com.sec.android.inputmethod"
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: Exception) {}
        serviceScope.cancel()
        if (instance == this) instance = null
    }

    companion object {
        private var instance: AppLockAccessibilityService? = null
        private val unlockedPackages = ConcurrentHashMap<String, Long>()
        private var currentForegroundPackage: String? = null

        fun unlockPackage(packageName: String?) {
            if (packageName != null) {
                unlockedPackages[packageName] = System.currentTimeMillis()
            }
        }

        fun lockPackage(packageName: String) {
            unlockedPackages.remove(packageName)
        }
    }
}
