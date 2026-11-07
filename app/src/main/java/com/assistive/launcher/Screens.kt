package com.assistive.launcher

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun rememberResumeTick(): Int {
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return tick
}

/* ───────────────────────── HOME ───────────────────────── */

@Composable
fun HomeScreen(state: AppState, apps: List<AppItem>, onAdmin: () -> Unit) {
    val a = LocalA.current
    val ctx = LocalContext.current
    val shown = remember(state.apps, apps) { state.apps.mapNotNull { p -> apps.firstOrNull { it.pkg == p } } }
    val badges = Badges.packages
    val bars = WindowInsets.systemBars.asPaddingValues()
    var taps by remember { mutableStateOf(listOf<Long>()) }

    Box(Modifier.fillMaxSize().background(a.homeBg)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(state.columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = bars.calculateTopPadding() + 48.dp,
                bottom = bars.calculateBottomPadding() + 24.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            items(shown, key = { it.pkg }) { app ->
                Tile(app, app.pkg in badges, state.columns) { AppRepo.launch(ctx, app.pkg) }
            }
        }
        if (shown.isEmpty() && state.setupDone) {
            Text(
                "No apps yet.\nTriple-tap the top of the screen to open settings.",
                color = a.label, fontSize = 17.sp, textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp)
            )
        }
        // Hidden admin gesture: triple-tap the top strip
        Box(
            Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .height(bars.calculateTopPadding() + 40.dp)
                .pointerInput(Unit) {
                    detectTapGestures {
                        val now = System.currentTimeMillis()
                        taps = (taps + now).filter { now - it < 900 }
                        if (taps.size >= 3) { taps = emptyList(); onAdmin() }
                    }
                }
        )
    }
}

