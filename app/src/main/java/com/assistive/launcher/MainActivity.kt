package com.assistive.launcher

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val state = AppState(Prefs(applicationContext))
        setContent { Root(state, this) }
    }
}

@Composable
fun Root(state: AppState, activity: ComponentActivity) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val dark = when (state.theme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val colors = if (dark) DarkColors else LightColors

    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    val tick = rememberResumeTick()
    LaunchedEffect(tick) { apps = withContext(Dispatchers.Default) { AppRepo.loadAll(ctx) } }

    SideEffect {
        val c = WindowCompat.getInsetsController(activity.window, view)
        c.isAppearanceLightStatusBars = !dark
        c.isAppearanceLightNavigationBars = !dark
    }

    // 0 = home, 1 = setup, 2 = admin
    var screen by remember { mutableIntStateOf(if (state.setupDone) 0 else 1) }
    var askPin by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    fun requestHome() {
        if (Build.VERSION.SDK_INT >= 29) {
            val rm = ctx.getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (!rm.isRoleHeld(RoleManager.ROLE_HOME)) roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
                return
            }
        }
        try { ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) } catch (_: Exception) {}
    }

    BackHandler(enabled = screen == 0) { /* launcher: back does nothing */ }

    CompositionLocalProvider(LocalA provides colors) {
        MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
            when (screen) {
                0 -> HomeScreen(state, apps) {
                    if (state.pin.isEmpty()) screen = 2 else { pinError = null; askPin = true }
                }
                1 -> SetupFlow(
                    state, apps,
                    onStart = { screen = 0; requestHome() },
                    onLater = { activity.finish() }
                )
                else -> AdminScreen(
                    state, apps,
                    onDone = { screen = 0 },
                    onSetupAgain = { screen = 1 },
                    onMakeDefault = { requestHome() }
                )
            }
            if (askPin) {
                PinDialog(
                    title = "Enter Passcode", errorText = pinError,
                    onSubmit = { if (it == state.pin) { askPin = false; screen = 2 } else pinError = "Wrong passcode" },
                    onCancel = { askPin = false }
                )
            }
        }
    }
}
