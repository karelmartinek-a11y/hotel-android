package cz.hcasc.hotel.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import cz.hcasc.hotel.App
import cz.hcasc.hotel.BuildConfig
import cz.hcasc.hotel.repo.DeviceRepo
import cz.hcasc.hotel.repo.QueueRepo
import cz.hcasc.hotel.repo.ReportsRepo
import cz.hcasc.hotel.repo.model.ReportCategory
import cz.hcasc.hotel.repo.model.ReportItem
import cz.hcasc.hotel.work.SendQueueWorker
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    title: String,
    subtitle: String,
    category: ReportCategory,
    emptyText: String
) {
    val context = LocalContext.current
    val deviceRepo = remember { DeviceRepo.from(context) }
    val reportsRepo = remember { reportsRepo(context) }
    val queueRepo = remember { QueueRepo.get(context) }
    val scope = rememberCoroutineScope()

    var activated by remember { mutableStateOf<Boolean?>(null) }
    var items by remember { mutableStateOf<List<ReportItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var actionLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Create-report UI state
    var showCreate by remember { mutableStateOf(false) }
    var createType by remember { mutableStateOf(category) }
    var createRoom by remember { mutableStateOf("") }
    var createDesc by remember { mutableStateOf("") }
    var createPhotos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        refreshActivationAndData(
            deviceRepo,
            reportsRepo,
            category,
            onActivated = { activated = it },
            onItems = { items = it },
            onLoading = { loading = it },
            onError = { error = it }
        )
    }

    // Pick images (gallery)
    val pickImages = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris != null && uris.isNotEmpty()) {
            createPhotos = (createPhotos + uris).distinct().take(BuildConfig.MAX_PHOTOS_PER_REPORT)
        }
    }

    // Take photo (camera)
    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { ok ->
        val uri = cameraUri
        cameraUri = null
        if (ok && uri != null) {
            createPhotos = (createPhotos + uri).distinct().take(BuildConfig.MAX_PHOTOS_PER_REPORT)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        when (activated) {
            null -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                    Text("Kontroluji stav zařízení…", style = MaterialTheme.typography.bodyMedium)
                }
            }

            false -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium)
                        .padding(12.dp)
                ) {
                    Text("Zařízení není aktivní", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Text(
                        "Než bude možné pracovat s hlášeními, požádejte administrátora o aktivaci. Poté klepněte na Kontrolovat stav.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                refreshActivationAndData(
                                    deviceRepo,
                                    reportsRepo,
                                    category,
                                    onActivated = { activated = it },
                                    onItems = { items = it },
                                    onLoading = { loading = it },
                                    onError = { error = it }
                                )
                            }
                        }
                    ) {
                        Text("Kontrolovat stav")
                    }
                }
            }

            true -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                refreshActivationAndData(
                                    deviceRepo,
                                    reportsRepo,
                                    category,
                                    onActivated = { activated = it },
                                    onItems = { items = it },
                                    onLoading = { loading = it },
                                    onError = { error = it }
                                )
                            }
                        }
                    ) {
                        Text("Obnovit")
                    }
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.padding(4.dp), strokeWidth = 2.dp)
                        Text("Načítám hlášení…", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Create button (parita s webem: hlášení + fotky + offline fronta)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = {
                        createType = category
                        showCreate = true
                    }) {
                        Text("Nové hlášení")
                    }
                    Text(
                        "Max ${BuildConfig.MAX_PHOTOS_PER_REPORT} fotek",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                if (!loading && error == null) {
                    if (items.isEmpty()) {
                        Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        items.forEach { item ->
                            ReportCard(
                                item = item,
                                baseUrl = cz.hcasc.hotel.config.BuildConfigExt.BASE_URL,
                                onDone = {
                                    scope.launch {
                                        actionLoading = true
                                        runCatching { reportsRepo.markDone(item.id) }
                                        refreshActivationAndData(
                                            deviceRepo,
                                            reportsRepo,
                                            category,
                                            onActivated = { activated = it },
                                            onItems = { items = it },
                                            onLoading = { loading = it },
                                            onError = { error = it }
                                        )
                                        actionLoading = false
                                    }
                                },
                                actionLoading = actionLoading
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateReportSheet(
            title = title,
            createType = createType,
            allowTypeSwitch = (title == "Pokojská"),
            room = createRoom,
            description = createDesc,
            photoUris = createPhotos,
            onRoomChange = { createRoom = it },
            onDescriptionChange = { createDesc = it.take(BuildConfig.MAX_DESCRIPTION_LEN) },
            onTypeChange = { createType = it },
            onAddFromGallery = { pickImages.launch("image/*") },
            onAddFromCamera = {
                val uri = createTempPhotoUri(context)
                cameraUri = uri
                takePicture.launch(uri)
            },
            onRemovePhoto = { u -> createPhotos = createPhotos.filterNot { it == u } },
            onDismiss = { showCreate = false },
            onSubmit = {
                val roomInt = createRoom.trim().toIntOrNull()
                if (roomInt == null || roomInt <= 0) {
                    error = "Zadejte číslo pokoje."
                    return@CreateReportSheet
                }
                if (createPhotos.isEmpty()) {
                    error = "Přidejte alespoň 1 fotografii."
                    return@CreateReportSheet
                }

                scope.launch(Dispatchers.IO) {
                    runCatching {
                        val id = queueRepo.enqueueReport(
                            type = createType.name,
                            room = roomInt,
                            description = createDesc.trim().takeIf { it.isNotBlank() },
                            photoUris = createPhotos
                        )
                        runCatching { queueRepo.sendOne(id) }
                        SendQueueWorker.triggerOneOff(context)
                    }
                }

                createRoom = ""
                createDesc = ""
                createPhotos = emptyList()
                showCreate = false

                scope.launch {
                    refreshActivationAndData(
                        deviceRepo,
                        reportsRepo,
                        category,
                        onActivated = { activated = it },
                        onItems = { items = it },
                        onLoading = { loading = it },
                        onError = { error = it }
                    )
                }
            }
        )
    }
}

@Composable
private fun ReportCard(
    item: ReportItem,
    baseUrl: String,
    onDone: () -> Unit,
    actionLoading: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pokoj ${item.room}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(item.createdAtHuman, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!item.description.isNullOrBlank()) {
                Text(item.description ?: "", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("Bez popisu", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            if (item.thumbnailUrls.isNotEmpty()) {
                PhotoStrip(
                    thumbnailUrls = item.thumbnailUrls,
                    baseUrl = baseUrl
                )
            }
            Divider()
            Button(onClick = onDone, enabled = !actionLoading) {
                Text(if (actionLoading) "Označuji…" else "Označit vyřízené")
            }
        }
    }
}

@Composable
private fun PhotoStrip(thumbnailUrls: List<String>, baseUrl: String) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        thumbnailUrls.take(5).forEach { u ->
            val resolved = resolveUrl(baseUrl, u)
            AsyncImage(
                model = resolved,
                contentDescription = "foto",
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small),
            )
        }
        if (thumbnailUrls.size > 5) {
            Text(
                "+${thumbnailUrls.size - 5}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun reportsRepo(context: Context): ReportsRepo {
    val app = context.applicationContext as App
    return app.reportsRepo
}

private suspend fun refreshActivationAndData(
    deviceRepo: DeviceRepo,
    reportsRepo: ReportsRepo,
    category: ReportCategory,
    onActivated: (Boolean) -> Unit,
    onItems: (List<ReportItem>) -> Unit,
    onLoading: (Boolean) -> Unit,
    onError: (String?) -> Unit
) {
    onLoading(true)
    onError(null)
    // Nejprve načti aktuální stav ze serveru (může se změnit mezi spuštěními).
    runCatching { deviceRepo.refreshStateBestEffort() }
    val activation = deviceRepo.getActivationSnapshot()
    onActivated(activation.isActivated)
    if (!activation.isActivated) {
        onError("Zařízení není aktivní. Ověřte aktivaci v administraci a klepněte na Kontrolovat stav.")
        onLoading(false)
        return
    }

    val data = runCatching { reportsRepo.listOpen(category) }
    if (data.isSuccess) {
        onItems(data.getOrDefault(emptyList()))
        onLoading(false)
        return
    }

    val failure = data.exceptionOrNull()
    if (failure is HttpException && (failure.code() == 401 || failure.code() == 403)) {
        deviceRepo.markDeactivatedFromServer()
        onActivated(false)
        onError("Zařízení není aktivní. Ověřte aktivaci v administraci a klepněte na Kontrolovat stav.")
    } else {
        onError("Nepodařilo se načíst hlášení. Zkontrolujte připojení a aktivaci zařízení.")
    }
    onLoading(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateReportSheet(
    title: String,
    createType: ReportCategory,
    allowTypeSwitch: Boolean,
    room: String,
    description: String,
    photoUris: List<Uri>,
    onRoomChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTypeChange: (ReportCategory) -> Unit,
    onAddFromGallery: () -> Unit,
    onAddFromCamera: () -> Unit,
    onRemovePhoto: (Uri) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Nové hlášení – $title", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            if (allowTypeSwitch) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onTypeChange(ReportCategory.FIND) }, enabled = createType != ReportCategory.FIND) {
                        Text("Nález")
                    }
                    OutlinedButton(onClick = { onTypeChange(ReportCategory.ISSUE) }, enabled = createType != ReportCategory.ISSUE) {
                        Text("Závada")
                    }
                }
            }

            OutlinedTextField(
                value = room,
                onValueChange = { v -> onRoomChange(v.filter { it.isDigit() }.take(4)) },
                label = { Text("Číslo pokoje") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Popis (volitelné)") },
                supportingText = { Text("${description.length}/${BuildConfig.MAX_DESCRIPTION_LEN}") },
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Fotografie (1–${BuildConfig.MAX_PHOTOS_PER_REPORT})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onAddFromCamera, enabled = photoUris.size < BuildConfig.MAX_PHOTOS_PER_REPORT) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Foto")
                    }
                    OutlinedButton(onClick = onAddFromGallery, enabled = photoUris.size < BuildConfig.MAX_PHOTOS_PER_REPORT) {
                        Icon(Icons.Filled.Collections, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Galerie")
                    }
                }

                if (photoUris.isNotEmpty()) {
                    val scroll = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scroll),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        photoUris.forEach { u ->
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small)
                                    .clickable { onRemovePhoto(u) },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(model = u, contentDescription = "foto", modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                    Text("Klepnutím na fotku ji odeberete.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Zrušit") }
                Button(onClick = onSubmit, modifier = Modifier.weight(1f)) { Text("Uložit a odeslat") }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

private fun resolveUrl(baseUrl: String, maybeRelative: String): String {
    val s = maybeRelative.trim()
    if (s.startsWith("http://") || s.startsWith("https://")) return s
    val b = baseUrl.trimEnd('/')
    return if (s.startsWith("/")) "$b$s" else "$b/$s"
}

private fun createTempPhotoUri(context: Context): Uri {
    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    val name = "report_${sdf.format(Date())}.jpg"
    val dir = File(context.cacheDir, "captured").apply { mkdirs() }
    val file = File(dir, name)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
