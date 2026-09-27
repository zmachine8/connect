package ee.ut.connect.ui.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import ee.ut.connect.data.repository.FirestoreChatRepository
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayOutputStream
import java.io.File

private data class SelectedDocument(val name: String, val mimeType: String, val bytes: ByteArray)

@Composable
fun ChatRoute(userId: String, displayName: String, onBack: () -> Unit) {
    val viewModel: ChatViewModel = viewModel(key = userId) { ChatViewModel(userId, FirestoreChatRepository()) }
    val context = LocalContext.current
    var captured by remember { mutableStateOf<Bitmap?>(null) }
    var pendingFile by remember { mutableStateOf<File?>(null) }
    var selectedDocument by remember { mutableStateOf<SelectedDocument?>(null) }
    fun shareLocation() {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = when {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> { viewModel.showError("Turn on device location and try again"); return }
        }
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                manager.getCurrentLocation(provider, null as CancellationSignal?, context.mainExecutor) { location ->
                    if (location == null) viewModel.showError("Could not get current location")
                    else viewModel.sendLocation(location.latitude, location.longitude)
                }
            } else {
                @Suppress("DEPRECATION")
                val location = manager.getLastKnownLocation(provider)
                if (location == null) viewModel.showError("Could not get current location")
                else viewModel.sendLocation(location.latitude, location.longitude)
            }
        } catch (_: SecurityException) { viewModel.showError("Location permission is required") }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) shareLocation() else viewModel.showError("Location permission was denied")
    }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: "Document"
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (output.size() <= 200_000) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                if (bytes == null || bytes.isEmpty() || bytes.size > 200_000)
                    viewModel.showError("Choose a document smaller than 200 KB")
                else {
                    captured = null
                    pendingFile?.delete()
                    pendingFile = null
                    selectedDocument = SelectedDocument(name.take(120), mimeType.take(100), bytes)
                }
            } catch (_: Exception) { viewModel.showError("Could not read the selected document") }
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            captured = pendingFile?.let {
                BitmapFactory.decodeFile(it.absolutePath, BitmapFactory.Options().apply { inSampleSize = 2 })
            }
        } else {
            captured = null
            pendingFile?.delete()
            pendingFile = null
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            var sampleSize = 1
            while (maxOf(options.outWidth, options.outHeight) / sampleSize > 1280) sampleSize *= 2
            captured = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
            }
            pendingFile?.delete()
            pendingFile = null
        }
    }
    ChatScreen(displayName, viewModel.uiState, onBack, viewModel::updateDraft,
        onCapture = {
            captured = null
            selectedDocument = null
            pendingFile?.delete()
            val directory = File(context.cacheDir, "photos").apply { mkdirs() }
            val file = File.createTempFile("connect-", ".jpg", directory)
            pendingFile = file
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            camera.launch(uri)
        },
        onPickPhoto = { selectedDocument = null; gallery.launch("image/*") },
        onPickDocument = { documentPicker.launch(arrayOf("*/*")) },
        onShareLocation = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                shareLocation()
            else locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        },
        photoPreview = captured,
        documentName = selectedDocument?.name,
        onCancelPhoto = { captured = null; pendingFile?.delete(); pendingFile = null },
        onCancelDocument = { selectedDocument = null },
        onSend = {
            val bitmap = captured
            val document = selectedDocument
            if (bitmap == null && document != null) {
                viewModel.sendDocument(document.name, document.mimeType, document.bytes) { success ->
                    if (success) selectedDocument = null
                }
            } else if (bitmap == null) {
                viewModel.send()
            } else {
                val scale = minOf(1f, 640f / maxOf(bitmap.width, bitmap.height))
                val small = Bitmap.createScaledBitmap(bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1), true)
                var bytes = ByteArray(0)
                for (quality in listOf(65, 45, 30)) {
                    bytes = ByteArrayOutputStream().also { small.compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
                    if (bytes.size <= 200_000) break
                }
                if (bytes.size > 200_000) {
                    val smaller = Bitmap.createScaledBitmap(small,
                        (small.width * 3 / 4).coerceAtLeast(1), (small.height * 3 / 4).coerceAtLeast(1), true)
                    bytes = ByteArrayOutputStream().also { smaller.compress(Bitmap.CompressFormat.JPEG, 30, it) }.toByteArray()
                }
                viewModel.sendPhoto(bytes) { success ->
                    if (success) { captured = null; pendingFile?.delete(); pendingFile = null }
                }
            }
        },
    )
}

