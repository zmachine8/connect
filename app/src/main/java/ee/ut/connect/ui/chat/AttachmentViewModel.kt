package ee.ut.connect.ui.chat

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import java.io.File

internal class AttachmentViewModel(application: Application, private val savedState: SavedStateHandle) : AndroidViewModel(application) {
    private val directory = File(application.cacheDir, "draft-attachments").apply { mkdirs() }
    private fun savedFile(key: String) = savedState.get<String>(key)?.let(::File)?.takeIf { it.exists() }
    private var photoState by mutableStateOf(savedFile("photoPath")?.let { BitmapFactory.decodeFile(it.absolutePath) })
    private var documentState by mutableStateOf(savedFile("documentPath")?.let {
        SelectedDocument(savedState.get<String>("documentName") ?: "Document",
            savedState.get<String>("documentMime") ?: "application/octet-stream", it.readBytes())
    })
    var captured: Bitmap?
        get() = photoState
        set(value) {
            val file = value?.let { bitmap ->
                File.createTempFile("photo-", ".png", directory).also {
                    it.outputStream().use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
                }
            }
            savedFile("photoPath")?.delete()
            savedState["photoPath"] = file?.absolutePath
            photoState = value
        }
    var selectedDocument: SelectedDocument?
        get() = documentState
        set(value) {
            val file = value?.let { document ->
                File.createTempFile("document-", ".bin", directory).also { it.writeBytes(document.bytes) }
            }
            savedFile("documentPath")?.delete()
            savedState["documentPath"] = file?.absolutePath
            savedState["documentName"] = value?.name
            savedState["documentMime"] = value?.mimeType
            documentState = value
        }
    var pendingFile: File?
        get() = savedFile("cameraPath")
        set(value) { savedState["cameraPath"] = value?.absolutePath }

    override fun onCleared() {
        savedFile("photoPath")?.delete()
        savedFile("documentPath")?.delete()
        pendingFile?.delete()
        super.onCleared()
    }
}
