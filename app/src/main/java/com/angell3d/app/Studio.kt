package com.angell3d.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// ---------------------------------------------------------------- Esqueleto (huesos)

class Bone(
    val name: String,
    val parent: Int,
    val atEnd: Boolean,
    val ox: Float, val oy: Float, val oz: Float,
    val dx: Float, val dy: Float, val dz: Float,
    val len: Float
)

val BONES = listOf(
    Bone("Torso", -1, true, 0f, 0f, 0f, 0f, 1f, 0f, 0.9f),
    Bone("Cabeza", 0, true, 0f, 0.05f, 0f, 0f, 1f, 0f, 0.4f),
    Bone("Brazo izq", 0, true, -0.3f, -0.08f, 0f, 0f, -1f, 0f, 0.5f),
    Bone("Antebrazo izq", 2, true, 0f, 0f, 0f, 0f, -1f, 0f, 0.45f),
    Bone("Brazo der", 0, true, 0.3f, -0.08f, 0f, 0f, -1f, 0f, 0.5f),
    Bone("Antebrazo der", 4, true, 0f, 0f, 0f, 0f, -1f, 0f, 0.45f),
    Bone("Muslo izq", 0, false, -0.15f, 0f, 0f, 0f, -1f, 0f, 0.5f),
    Bone("Pierna izq", 6, true, 0f, 0f, 0f, 0f, -1f, 0f, 0.5f),
    Bone("Muslo der", 0, false, 0.15f, 0f, 0f, 0f, -1f, 0f, 0.5f),
    Bone("Pierna der", 8, true, 0f, 0f, 0f, 0f, -1f, 0f, 0.5f)
)

private val IDENTITY = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)

private fun mul(a: FloatArray, b: FloatArray): FloatArray {
    val r = FloatArray(9)
    for (i in 0..2) for (j in 0..2) {
        var s = 0f
        for (k in 0..2) s += a[i * 3 + k] * b[k * 3 + j]
        r[i * 3 + j] = s
    }
    return r
}

private fun rotX(deg: Float): FloatArray {
    val a = Math.toRadians(deg.toDouble())
    val c = cos(a).toFloat()
    val s = sin(a).toFloat()
    return floatArrayOf(1f, 0f, 0f, 0f, c, -s, 0f, s, c)
}

private fun rotZ(deg: Float): FloatArray {
    val a = Math.toRadians(deg.toDouble())
    val c = cos(a).toFloat()
    val s = sin(a).toFloat()
    return floatArrayOf(c, -s, 0f, s, c, 0f, 0f, 0f, 1f)
}

private fun applyM(m: FloatArray, v: FloatArray): FloatArray = floatArrayOf(
    m[0] * v[0] + m[1] * v[1] + m[2] * v[2],
    m[3] * v[0] + m[4] * v[1] + m[5] * v[2],
    m[6] * v[0] + m[7] * v[1] + m[8] * v[2]
)

/** Devuelve (inicio, fin) de cada hueso en 3D para los ángulos dados. */
fun solvePose(angles: List<Float>): List<Pair<FloatArray, FloatArray>> {
    val rots = ArrayList<FloatArray>()
    val res = ArrayList<Pair<FloatArray, FloatArray>>()
    for (i in BONES.indices) {
        val b = BONES[i]
        val ax = if (i * 2 < angles.size) angles[i * 2] else 0f
        val az = if (i * 2 + 1 < angles.size) angles[i * 2 + 1] else 0f
        val local = mul(rotZ(az), rotX(ax))
        val pr = if (b.parent < 0) IDENTITY else rots[b.parent]
        val r = mul(pr, local)
        val start: FloatArray = if (b.parent < 0) {
            floatArrayOf(0f, 1.0f, 0f)
        } else {
            val base = if (b.atEnd) res[b.parent].second else res[b.parent].first
            val off = applyM(pr, floatArrayOf(b.ox, b.oy, b.oz))
            floatArrayOf(base[0] + off[0], base[1] + off[1], base[2] + off[2])
        }
        val d = applyM(r, floatArrayOf(b.dx, b.dy, b.dz))
        val end = floatArrayOf(start[0] + d[0] * b.len, start[1] + d[1] * b.len, start[2] + d[2] * b.len)
        rots.add(r)
        res.add(Pair(start, end))
    }
    return res
}