@Composable
fun Tile(app: AppItem, badge: Boolean, columns: Int, onClick: () -> Unit) {
    val a = LocalA.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.95f else 1f, label = "press")
    val shape = RoundedCornerShape(30.dp)

    Box(Modifier.fillMaxWidth().aspectRatio(if (columns == 1) 2.4f else 0.98f).scale(sc)) {
        Box(
            Modifier.fillMaxSize().clip(shape).background(a.tile)
                .border(1.dp, a.tileBorder, shape)
                .clickable(interactionSource = src, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (columns == 1) {
                Row(Modifier.fillMaxSize().padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        app.icon, null, Modifier.size(88.dp).clip(RoundedCornerShape(percent = 22)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(22.dp))
                    Text(app.label, color = a.label, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Image(
                        app.icon, null,
                        Modifier.fillMaxWidth(0.6f).aspectRatio(1f).clip(RoundedCornerShape(percent = 22)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        app.label, color = a.label, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
        if (badge) Box(Modifier.align(Alignment.TopCenter).offset(y = (-14).dp)) { NewPill() }
    }
}

@Composable
fun NewPill() {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(IOSBlue).padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Notifications, null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("New", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

/* ───────────────────────── SHARED PIECES ───────────────────────── */

@Composable
fun BlueButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(14.dp))
            .background(if (enabled) IOSBlue else IOSBlue.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val a = LocalA.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(if (a.dark) Color(0xFF2C2C2E) else Color(0x1F767680)).padding(2.dp)
    ) {
        options.forEachIndexed { i, o ->
            Box(
                Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (i == selected) (if (a.dark) Color(0xFF636366) else Color.White) else Color.Transparent)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center
            ) { Text(o, color = a.text, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
        }
    }
}

@Composable
fun AppPickerList(apps: List<AppItem>, selected: List<String>, onToggle: (String) -> Unit, modifier: Modifier = Modifier) {
    val a = LocalA.current
    LazyColumn(modifier) {
        items(apps, key = { it.pkg }) { app ->
            Row(
                Modifier.fillMaxWidth().clickable { onToggle(app.pkg) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(app.icon, null, Modifier.size(46.dp).clip(RoundedCornerShape(percent = 22)), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(14.dp))
                Text(app.label, color = a.text, fontSize = 17.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Checkbox(checked = app.pkg in selected, onCheckedChange = null)
            }
        }
    }
}

@Composable
fun PinDialog(title: String, errorText: String? = null, confirmText: String = "OK", onSubmit: (String) -> Unit, onCancel: () -> Unit) {
    var v by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = v,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) v = it },
                singleLine = true,
                isError = errorText != null,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                supportingText = { if (errorText != null) Text(errorText) }
            )
        },
        confirmButton = { TextButton(onClick = { onSubmit(v) }) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
    )
}

fun toggle(list: List<String>, pkg: String) = if (pkg in list) list - pkg else list + pkg

/* ───────────────────────── SETUP FLOW ───────────────────────── */

@Composable
fun SetupFlow(state: AppState, apps: List<AppItem>, onStart: () -> Unit, onLater: () -> Unit) {
    val a = LocalA.current
    val ctx = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf(state.apps) }
    var seeded by remember { mutableStateOf(state.apps.isNotEmpty()) }
    var pin by remember { mutableStateOf("") }

    LaunchedEffect(apps) {
        if (!seeded && apps.isNotEmpty()) {
            selected = AppRepo.defaults(ctx).filter { d -> apps.any { it.pkg == d } }
            seeded = true
        }
    }
    BackHandler(enabled = step > 0) { step-- }

    Column(Modifier.fillMaxSize().background(a.plain).systemBarsPadding().padding(horizontal = 24.dp)) {
        Box(Modifier.height(44.dp), contentAlignment = Alignment.CenterStart) {
            if (step in 1..2) TextButton(onClick = { step-- }) { Text("‹ Back", color = IOSBlue, fontSize = 17.sp) }
        }
        when (step) {
            0 -> {
                Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    PhoneIllustration()
                    Spacer(Modifier.height(36.dp))
                    Text("Set Up Assistive Access", color = a.text, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Select apps and customize options for the person who will be using Assistive Access on this phone.",
                        color = a.secondary, fontSize = 15.sp, textAlign = TextAlign.Center
                    )
                }
                BlueButton("Continue") { step = 1 }
            }
            1 -> {
                Text("Choose Apps", color = a.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Pick the apps that will appear on the Home Screen.", color = a.secondary, fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))
                AppPickerList(apps, selected, { selected = toggle(selected, it) }, Modifier.weight(1f))
                Spacer(Modifier.height(12.dp))
                BlueButton("Continue", enabled = selected.isNotEmpty()) { step = 2 }
            }
            2 -> {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Text("Customize", color = a.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(24.dp))
                    Text("Layout", color = a.secondary, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    Segmented(listOf("Rows", "Grid"), if (state.columns == 1) 0 else 1) { state.setColumns(if (it == 0) 1 else 2) }
                    Spacer(Modifier.height(24.dp))
                    Text("Appearance", color = a.secondary, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    Segmented(listOf("Automatic", "Light", "Dark"), state.theme) { state.setTheme(it) }
                    Spacer(Modifier.height(24.dp))
                    Text("Passcode to exit (optional)", color = a.secondary, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                        singleLine = true,
                        placeholder = { Text("4–6 digits") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(12.dp))
                BlueButton("Continue") {
                    state.setApps(selected)
                    state.setPin(if (pin.length >= 4) pin else "")
                    step = 3
                }
            }
            else -> {
                Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(Modifier.size(68.dp).border(4.dp, IOSGreen, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Check, null, tint = IOSGreen, modifier = Modifier.size(36.dp))
                    }
                    Spacer(Modifier.height(28.dp))
                    Text("Assistive Access is ready to use", color = a.text, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "You can change settings at any time: triple-tap the top of the Home Screen.",
                        color = a.secondary, fontSize = 15.sp, textAlign = TextAlign.Center
                    )
                }
                BlueButton("Start Using Assistive Access") { state.finishSetup(); onStart() }
                TextButton(onClick = { state.finishSetup(); onLater() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Enter Later", color = IOSBlue, fontSize = 17.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun PhoneIllustration() {
    val a = LocalA.current
    val bar = if (a.dark) Color(0xFF3A3A3C) else Color(0xFFC7C7CC)
    Box(
        Modifier.size(width = 120.dp, height = 190.dp).clip(RoundedCornerShape(14.dp))
            .background(if (a.dark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7))
            .border(1.dp, if (a.dark) Color(0xFF3A3A3C) else Color(0xFFD1D1D6), RoundedCornerShape(14.dp))
    ) {
        Column(Modifier.padding(12.dp).padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) {
                Row(
                    Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(5.dp))
                        .background(if (a.dark) Color(0xFF2C2C2E) else Color.White).padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(bar))
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.height(5.dp).weight(1f).clip(RoundedCornerShape(3.dp)).background(bar))
                }
            }
        }
    }
}

/* ───────────────────────── ADMIN ───────────────────────── */

@Composable
fun Sep() {
    val a = LocalA.current
    Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(0.5.dp).background(a.separator))
}

@Composable
fun Section(header: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val a = LocalA.current
    if (header != null) {
        Text(header.uppercase(), color = a.secondary, fontSize = 13.sp, modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 6.dp))
    } else Spacer(Modifier.height(22.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(a.row), content = content)
}

@Composable
fun SettingRow(title: String, value: String? = null, color: Color? = null, onClick: () -> Unit) {
    val a = LocalA.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = color ?: a.text, fontSize = 17.sp, modifier = Modifier.weight(1f))
        if (value != null) Text(value, color = a.secondary, fontSize = 17.sp)
    }
}

@Composable
fun AdminScreen(
    state: AppState,
    apps: List<AppItem>,
    onDone: () -> Unit,
    onSetupAgain: () -> Unit,
    onMakeDefault: () -> Unit
) {
    val a = LocalA.current
    val ctx = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }
    val tick = rememberResumeTick()
    val listenerOn = remember(tick) { NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName) }

    BackHandler { if (showPicker) showPicker = false else onDone() }

    Column(Modifier.fillMaxSize().background(a.listBg).systemBarsPadding().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (showPicker) "Apps" else "Assistive Access", color = a.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = { if (showPicker) showPicker = false else onDone() }) {
                Text("Done", color = IOSBlue, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (showPicker) {
            AppPickerList(apps, state.apps, { state.setApps(toggle(state.apps, it)) }, Modifier.weight(1f))
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Section("Home Screen") {
                    SettingRow("Apps", "${state.apps.size}") { showPicker = true }
                    Sep()
                    Column(Modifier.padding(16.dp)) {
                        Text("Layout", color = a.secondary, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Segmented(listOf("Rows", "Grid"), if (state.columns == 1) 0 else 1) { state.setColumns(if (it == 0) 1 else 2) }
                    }
                    Sep()
                    Column(Modifier.padding(16.dp)) {
                        Text("Appearance", color = a.secondary, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Segmented(listOf("Automatic", "Light", "Dark"), state.theme) { state.setTheme(it) }
                    }
                }
                Section("Options") {
                    SettingRow("\"New\" badges", if (listenerOn) "On" else "Off") {
                        ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                    Sep()
                    SettingRow("Passcode", if (state.pin.isEmpty()) "Off" else "On") { showPin = true }
                    if (state.pin.isNotEmpty()) {
                        Sep()
                        SettingRow("Remove Passcode", color = IOSRed) { state.setPin("") }
                    }
                }
                Section {
                    SettingRow("Set Up Again") { onSetupAgain() }
                    Sep()
                    SettingRow("Make Default Home App") { onMakeDefault() }
                }
                Section {
                    SettingRow("Exit Assistive Access", color = IOSRed) {
                        val i = Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        try { ctx.startActivity(i) } catch (e: Exception) {
                            ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                }
                Text(
                    "To leave, pick another Home app in the screen that opens.",
                    color = a.secondary, fontSize = 13.sp, modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    if (showPin) {
        PinDialog(
            title = "Set Passcode (4–6 digits)", confirmText = "Save",
            onSubmit = { if (it.length >= 4) { state.setPin(it); showPin = false } },
            onCancel = { showPin = false }
        )
    }
}
