package com.assistive.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("assistive", Context.MODE_PRIVATE)
    var setupDone: Boolean
        get() = sp.getBoolean("setupDone", false)
        set(v) = sp.edit().putBoolean("setupDone", v).apply()
    var apps: List<String>
        get() = (sp.getString("apps", "") ?: "").split(",").filter { it.isNotBlank() }
        set(v) = sp.edit().putString("apps", v.joinToString(",")).apply()
    var columns: Int
        get() = sp.getInt("columns", 2)
        set(v) = sp.edit().putInt("columns", v).apply()
    /** 0 = automatic, 1 = light, 2 = dark */
    var theme: Int
        get() = sp.getInt("theme", 0)
        set(v) = sp.edit().putInt("theme", v).apply()
    var pin: String
        get() = sp.getString("pin", "") ?: ""
        set(v) = sp.edit().putString("pin", v).apply()
}

class AppState(private val p: Prefs) {
    var setupDone by mutableStateOf(p.setupDone)
        private set
    var apps by mutableStateOf(p.apps)
        private set
    var columns by mutableStateOf(p.columns)
        private set
    var theme by mutableStateOf(p.theme)
        private set
    var pin by mutableStateOf(p.pin)
        private set

    fun finishSetup() { setupDone = true; p.setupDone = true }
    fun setApps(v: List<String>) { apps = v; p.apps = v }
    fun setColumns(v: Int) { columns = v; p.columns = v }
    fun setTheme(v: Int) { theme = v; p.theme = v }
    fun setPin(v: String) { pin = v; p.pin = v }
}

data class AppItem(val pkg: String, val label: String, val icon: ImageBitmap)

object AppRepo {
    private val cache = HashMap<String, ImageBitmap>()

    private fun resolvePkg(pm: PackageManager, i: Intent): String? =
        pm.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName?.takeIf { it != "android" }

    fun loadAll(ctx: Context): List<AppItem> {
        val pm = ctx.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val dialer = resolvePkg(pm, Intent(Intent.ACTION_DIAL))
        val out = LinkedHashMap<String, AppItem>()
        for (ri in pm.queryIntentActivities(main, 0)) {
            val pkg = ri.activityInfo.packageName
            if (pkg == ctx.packageName || out.containsKey(pkg)) continue
            var label = ri.loadLabel(pm).toString()
            if (pkg == dialer) label = "Calls"
            val icon = synchronized(cache) { cache[pkg] } ?: ri.loadIcon(pm)
                .toBitmap(160, 160).asImageBitmap()
                .also { synchronized(cache) { cache[pkg] = it } }
            out[pkg] = AppItem(pkg, label, icon)
        }
        return out.values.sortedBy { it.label.lowercase() }
    }

    /** Package names of sensible default apps (phone, messages, camera, gallery, browser). */
    fun defaults(ctx: Context): List<String> {
        val pm = ctx.packageManager
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)
        )
        return intents.mapNotNull { resolvePkg(pm, it) }.distinct()
    }

    fun launch(ctx: Context, pkg: String) {
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    }
}