fun evalKeys(t: Float): List<Float>? {
    val ks = Store.keys.sortedBy { it.time }
    if (ks.isEmpty()) return null
    if (t <= ks.first().time) return ks.first().angles
    if (t >= ks.last().time) return ks.last().angles
    for (i in 0 until ks.size - 1) {
        val a = ks[i]
        val b = ks[i + 1]
        if (t >= a.time && t <= b.time) {
            val f = if (b.time - a.time < 0.0001f) 0f else (t - a.time) / (b.time - a.time)
            return List(a.angles.size) { k -> a.angles[k] + (b.angles[k] - a.angles[k]) * f }
        }
    }
    return null
}

class RigState {
    val angles = mutableStateListOf<Float>().apply { repeat(BONES.size * 2) { add(0f) } }
    var t by mutableFloatStateOf(0f)
    var playing by mutableStateOf(false)
    var sel by mutableIntStateOf(0)
}

// ---------------------------------------------------------------- Pantalla

@Composable
fun StudioScreen(onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val rig = remember { RigState() }
    Column(Modifier.fillMaxSize()) {
        Header("Estudio 3D", onBack)
        TabRow(selectedTabIndex = tab) {
            listOf("Huesos 3D", "Cara", "Importar").forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
            }
        }
        when (tab) {
            0 -> RigTab(rig)
            1 -> FaceTab()
            else -> ImportTab()
        }
    }
}

@Composable
fun RigViewer(angles: List<Float>, sel: Int, modifier: Modifier) {
    var yaw by remember { mutableFloatStateOf(0.6f) }
    var pitch by remember { mutableFloatStateOf(0.25f) }
    var dist by remember { mutableFloatStateOf(5f) }
    Canvas(
        modifier.pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                yaw += pan.x * 0.01f
                pitch = (pitch + pan.y * 0.01f).coerceIn(-1.3f, 1.3f)
                dist = (dist / zoom).coerceIn(2.5f, 12f)
            }
        }
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val sc = min(w, h) * 1.4f

        fun proj(p: FloatArray): Offset? {
            val vx = p[0]
            val vy = p[1] - 1.1f
            val vz = p[2]
            val cyw = cos(yaw)
            val syw = sin(yaw)
            val x1 = vx * cyw + vz * syw
            val z1 = -vx * syw + vz * cyw
            val cp = cos(pitch)
            val sp = sin(pitch)
            val y2 = vy * cp - z1 * sp
            val z2 = vy * sp + z1 * cp
            val z = z2 + dist
            return if (z < 0.3f) null else Offset(cx + x1 / z * sc, cy - y2 / z * sc)
        }

        // Piso (escenario)
        val gridColor = Color(0x3300E5FF)
        for (i in -4..4) {
            val a1 = proj(floatArrayOf(i.toFloat(), 0f, -4f))
            val a2 = proj(floatArrayOf(i.toFloat(), 0f, 4f))
            if (a1 != null && a2 != null) drawLine(gridColor, a1, a2, strokeWidth = 2f)
            val b1 = proj(floatArrayOf(-4f, 0f, i.toFloat()))
            val b2 = proj(floatArrayOf(4f, 0f, i.toFloat()))
            if (b1 != null && b2 != null) drawLine(gridColor, b1, b2, strokeWidth = 2f)
        }

        // Personaje
        val pose = solvePose(angles)
        pose.forEachIndexed { i, pr ->
            val s = proj(pr.first)
            val e = proj(pr.second)
            if (s != null && e != null) {
                val col = if (i == sel) Color(0xFFFF8F00) else Cyan
                drawLine(col, s, e, strokeWidth = 14f, cap = StrokeCap.Round)
                drawCircle(Color.White, 8f, s)
                drawCircle(Color.White, 8f, e)
                if (i == 1) {
                    val mid = Offset((s.x + e.x) / 2f, (s.y + e.y) / 2f)
                    val rad = max(10f, (e - s).getDistance() / 2f)
                    drawCircle(col, rad, mid, style = Stroke(width = 6f))
                }
            }
        }
    }
}

