package eu.kanade.tachiyomi.ui.poster

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.util.Screen
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen

class PosterScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val viewModel = metroViewModel<PosterViewModel>()
        val state by viewModel.state.collectAsState()
        var picking by rememberSaveable { mutableStateOf(false) }

        val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) viewModel.setPhoto(uri)
        }

        BackHandler(enabled = picking) { picking = false }

        if (picking) {
            Scaffold(
                topBar = { scrollBehavior ->
                    AppBar(
                        title = "Pilih komik (${state.selectedIds.size}/$POSTER_MAX_COMICS)",
                        navigateUp = { picking = false },
                        scrollBehavior = scrollBehavior,
                    )
                },
                snackbarHost = { SnackbarHost(hostState = viewModel.snackbarHostState) },
            ) { padding ->
                if (state.library.isEmpty()) {
                    EmptyScreen(
                        message = "Belum ada komik di pustaka",
                        modifier = Modifier.padding(padding),
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(110.dp),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = padding.calculateTopPadding() + 8.dp,
                            bottom = padding.calculateBottomPadding() + 8.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.library, key = { it.id }) { item ->
                            val selected = item.id in state.selectedIds
                            Box(modifier = Modifier.clickable { viewModel.toggleComic(item.id) }) {
                                MangaCover.Book(
                                    data = item.manga,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                                    )
                                    Icon(
                                        imageVector = MaterialSymbols.Rounded.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.align(Alignment.Center),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            return
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = "Poster komik",
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
            snackbarHost = { SnackbarHost(hostState = viewModel.snackbarHostState) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    val preview = state.preview
                    if (preview != null) {
                        Image(
                            bitmap = preview.asImageBitmap(),
                            contentDescription = "Pratinjau poster",
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = "Pilih foto dan $POSTER_MIN_COMICS–$POSTER_MAX_COMICS komik untuk melihat pratinjau poster.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    if (state.rendering) {
                        CircularProgressIndicator()
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            pickPhoto.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (state.photoUri == null) "Pilih foto" else "Ganti foto")
                    }
                    Button(
                        onClick = { picking = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Pilih komik (${state.selectedIds.size}/$POSTER_MAX_COMICS)")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = viewModel::shuffle,
                        enabled = state.preview != null && !state.rendering,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Susun ulang")
                    }
                    Text(
                        text = "Watermark KomKy",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                    )
                    Switch(
                        checked = state.watermark,
                        onCheckedChange = viewModel::setWatermark,
                    )
                }

                val canExport = state.preview != null && !state.rendering && !state.saving
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = viewModel::savePoster,
                        enabled = canExport,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Simpan")
                    }
                    OutlinedButton(
                        onClick = { viewModel.sharePoster(context) },
                        enabled = canExport,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Bagikan")
                    }
                }
            }
        }
    }
}
