package com.angell3d.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LibraryScreen(onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Header("Biblioteca", onBack)
        TabRow(selectedTabIndex = tab) {
            listOf("Personajes", "Guiones", "Notas").forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
            }
        }
        when (tab) {
            0 -> CharactersTab()
            1 -> TextsTab("guion")
            else -> TextsTab("nota")
        }
    }
}

@Composable
fun CharactersTab() {
    var editing by remember { mutableStateOf<Character?>(null) }
    var creating by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        if (Store.characters.isEmpty()) {
            Text(
                "Aún no hay personajes.\nToca + para crear la ficha del primero.",
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
            items(Store.characters, key = { it.id }) { c ->
                Card(
                    onClick = { editing = c },
                    colors = CardDefaults.cardColors(containerColor = Panel2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Thumb(c.photo, 56.dp, c.name)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(c.name.ifBlank { "(sin nombre)" }, fontWeight = FontWeight.Bold, color = Cyan)
                            if (c.desc.isNotBlank()) {
                                Text(c.desc, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (c.sketch.isNotBlank()) {
                                Text("✏️ Con boceto", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
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
        CharacterEditor(
            initial = Character(),
            onSave = { Store.characters.add(it); creating = false },
            onDelete = null,
            onDismiss = { creating = false }
        )
    }
    editing?.let { c ->
        CharacterEditor(
            initial = c,
            onSave = { n -> Store.characters.update({ it.id == c.id }) { n }; editing = null },
            onDelete = {
                Store.characters.removeAll { it.id == c.id }
                Store.relations.removeAll { it.a == c.id || it.b == c.id }
                editing = null
            },
            onDismiss = { editing = null }
        )
    }
}

@Composable
fun CharacterEditor(
    initial: Character,
    onSave: (Character) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(initial.name) }
    var desc by remember { mutableStateOf(initial.desc) }
    var photo by remember { mutableStateOf(initial.photo) }
    var sketch by remember { mutableStateOf(initial.sketch) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) copyToApp(ctx, uri, "img")?.let { photo = it.first }
    }
    val sketchPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) copyToApp(ctx, uri, "img")?.let { sketch = it.first }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ficha de personaje") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Thumb(photo, 72.dp, name)
                    Column(Modifier.padding(start = 12.dp)) {
                        OutlinedButton(onClick = { photoPicker.launch("image/*") }) { Text("📷 Foto / cara") }
                    }
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nombre") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc, onValueChange = { desc = it },
                    label = { Text("Descripción / personalidad") }, minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = { sketchPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (sketch.isBlank()) "✏️ Agregar boceto" else "✏️ Cambiar boceto")
                }
                val sk = rememberBitmap(sketch, 600)
                if (sk != null) {
                    androidx.compose.foundation.Image(
                        bitmap = sk,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .size(200.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(initial.copy(name = name.trim(), desc = desc, photo = photo, sketch = sketch))
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
fun TextsTab(kind: String) {
    var editing by remember { mutableStateOf<TextItem?>(null) }
    var creating by remember { mutableStateOf(false) }
    val list = Store.texts.filter { it.kind == kind }
    val noun = if (kind == "guion") "guion" else "nota"
    Box(Modifier.fillMaxSize()) {
        if (list.isEmpty()) {
            Text(
                "Aún no hay ${noun}s.\nToca + para escribir el primero.",
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
            items(list, key = { it.id }) { t ->
                Card(
                    onClick = { editing = t },
                    colors = CardDefaults.cardColors(containerColor = Panel2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(t.title.ifBlank { "(sin título)" }, fontWeight = FontWeight.Bold, color = Cyan)
                        Text(t.text, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        TextEditor(
            initial = TextItem(kind = kind),
            onSave = { Store.texts.add(it); creating = false },
            onDelete = null,
            onDismiss = { creating = false }
        )
    }
    editing?.let { t ->
        TextEditor(
            initial = t,
            onSave = { n -> Store.texts.update({ it.id == t.id }) { n }; editing = null },
            onDelete = { Store.texts.removeAll { it.id == t.id }; editing = null },
            onDismiss = { editing = null }
        )
    }
}

@Composable
fun TextEditor(
    initial: TextItem,
    onSave: (TextItem) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var text by remember { mutableStateOf(initial.text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.kind == "guion") "Guion" else "Nota") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Título") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text("Texto") }, minLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(initial.copy(title = title.trim(), text = text)) }) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Borrar") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}
