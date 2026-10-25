package com.angell3d.app

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.File

val Cyan = Color(0xFF00E5FF)
val Bg = Color(0xFF0B1220)
val Panel = Color(0xFF131C2E)
val Panel2 = Color(0xFF1B2740)

@Composable
fun AngellTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Cyan,
            onPrimary = Color.Black,
            secondary = Color(0xFFFF8F00),
            background = Bg,
            onBackground = Color.White,
            surface = Panel,
            onSurface = Color.White,
            surfaceVariant = Panel2,
            onSurfaceVariant = Color(0xFFB0BEC5)
        ),
        content = content
    )
}

@Composable
fun Header(title: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) { Text("← Atrás") }
        Text(title, style = MaterialTheme.typography.titleLarge, color = Cyan)
    }
}

/** Copia un archivo elegido por el usuario dentro de la carpeta privada de la app. Devuelve (ruta, nombre). */
fun copyToApp(ctx: Context, uri: Uri, folder: String): Pair<String, String>? {
    return try {
        var name = "archivo"
        ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (i >= 0 && c.moveToFirst()) name = c.getString(i) ?: name
        }
        val dir = File(ctx.filesDir, folder).apply { mkdirs() }
        val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val out = File(dir, newId() + "_" + safe)
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { input.copyTo(it) }
        }
        Pair(out.absolutePath, name)
    } catch (e: Exception) {
        null
    }
}

/** Carga una imagen reducida (ligera para celulares de gama baja). */
fun loadBitmap(path: String, maxDim: Int): ImageBitmap? {
    if (path.isBlank()) return null
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var s = 1
        while (bounds.outWidth / s > maxDim || bounds.outHeight / s > maxDim) s *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = s }
        BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
    } catch (e: Throwable) {
        null
    }
}

@Composable
fun rememberBitmap(path: String, maxDim: Int = 512): ImageBitmap? {
    return remember(path) { loadBitmap(path, maxDim) }
}

@Composable
fun Thumb(path: String, size: Dp, label: String) {
    val bmp = rememberBitmap(path, 200)
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF263859)),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(label.take(1).uppercase(), color = Cyan, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Picker(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(label + ": " + (options.firstOrNull { it.first == selected }?.second ?: "—"))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.second) },
                    onClick = { onSelect(opt.first); open = false }
                )
            }
        }
    }
}