@Composable
fun ChatScreen(
    displayName: String,
    state: ChatUiState,
    onBack: () -> Unit,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
    onCapture: () -> Unit,
    onPickPhoto: () -> Unit,
    onPickDocument: () -> Unit,
    onShareLocation: () -> Unit,
    photoPreview: Bitmap?,
    documentName: String?,
    onCancelPhoto: () -> Unit,
    onCancelDocument: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    var menuExpanded by remember { mutableStateOf(false) }
    val lastMessageId = state.messages.lastOrNull()?.id
    LaunchedEffect(lastMessageId) {
        if (lastMessageId != null) listState.animateScrollToItem(state.messages.lastIndex)
    }
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Text(displayName, style = MaterialTheme.typography.titleLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 12.dp, top = 12.dp))
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.messages, key = { it.id }) { message ->
                val mine = message.senderId == FirebaseAuth.getInstance().currentUser?.uid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (mine) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        if (message.documentBytes != null) {
                            Column(Modifier.padding(12.dp)) {
                                Text("📄 ${message.documentName ?: "Document"}")
                                if (message.text.isNotBlank()) Text(message.text)
                                TextButton(onClick = {
                                    val file = File(context.cacheDir, "photos/${message.id}").apply {
                                        parentFile?.mkdirs(); writeBytes(message.documentBytes)
                                    }
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, message.documentMimeType ?: "application/octet-stream")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    try { context.startActivity(intent) } catch (_: Exception) { }
                                }) { Text("Open") }
                            }
                        } else if (message.latitude != null && message.longitude != null) {
                            TextButton(onClick = {
                                val geo = Uri.parse("geo:${message.latitude},${message.longitude}?q=${message.latitude},${message.longitude}")
                                try { context.startActivity(Intent(Intent.ACTION_VIEW, geo)) } catch (_: Exception) { }
                            }) { Text("📍 View location") }
                        } else if (message.imageBytes == null) {
                            Text(message.text, modifier = Modifier.padding(12.dp))
                        } else {
                            val bytes = message.imageBytes
                            val bitmap = remember(message.id) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                            Column {
                                if (bitmap != null) Image(bitmap.asImageBitmap(), contentDescription = "Chat photo",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.widthIn(max = 240.dp).heightIn(max = 320.dp)
                                        .size(
                                            (240f * bitmap.width / maxOf(bitmap.width, bitmap.height)).dp,
                                            (240f * bitmap.height / maxOf(bitmap.width, bitmap.height)).dp,
                                        ))
                                else Text("Could not display photo", modifier = Modifier.padding(12.dp))
                                if (message.text.isNotBlank()) Text(message.text, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                }
            }
        }
        if (state.loading) CircularProgressIndicator()
        if (state.error != null) Text(state.error, color = MaterialTheme.colorScheme.error)
        if (state.uploading) Text("Uploading photo…")
        if (state.messages.isEmpty()) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("No messages yet") }
        }
        if (photoPreview != null) Row(verticalAlignment = Alignment.Top) {
            Image(photoPreview.asImageBitmap(), contentDescription = "Selected photo", modifier = Modifier.size(80.dp))
            IconButton(onClick = onCancelPhoto, enabled = !state.uploading) {
                Text("×", style = MaterialTheme.typography.titleLarge)
            }
        }
        if (documentName != null) Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📄 $documentName", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = onCancelDocument, enabled = !state.uploading) { Text("×") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth()) {
            Box {
                IconButton(onClick = { menuExpanded = true }, enabled = !state.loading && !state.uploading) {
                    Text("+", style = MaterialTheme.typography.headlineMedium)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Photos") }, onClick = { menuExpanded = false; onPickPhoto() })
                    DropdownMenuItem(text = { Text("Camera") }, onClick = { menuExpanded = false; onCapture() })
                    DropdownMenuItem(text = { Text("Location") }, onClick = { menuExpanded = false; onShareLocation() })
                    DropdownMenuItem(text = { Text("Document") }, onClick = { menuExpanded = false; onPickDocument() })
                }
            }
            OutlinedTextField(
                value = state.draft,
                onValueChange = onDraftChanged,
                placeholder = { Text(if (photoPreview == null && documentName == null) "Message" else "Add a caption") },
                maxLines = 4,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            )
            Button(onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                onSend()
            }, enabled = !state.loading && !state.sending && !state.uploading && (state.draft.isNotBlank() || photoPreview != null || documentName != null)) { Text("Send") }
        }
    }
}
