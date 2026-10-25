package com.angell3d.app

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

val TRACK_KINDS = listOf(
    Pair("personaje", "Personaje"),
    Pair("escenario", "Escenario"),
    Pair("camara", "Cámara"),
    Pair("audio", "Audio (música / efectos)"),
    Pair("titulo", "Títulos 2D / 3D"),
    Pair("relacion", "Relaciones")
)

fun trackColor(kind: String): Color = when (kind) {
    "camara" -> Color(0xFFAB47BC)
    "escenario" -> Color(0xFF43A047)
    "personaje" -> Color(0xFF00ACC1)
    "audio" -> Color(0xFFFB8C00)
    "titulo" -> Color(0xFFFDD835)
    else -> Color(0xFFEC407A)
}

fun f1(x: Float): String = String.format("%.1f", x)

private const val PPS = 40f // dp por segundo
private val ROW_H = 48.dp

@Composable
fun TimelineScreen(onBack: () -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }
    var addFor by remember { mutableStateOf<Track?>(null) }
    var addTrack by remember { mutableStateOf(false) }
    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()
    val total = max(30f, (Store.blocks.maxOfOrNull { it.start + it.length } ?: 0f) + 10f)

    Column(Modifier.fillMaxSize()) {
        Header("Línea de tiempo", onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(vScroll)
        ) {
            Row {
                Column(Modifier.width(96.dp)) {
                    Box(Modifier.height(24.dp))
                    Store.tracks.forEach { t ->
                        Row(
                            Modifier
                                .height(ROW_H)
                                .fillMaxWidth()
                                .padding(end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { addFor = t },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                            ) {
                                Text(
                                    "+ " + t.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 12.sp,
                                    color = trackColor(t.kind)
                                )
                            }
                        }
                    }
                }
                Column(Modifier.horizontalScroll(hScroll)) {
                    Box(
                        Modifier
                            .width((total * PPS).dp)
                            .height(24.dp)
                    ) {
                        var s = 0
                        while (s <= total.toInt()) {
                            Text(
                                "${s}s",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.offset(x = (s * PPS).dp, y = 4.dp)
                            )
                            s += 5
                        }
                    }
                    Store.tracks.forEach { t ->
                        Box(
                            Modifier
                                .width((total * PPS).dp)
                                .height(ROW_H)
                                .background(if (Store.tracks.indexOf(t) % 2 == 0) Panel else Bg)
                        ) {
                            Store.blocks.filter { it.trackId == t.id }.forEach { b ->
                                BlockView(b, t.kind, selected == b.id) { selected = b.id }
                            }
                        }
                    }
                }
            }
        }

        val sel = Store.blocks.firstOrNull { it.id == selected }
        if (sel != null) {
            val kind = Store.tracks.firstOrNull { it.id == sel.trackId }?.kind ?: "personaje"
            BlockEditor(sel, kind) { selected = null }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { addTrack = true }) { Text("+ Pista") }
                Text(
                    "Toca “+ nombre de pista” para agregar bloques. Arrastra un bloque para moverlo.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    addFor?.let { t -> AddBlockDialog(t, { selected = it; addFor = null }) { addFor = null } }
    if (addTrack) AddTrackDialog { addTrack = false }
}

@Composable
fun BlockView(b: Block, kind: String, selected: Boolean, onSelect: () -> Unit) {
    val id = b.id
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier
            .offset(x = (b.start * PPS).dp, y = 4.dp)
            .width(max(b.length * PPS, 28f).dp)
            .height(40.dp)
            .background(trackColor(kind), shape)
            .border(if (selected) 2.dp else 0.dp, Color.White, shape)
            .pointerInput(id) { detectTapGestures(onTap = { onSelect() }) }
            .pointerInput(id) {
                detectDragGestures(onDragStart = { onSelect() }) { change, drag ->
                    change.consume()
                    val dSec = drag.x / density / PPS
                    Store.blocks.update({ it.id == id }) { it.copy(start = max(0f, it.start + dSec)) }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            b.label,
            color = Color.Black,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun BlockEditor(sel: Block, kind: String, onClose: () -> Unit) {
    val id = sel.id
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            try { player?.release() } catch (e: Exception) { }
        }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel2),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(
                value = sel.label,
                onValueChange = { v -> Store.blocks.update({ it.id == id }) { it.copy(label = v) } },
                label = { Text("Nombre del bloque") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Inicio ${f1(sel.start)} s", Modifier.width(110.dp), fontSize = 13.sp)
                TextButton(onClick = {
                    Store.blocks.update({ it.id == id }) { it.copy(start = max(0f, it.start - 0.5f)) }
                }) { Text("−0.5") }
                TextButton(onClick = {
                    Store.blocks.update({ it.id == id }) { it.copy(start = it.start + 0.5f) }
                }) { Text("+0.5") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Duración ${f1(sel.length)} s", Modifier.width(110.dp), fontSize = 13.sp)
                TextButton(onClick = {
                    Store.blocks.update({ it.id == id }) { it.copy(length = max(0.5f, it.length - 0.5f)) }
                }) { Text("−0.5") }
                TextButton(onClick = {
                    Store.blocks.update({ it.id == id }) { it.copy(length = it.length + 0.5f) }
                }) { Text("+0.5") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (kind == "titulo") {
                    OutlinedButton(onClick = {
                        Store.blocks.update({ it.id == id }) {
                            it.copy(extra = if (it.extra == "3D") "2D" else "3D")
                        }
                    }) { Text("Título: " + (if (sel.extra == "3D") "3D" else "2D")) }
                }
                if (kind == "audio" && sel.extra.isNotBlank()) {
                    OutlinedButton(onClick = {
                        try {
                            player?.release()
                            val mp = MediaPlayer()
                            mp.setDataSource(sel.extra)
                            mp.prepare()
                            mp.start()
                            player = mp
                        } catch (e: Exception) {
                        }
                    }) { Text("▶ Probar") }
                    OutlinedButton(onClick = {
                        try { player?.stop() } catch (e: Exception) { }
                    }) { Text("■") }
                }
                OutlinedButton(onClick = {
                    Store.blocks.add(sel.copy(id = newId(), start = sel.start + sel.length))
                }) { Text("Duplicar") }
                OutlinedButton(onClick = {
                    Store.blocks.removeAll { it.id == id }
                    onClose()
                }) { Text("Borrar") }
                Button(onClick = onClose) { Text("Listo") }
            }
        }
    }
}

@Composable
fun AddBlockDialog(track: Track, onAdded: (String) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val startAt = Store.blocks.filter { it.trackId == track.id }.maxOfOrNull { it.start + it.length } ?: 0f
    var label by remember { mutableStateOf("") }
    var relId by remember { mutableStateOf(Store.relations.firstOrNull()?.id ?: "") }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val res = copyToApp(ctx, uri, "audio")
            if (res != null) {
                var secs = 5f
                try {
                    val mr = MediaMetadataRetriever()
                    mr.setDataSource(res.first)
                    val ms = mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toFloatOrNull()
                    if (ms != null) secs = ms / 1000f
                    mr.release()
                } catch (e: Exception) {
                }
                Store.assets.add(Asset(name = res.second, path = res.first, kind = "audio"))
                val b = Block(trackId = track.id, start = startAt, length = secs, label = res.second, extra = res.first)
                Store.blocks.add(b)
                onAdded(b.id)
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(track.name) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (track.kind) {
                    "audio" -> {
                        Text("Importa música, voces o efectos de sonido desde tu teléfono.")
                        Button(onClick = { audioPicker.launch("audio/*") }, modifier = Modifier.fillMaxWidth()) {
                            Text("🎵 Importar audio")
                        }
                    }
                    "relacion" -> {
                        if (Store.relations.isEmpty()) {
                            Text("Primero crea relaciones en el Mapa de la historia.")
                        } else {
                            Picker(
                                "Relación",
                                Store.relations.map { Pair(it.id, relText(it)) },
                                relId
                            ) { relId = it }
                        }
                    }
                    else -> {
                        OutlinedTextField(
                            value = label, onValueChange = { label = it },
                            label = {
                                Text(
                                    when (track.kind) {
                                        "titulo" -> "Texto del título"
                                        "camara" -> "Movimiento (ej. zoom, paneo)"
                                        "escenario" -> "Escenario o animación"
                                        else -> "Animación (ej. caminar)"
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (track.kind != "audio") {
                TextButton(onClick = {
                    val text = if (track.kind == "relacion") {
                        Store.relations.firstOrNull { it.id == relId }?.let { relText(it) } ?: ""
                    } else label.ifBlank { track.name }
                    if (text.isNotBlank()) {
                        val b = Block(
                            trackId = track.id, start = startAt, length = 3f, label = text,
                            extra = if (track.kind == "titulo") "2D" else ""
                        )
                        Store.blocks.add(b)
                        onAdded(b.id)
                    }
                }) { Text("Agregar") }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    Store.blocks.removeAll { it.trackId == track.id }
                    Store.tracks.removeAll { it.id == track.id }
                    onDismiss()
                }) { Text("Borrar pista") }
                TextButton(onClick = onDismiss) { Text("Cerrar") }
            }
        }
    )
}

@Composable
fun AddTrackDialog(onDismiss: () -> Unit) {
    var kind by remember { mutableStateOf("personaje") }
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva pista") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Picker("Tipo", TRACK_KINDS, kind) { kind = it }
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nombre (ej. Personaje 2)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val n = name.ifBlank { TRACK_KINDS.first { it.first == kind }.second }
                Store.tracks.add(Track(kind = kind, name = n))
                onDismiss()
            }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
