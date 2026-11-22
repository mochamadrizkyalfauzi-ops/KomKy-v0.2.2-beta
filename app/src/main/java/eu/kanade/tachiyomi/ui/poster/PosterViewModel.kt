package eu.kanade.tachiyomi.ui.poster

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.tachiyomi.data.saver.Image
import eu.kanade.tachiyomi.data.saver.ImageSaver
import eu.kanade.tachiyomi.data.saver.Location
import eu.kanade.tachiyomi.util.system.toShareIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.interactor.GetLibraryManga
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlin.time.Clock

internal const val POSTER_MIN_COMICS = 3
internal const val POSTER_MAX_COMICS = 5
private const val PREVIEW_WIDTH = 540

data class PosterState(
    val loading: Boolean = true,
    val library: List<LibraryManga> = emptyList(),
    val photoUri: Uri? = null,
    val selectedIds: List<Long> = emptyList(),
    val seed: Int = 1,
    val watermark: Boolean = true,
    val preview: Bitmap? = null,
    val rendering: Boolean = false,
    val saving: Boolean = false,
)

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class PosterViewModel(
    private val getLibraryManga: GetLibraryManga,
    private val imageSaver: ImageSaver,
) : ViewModel() {

    private val _state = MutableStateFlow(PosterState())
    val state: StateFlow<PosterState> = _state.asStateFlow()

    val snackbarHostState = SnackbarHostState()

    private var renderJob: Job? = null

    private val appContext: Context
        get() = imageSaver.context

    init {
        viewModelScope.launch(Dispatchers.IO) {
            getLibraryManga.subscribe().collectLatest { list ->
                val sorted = list
                    .distinctBy { it.id }
                    .sortedBy { it.manga.title.lowercase() }
                _state.update { it.copy(library = sorted, loading = false) }
            }
        }
    }

    fun setPhoto(uri: Uri) {
        _state.update { it.copy(photoUri = uri) }
        requestRender()
    }

    fun toggleComic(id: Long) {
        val current = _state.value.selectedIds
        if (id in current) {
            _state.update { it.copy(selectedIds = current - id) }
        } else if (current.size >= POSTER_MAX_COMICS) {
            showMessage("Maksimal $POSTER_MAX_COMICS komik")
            return
        } else {
            _state.update { it.copy(selectedIds = current + id) }
        }
        requestRender()
    }

    fun shuffle() {
        _state.update { it.copy(seed = Random.nextInt()) }
        requestRender()
    }

    fun setWatermark(enabled: Boolean) {
        _state.update { it.copy(watermark = enabled) }
        requestRender()
    }

    fun savePoster() {
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            try {
                saveInternal(temp = false)
                showMessage("Poster tersimpan di galeri (Pictures/KomKy)")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e)
                showMessage("Gagal menyimpan poster")
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }

    fun sharePoster(context: Context) {
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            try {
                val uri = saveInternal(temp = true)
                if (uri != null) {
                    withContext(Dispatchers.Main) {
                        context.startActivity(uri.toShareIntent(context))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e)
                showMessage("Gagal membagikan poster")
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }

    private suspend fun saveInternal(temp: Boolean): Uri? {
        val s = _state.value
        val photo = s.photoUri ?: return null
        val comics = selectedComics(s)
        if (comics.size < POSTER_MIN_COMICS) return null

        val bitmap = PosterRenderer.render(
            context = appContext,
            photoUri = photo,
            comics = comics,
            seed = s.seed,
            watermark = s.watermark,
            width = PosterRenderer.BASE_WIDTH,
        )
        return withContext(Dispatchers.IO) {
            imageSaver.save(
                Image.Cover(
                    bitmap = bitmap,
                    name = "KomKy-Poster-${Clock.System.now().epochSeconds}",
                    location = if (temp) Location.Cache else Location.Pictures.create(),
                ),
            )
        }
    }

    private fun selectedComics(s: PosterState): List<LibraryManga> {
        return s.selectedIds.mapNotNull { id -> s.library.firstOrNull { it.id == id } }
    }

    private fun requestRender() {
        renderJob?.cancel()
        val s = _state.value
        val photo = s.photoUri
        val comics = selectedComics(s)
        if (photo == null || comics.size < POSTER_MIN_COMICS) {
            _state.update { it.copy(preview = null, rendering = false) }
            return
        }
        _state.update { it.copy(rendering = true) }
        renderJob = viewModelScope.launch {
            val bitmap = try {
                PosterRenderer.render(
                    context = appContext,
                    photoUri = photo,
                    comics = comics,
                    seed = s.seed,
                    watermark = s.watermark,
                    width = PREVIEW_WIDTH,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e)
                null
            }
            _state.update { it.copy(preview = bitmap, rendering = false) }
            if (bitmap == null) showMessage("Gagal membuat poster")
        }
    }

    private fun showMessage(message: String) {
        viewModelScope.launch {
            snackbarHostState.showSnackbar(message, withDismissAction = true)
        }
    }
}
