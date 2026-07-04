package com.nathan.applock.ui.applist

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nathan.applock.data.AppLockerModule
import com.nathan.applock.util.toImageBitmapSafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppItem(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?
)

data class AppListUiState(
    val isLoading: Boolean = true,
    val apps: List<AppItem> = emptyList(),
    val lockedPackages: Set<String> = emptySet(),
    val hiddenPackages: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isPinSetUp: Boolean = false
)

class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val lockRepo = AppLockerModule.lockRepository
    private val authRepo = AppLockerModule.secureAuthRepository

    private val _isLoading = MutableStateFlow(true)
    private val _apps = MutableStateFlow<List<AppItem>>(emptyList())
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<AppListUiState> = combine(
        _isLoading,
        _apps,
        lockRepo.lockedPackages,
        lockRepo.hiddenPackages,
        _searchQuery
    ) { isLoading, apps, locked, hidden, query ->
        val filtered = if (query.isBlank()) {
            apps
        } else {
            apps.filter { it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
        }
        AppListUiState(
            isLoading = isLoading,
            apps = filtered,
            lockedPackages = locked,
            hiddenPackages = hidden,
            searchQuery = query,
            isPinSetUp = authRepo.isPinSetUp
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppListUiState(isPinSetUp = authRepo.isPinSetUp)
    )

    init {
        loadApps()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleAppLock(packageName: String, locked: Boolean) {
        viewModelScope.launch {
            lockRepo.setPackageLocked(packageName, locked)
        }
    }

    fun toggleAppHidden(packageName: String, hidden: Boolean) {
        viewModelScope.launch {
            lockRepo.setPackageHidden(packageName, hidden)
        }
    }

    fun refreshPinState() {
        _isLoading.value = _isLoading.value
    }

    private fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val loaded = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(intent, 0)
                val myPackage = getApplication<Application>().packageName

                resolveInfos
                    .mapNotNull { info ->
                        val pkg = info.activityInfo.packageName
                        if (pkg == myPackage) return@mapNotNull null
                        val label = info.loadLabel(pm).toString()
                        val icon = info.loadIcon(pm).toImageBitmapSafe()
                        AppItem(pkg, label, icon)
                    }
                    .distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }
            }
            _apps.value = loaded
            _isLoading.value = false
        }
    }
}
