package com.nathan.applock

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.nathan.applock.service.AppLockAccessibilityService
import com.nathan.applock.ui.pin.PinEntryScreen
import com.nathan.applock.ui.theme.AppLockTheme

class LockActivity : FragmentActivity() {

    private var lockedPackage by mutableStateOf<String?>(null)
    private var isHiddenApp by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lockedPackage = intent.getStringExtra(EXTRA_LOCKED_PACKAGE)
        isHiddenApp = intent.getBooleanExtra(EXTRA_IS_HIDDEN, false)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                finish()
            }
        })

        setContent {
            AppLockTheme {
                var showCrashScreen by remember { mutableStateOf(isHiddenApp) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showCrashScreen) {
                        DummyCrashScreen(
                            packageName = lockedPackage ?: "App",
                            onRevealPin = { showCrashScreen = false },
                            onCloseApp = {
                                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_HOME)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                startActivity(homeIntent)
                                finish()
                            }
                        )
                    } else {
                        PinEntryScreen(
                            targetPackageName = lockedPackage,
                            onUnlockSuccess = {
                                AppLockAccessibilityService.unlockPackage(lockedPackage)
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val newPkg = intent.getStringExtra(EXTRA_LOCKED_PACKAGE)
        if (newPkg != null) {
            lockedPackage = newPkg
            isHiddenApp = intent.getBooleanExtra(EXTRA_IS_HIDDEN, false)
        }
    }

    companion object {
        const val EXTRA_LOCKED_PACKAGE = "extra_locked_package"
        const val EXTRA_IS_HIDDEN = "extra_is_hidden"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DummyCrashScreen(
    packageName: String,
    onRevealPin: () -> Unit,
    onCloseApp: () -> Unit
) {
    val appName = packageName.substringAfterLast('.').replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2C2C2C))
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = Color(0xFFFF6B6B),
                    modifier = Modifier
                        .size(28.dp)
                        .combinedClickable(
                            onClick = {},
                            onDoubleClick = onRevealPin,
                            onLongClick = onRevealPin
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "App Crash Alert",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Unfortunately, $appName has stopped unexpectedly due to a system null pointer exception.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFCCCCCC)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onCloseApp,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF888888))
                ) {
                    Text("App info")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .combinedClickable(
                            onClick = onCloseApp,
                            onLongClick = onRevealPin
                        )
                ) {
                    Button(
                        onClick = onCloseApp,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F5F90)),
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text("Close app", color = Color.White)
                    }
                }
            }
        }
    }
}