@Composable
fun RigTab(st: RigState) {
    LaunchedEffect(st.playing) {
        if (st.playing) {
            val last = Store.keys.maxOfOrNull { it.time } ?: 0f
            if (Store.keys.size < 2) {
                st.playing = false
            } else {
                if (st.t >= last) st.t = 0f
                var prev = androidx.compose.runtime.withFrameNanos { it }
                while (st.playing && st.t < last) {
                    val now = androidx.compose.runtime.withFrameNanos { it }
                    st.t = (st.t + (now - prev) / 1_000_000_000f).coerceAtMost(last)
                    prev = now
                    evalKeys(st.t)?.let { r -> for (i in r.indices) st.angles[i] = r[i] }
                }
                st.playing = false
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        RigViewer(
            st.angles, st.sel,
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF070C16))
        )
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp)
                .verticalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            Text(
                "Un dedo gira la vista · pellizca para acercar",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                BONES.forEachIndexed { i, b ->
                    FilterChip(
                        selected = st.sel == i,
                        onClick = { st.sel = i },
                        label = { Text(b.name) },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
            val ix = st.sel * 2
            Text("Giro adelante / atrás: ${st.angles[ix].roundToInt()}°", fontSize = 13.sp)
            Slider(
                value = st.angles[ix],
                onValueChange = { st.angles[ix] = it },
                valueRange = -150f..150f
            )
            Text("Giro hacia los lados: ${st.angles[ix + 1].roundToInt()}°", fontSize = 13.sp)
            Slider(
                value = st.angles[ix + 1],
                onValueChange = { st.angles[ix + 1] = it },
                valueRange = -150f..150f
            )
            Text("Tiempo: ${f1(st.t)} s", fontSize = 13.sp)
            Slider(
                value = st.t,
                onValueChange = { v ->
                    st.t = v
                    evalKeys(v)?.let { r -> for (i in r.indices) st.angles[i] = r[i] }
                },
                valueRange = 0f..10f
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(onClick = {
                    val k = PoseKey(st.t, st.angles.toList())
                    val idx = Store.keys.indexOfFirst { abs(it.time - st.t) < 0.05f }
                    if (idx >= 0) Store.keys[idx] = k else Store.keys.add(k)
                }) { Text("💾 Guardar pose") }
                OutlinedButton(onClick = { st.playing = !st.playing }) {
                    Text(if (st.playing) "⏸ Pausa" else "▶ Reproducir")
                }
                OutlinedButton(onClick = {
                    for (i in st.angles.indices) st.angles[i] = 0f
                }) { Text("↺ Pose base") }
                OutlinedButton(onClick = { Store.keys.clear() }) { Text("Borrar poses") }
            }
            val times = Store.keys.sortedBy { it.time }.joinToString("  ") { f1(it.time) + "s" }
            Text(
                if (times.isBlank()) "Aún no hay poses guardadas. Mueve los huesos, elige un tiempo y toca “Guardar pose”."
                else "Poses guardadas en: $times",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------- Cara

@Composable
fun FaceTab() {
    var eye by remember { mutableFloatStateOf(1f) }
    var mouth by remember { mutableFloatStateOf(0f) }
    var smile by remember { mutableFloatStateOf(0f) }
    var brow by remember { mutableFloatStateOf(0f) }
    var charId by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val photo = Store.characters.firstOrNull { it.id == charId }?.photo ?: ""
    val bmp = rememberBitmap(photo, 800)

    fun preset(e: Float, m: Float, s: Float, b: Float) {
        eye = e; mouth = m; smile = s; brow = b
    }

    Column(Modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF070C16))
        ) {
            val r = min(size.width, size.height) * 0.42f
            val c = Offset(size.width / 2f, size.height / 2f)
            val facePath = Path().apply { addOval(Rect(c.x - r, c.y - r, c.x + r, c.y + r)) }
            if (bmp != null) {
                val side = min(bmp.width, bmp.height)
                clipPath(facePath) {
                    drawImage(
                        bmp,
                        srcOffset = IntOffset((bmp.width - side) / 2, (bmp.height - side) / 2),
                        srcSize = IntSize(side, side),
                        dstOffset = IntOffset((c.x - r).roundToInt(), (c.y - r).roundToInt()),
                        dstSize = IntSize((2 * r).roundToInt(), (2 * r).roundToInt())
                    )
                }
            } else {
                drawCircle(Color(0xFFFFCC80), r, c)
            }

            val ex = r * 0.38f
            val ey = c.y - r * 0.18f
            val ew = r * 0.16f
            val eh = r * 0.17f * eye + 2f
            for (sx in listOf(-1f, 1f)) {
                val ecx = c.x + sx * ex
                drawOval(Color.White, Offset(ecx - ew, ey - eh), Size(2 * ew, 2 * eh))
                drawCircle(Color(0xFF263238), min(ew, eh) * 0.8f, Offset(ecx, ey))
                val raise = max(0f, brow) * r * 0.10f
                val angry = max(0f, -brow) * r * 0.09f
                val by0 = ey - r * 0.30f - raise
                val outerX = ecx + sx * ew
                val innerX = ecx - sx * ew
                drawLine(
                    Color(0xFF4E342E),
                    Offset(outerX, by0 - angry * 0.4f),
                    Offset(innerX, by0 + angry),
                    strokeWidth = r * 0.05f, cap = StrokeCap.Round
                )
            }

            val my = c.y + r * 0.42f
            val mw = r * 0.34f
            val smileOff = smile * r * 0.16f
            val mh = mouth * r * 0.28f
            if (mh > 2f) {
                drawOval(Color(0xFF3E2723), Offset(c.x - mw, my - mh / 2f), Size(2 * mw, mh))
            }
            val cornerY = my - smileOff * 0.8f
            val ctrlY = my + smileOff * 1.6f
            val path = Path().apply {
                moveTo(c.x - mw, cornerY)
                quadraticBezierTo(c.x, ctrlY, c.x + mw, cornerY)
            }
            drawPath(path, Color(0xFF3E2723), style = Stroke(width = r * 0.04f, cap = StrokeCap.Round))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
                .verticalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            Picker(
                "Cara",
                listOf(Pair("", "Cara dibujada")) +
                    Store.characters.filter { it.photo.isNotBlank() }.map { Pair(it.id, it.name.ifBlank { "?" }) },
                charId
            ) { charId = it }
            Text("Ojos abiertos", fontSize = 13.sp)
            Slider(value = eye, onValueChange = { eye = it }, valueRange = 0f..1f)
            Text("Boca abierta", fontSize = 13.sp)
            Slider(value = mouth, onValueChange = { mouth = it }, valueRange = 0f..1f)
            Text("Sonrisa / tristeza", fontSize = 13.sp)
            Slider(value = smile, onValueChange = { smile = it }, valueRange = -1f..1f)
            Text("Cejas (enojo / sorpresa)", fontSize = 13.sp)
            Slider(value = brow, onValueChange = { brow = it }, valueRange = -1f..1f)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(onClick = { preset(1f, 0f, 0f, 0f) }) { Text("Neutral") }
                OutlinedButton(onClick = { preset(1f, 0.3f, 1f, 0.3f) }) { Text("Feliz") }
                OutlinedButton(onClick = { preset(0.7f, 0.1f, -0.7f, -1f) }) { Text("Enojado") }
                OutlinedButton(onClick = { preset(1f, 0.9f, 0f, 1f) }) { Text("Sorprendido") }
                OutlinedButton(onClick = {
                    scope.launch {
                        val old = eye
                        eye = 0f
                        delay(120)
                        eye = old
                    }
                }) { Text("Parpadear") }
            }
        }
    }
}

// ---------------------------------------------------------------- Importar

@Composable
fun ImportTab() {
    val ctx = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            copyToApp(ctx, uri, "models")?.let {
                Store.assets.add(Asset(name = it.second, path = it.first, kind = "modelo"))
            }
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Importar personajes y objetos 3D (GLB, glTF, FBX, OBJ)")
        Button(onClick = { picker.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
            Text("📦 Importar modelo 3D")
        }
        Text(
            "En esta versión el modelo se guarda dentro de tu proyecto. " +
                "El visor de modelos importados llegará en la siguiente versión.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Store.assets.filter { it.kind == "modelo" }.forEach { a ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(a.name, Modifier.weight(1f))
                TextButton(onClick = { Store.assets.removeAll { it.id == a.id } }) { Text("Quitar") }
            }
        }
    }
}
