package eu.kanade.presentation.more.settings.screen.about

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.LogoHeader
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.ui.more.NewUpdateScreen
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.updaterEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import logcat.LogPriority
import mihon.app.di.appGraph
import mihon.icons.simpleicons.Facebook
import mihon.icons.simpleicons.Github
import mihon.icons.simpleicons.Instagram
import mihon.icons.simpleicons.SimpleIcons
import mihon.icons.simpleicons.X
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.release.interactor.GetApplicationRelease
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.LinkIcon
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.api.get
import kotlin.time.Instant

object AboutScreen : Screen() {

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val handleBack = LocalBackPress.current
        val navigator = LocalNavigator.currentOrThrow
        var isCheckingUpdates by remember { mutableStateOf(false) }
        val crashLogUtil = remember { context.appGraph.crashLogUtil }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.pref_category_about),
                    navigateUp = if (handleBack != null) handleBack::invoke else null,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            ScrollbarLazyColumn(
                contentPadding = contentPadding,
            ) {
                item {
                    LogoHeader(
                        iconPadding = PaddingValues(vertical = 24.dp),
                    )
                }

                item {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.version),
                        // Keyed on the system 12/24h setting, which can change while the app is backgrounded.
                        // Date/time prefs only change in Appearance, which recreates this screen on return.
                        // Tampilan KomKy: "Stable vX.Y.Z" tanpa kode commit. Info debug lengkap
                        // tetap tersalin ke clipboard saat baris ini diketuk.
                        subtitle = getVersionName(withBuildDate = true),
                        onPreferenceClick = {
                            val deviceInfo = crashLogUtil.getDebugInfo()
                            context.copyToClipboard("Debug information", deviceInfo)
                        },
                    )
                }

                if (updaterEnabled) {
                    item {
                        TextPreferenceWidget(
                            title = stringResource(MR.strings.check_for_updates),
                            widget = {
                                AnimatedVisibility(visible = isCheckingUpdates) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 3.dp,
                                    )
                                }
                            },
                            onPreferenceClick = {
                                if (!isCheckingUpdates) {
                                    scope.launch {
                                        isCheckingUpdates = true

                                        checkVersion(
                                            context = context,
                                            onAvailableUpdate = { result ->
                                                val updateScreen = NewUpdateScreen(
                                                    versionName = result.release.version,
                                                    changelogInfo = result.release.info,
                                                    releaseLink = result.release.releaseLink,
                                                    downloadLink = result.release.downloadLink,
                                                )
                                                navigator.push(updateScreen)
                                            },
                                            onFinish = {
                                                isCheckingUpdates = false
                                            },
                                        )
                                    }
                                }
                            },
                        )
                    }
                }

                // WAJIB dipertahankan: daftar lisensi sumber terbuka (kepatuhan Apache 2.0).
                item {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.licenses),
                        onPreferenceClick = { navigator.push(OpenSourceLicensesScreen()) },
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = "Tentang kami",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = ABOUT_US_TEXT,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        LinkIcon(
                            label = "Instagram",
                            icon = SimpleIcons.Instagram,
                            url = "https://www.instagram.com/m.rizky_alfauzi",
                        )
                        LinkIcon(
                            label = "Facebook",
                            icon = SimpleIcons.Facebook,
                            url = "https://www.facebook.com/share/1PpRFDgjPX/",
                        )
                        LinkIcon(
                            label = "X",
                            icon = SimpleIcons.X,
                            url = "https://x.com/Rizkyapasi",
                        )
                        LinkIcon(
                            label = "GitHub",
                            icon = SimpleIcons.Github,
                            url = "https://github.com/mochamadrizkyalfauzi-ops",
                        )
                    }
                }
            }
        }
    }

    /**
     * Checks version and shows a user prompt if an update is available.
     */
    private suspend fun checkVersion(
        context: Context,
        onAvailableUpdate: (GetApplicationRelease.Result.NewUpdate) -> Unit,
        onFinish: () -> Unit,
    ) {
        val updateChecker = context.appGraph.updateChecker
        withContext(Dispatchers.Main) {
            try {
                when (val result = updateChecker.checkForUpdate(forceCheck = true)) {
                    is GetApplicationRelease.Result.NewUpdate -> {
                        onAvailableUpdate(result)
                    }
                    is GetApplicationRelease.Result.NoNewUpdate -> {
                        context.toast(MR.strings.update_check_no_new_updates)
                    }
                    is GetApplicationRelease.Result.OsTooOld -> {
                        context.toast(MR.strings.update_check_eol)
                    }
                }
            } catch (e: Exception) {
                context.toast(e.message)
                logcat(LogPriority.ERROR, e)
            } finally {
                onFinish()
            }
        }
    }

    private const val ABOUT_US_TEXT =
        "KomKy adalah aplikasi pembaca komik untuk Android yang dikembangkan oleh Mochamad Rizky Alfauzi. " +
            "Aplikasi ini dirancang agar pustaka komik tertata rapi, nyaman dibaca, dan bebas iklan di dalam aplikasi. " +
            "Terima kasih telah menggunakan KomKy. Saran dan masukan Anda sangat berarti bagi pengembangan aplikasi ke depan."

    /**
     * Label versi KomKy: "Stable v0.2.1" (+ " (dd/MM/yy HH.mm)" bila withBuildDate).
     * Angka versi dari versionName di app/build.gradle.kts (satu-satunya sumber).
     */
    fun getVersionName(withBuildDate: Boolean): String {
        val label = "Stable v${BuildConfig.VERSION_NAME.substringBefore('-')}"
        return if (withBuildDate) "$label (${getFormattedBuildTime()})" else label
    }

    /** Waktu build, format terkunci dd/MM/yy HH.mm (zona waktu HP), mis. 22/11/26 10.50. */
    internal fun getFormattedBuildTime(): String {
        return try {
            val t = Instant.parse(BuildConfig.BUILD_TIME).toLocalDateTime(TimeZone.currentSystemDefault())
            fun two(n: Int) = n.toString().padStart(2, '0')
            "${two(t.dayOfMonth)}/${two(t.month.number)}/${two(t.year % 100)} ${two(t.hour)}.${two(t.minute)}"
        } catch (_: Exception) {
            BuildConfig.BUILD_TIME
        }
    }
}
