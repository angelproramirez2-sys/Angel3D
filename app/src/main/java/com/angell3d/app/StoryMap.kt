package com.angell3d.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

val RELATION_TYPES = listOf("amigos", "novios", "familia", "enemigos", "rivales", "compañeros", "mentor", "otro")

fun relColor(type: String): Color = when (type) {
    "amigos" -> Color(0xFF66BB6A)
    "novios" -> Color(0xFFF06292)
    "familia" -> Color(0xFFFFCA28)
    "enemigos" -> Color(0xFFEF5350)
    "rivales" -> Color(0xFFFF7043)
    "compañeros" -> Color(0xFF42A5F5)
    else -> Cyan
}

fun charName(id: String): String = Store.characters.firstOrNull { it.id == id }?.name?.ifBlank { "?" } ?: "?"

fun relText(r: Relation): String = "${charName(r.a)} y ${charName(r.b)}: ${r.type}"

@Composable
fun StoryScreen(onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Header("Mapa de la historia", onBack)
        TabRow(selectedTabIndex = tab) {
            listOf("Escenas", "Relaciones").forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
            }
        }
        if (tab == 0) ScenesTab() else RelationsTab()
    }
}

@Composable
fun ScenesTab() {
    var editing by remember { mutableStateOf<Scene?>(null) }
    var creating by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        if (Store.scenes.isEmpty()) {
            Text(
                "Aún no hay escenas.\nToca + para agregar la primera y acomódalas a tu gusto.",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(Store.scenes, key = { _, s -> s.id }) { i, s ->
                Card(colors = CardDefaults.cardColors(containerColor = Panel2), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Escena ${i + 1}: ${s.title.ifBlank { "(sin título)" }}", fontWeight = FontWeight.Bold, color = Cyan)
                        if (s.summary.isNotBlank()) {
                            Text(s.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Store.relations.filter { it.sceneId == s.id }.forEach { r ->
                            Text("💞 " + relText(r), color = relColor(r.type), fontSize = 13.sp)
                        }
                        Row {
                            TextButton(onClick = {
                                if (i > 0) Store.scenes.add(i - 1, Store.scenes.removeAt(i))
                            }) { Text("▲") }
                            TextButton(onClick = {
                                if (i < Store.scenes.size - 1) Store.scenes.add(i + 1, Store.scenes.removeAt(i))
                            }) { Text("▼") }
                            TextButton(onClick = { editing = s }) { Text("Editar") }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { creating = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) { Text("+") }
    }
    if (creating) {
        SceneEditor(Scene(), { Store.scenes.add(it); creating = false }, null) { creating = false }
    }
    editing?.let { s ->
        SceneEditor(
            s,
            { n -> Store.scenes.update({ it.id == s.id }) { n }; editing = null },
            {
                Store.scenes.removeAll { it.id == s.id }
                for (k in Store.relations.indices) {
                    if (Store.relations[k].sceneId == s.id) Store.relations[k] = Store.relations[k].copy(sceneId = "")
                }
                editing = null
            }
        ) { editing = null }
    }
}

@Composable
fun SceneEditor(initial: Scene, onSave: (Scene) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(initial.title) }
    var summary by remember { mutableStateOf(initial.summary) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Escena") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Título de la escena") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = summary, onValueChange = { summary = it },
                    label = { Text("Qué pasa en esta escena") }, minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(initial.copy(title = title.trim(), summary = summary)) }) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Borrar") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

@Composable
fun RelationsTab() {
    var editing by remember { mutableStateOf<Relation?>(null) }
    var creating by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (Store.characters.size < 2) {
                Text(
                    "Crea al menos 2 personajes en la Biblioteca para armar el árbol de relaciones.",
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                RelationGraph(
                    Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(Store.relations, key = { it.id }) { r ->
                    Card(
                        onClick = { editing = r },
                        colors = CardDefaults.cardColors(containerColor = Panel2),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(relText(r), color = relColor(r.type), fontWeight = FontWeight.Bold)
                            val sc = Store.scenes.indexOfFirst { it.id == r.sceneId }
                            Text(
                                if (sc >= 0) "Aparece en la escena ${sc + 1}" else "Sin escena asignada",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
        if (Store.characters.size >= 2) {
            FloatingActionButton(
                onClick = { creating = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) { Text("+") }
        }
    }
    if (creating) {
        RelationEditor(
            Relation(a = Store.characters[0].id, b = Store.characters[1].id),
            { Store.relations.add(it); creating = false }, null
        ) { creating = false }
    }
    editing?.let { r ->
        RelationEditor(
            r,
            { n -> Store.relations.update({ it.id == r.id }) { n }; editing = null },
            { Store.relations.removeAll { it.id == r.id }; editing = null }
        ) { editing = null }
    }
}

@Composable
fun RelationEditor(initial: Relation, onSave: (Relation) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var a by remember { mutableStateOf(initial.a) }
    var b by remember { mutableStateOf(initial.b) }
    var type by remember { mutableStateOf(initial.type) }
    var scene by remember { mutableStateOf(initial.sceneId) }
    val chars = Store.characters.map { Pair(it.id, it.name.ifBlank { "?" }) }
    val scenes = listOf(Pair("", "(ninguna)")) +
        Store.scenes.mapIndexed { i, s -> Pair(s.id, "Escena ${i + 1}: ${s.title}") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Relación") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Picker("Personaje A", chars, a) { a = it }
                Picker("Personaje B", chars, b) { b = it }
                Picker("Tipo", RELATION_TYPES.map { Pair(it, it) }, type) { type = it }
                Picker("Desde la escena", scenes, scene) { scene = it }
                Text(
                    "Si eliges una escena, la relación aparece en esa escena del mapa.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (a != b) onSave(initial.copy(a = a, b = b, type = type, sceneId = scene))
            }) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Borrar") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

@Composable
fun RelationGraph(modifier: Modifier) {
    val chars = Store.characters
    val rels = Store.relations
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val nodeR = with(density) { 30.dp.toPx() }
        val radius = min(wPx, hPx) / 2f - nodeR - with(density) { 14.dp.toPx() }
        val n = chars.size
        val pos = HashMap<String, Offset>()
        chars.forEachIndexed { i, c ->
            val ang = (2.0 * PI * i / n - PI / 2.0).toFloat()
            pos[c.id] = Offset(wPx / 2f + radius * cos(ang), hPx / 2f + radius * sin(ang))
        }
        Canvas(Modifier.fillMaxSize()) {
            rels.forEach { r ->
                val p1 = pos[r.a]
                val p2 = pos[r.b]
                if (p1 != null && p2 != null) drawLine(relColor(r.type), p1, p2, strokeWidth = 5f)
            }
        }
        val halfLabel = with(density) { 40.dp.roundToPx() }
        rels.forEach { r ->
            val p1 = pos[r.a]
            val p2 = pos[r.b]
            if (p1 != null && p2 != null) {
                val mx = ((p1.x + p2.x) / 2f).roundToInt()
                val my = ((p1.y + p2.y) / 2f).roundToInt()
                Text(
                    r.type,
                    color = relColor(r.type),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .offset { IntOffset(mx - halfLabel, my - 10) }
                        .width(80.dp)
                )
            }
        }
        chars.forEach { c ->
            val p = pos[c.id]
            if (p != null) {
                Column(
                    Modifier
                        .offset { IntOffset(p.x.roundToInt() - halfLabel, (p.y - nodeR).roundToInt()) }
                        .width(80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Thumb(c.photo, 60.dp, c.name)
                    Text(c.name, fontSize = 12.sp, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
