package com.angell3d.app

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString().take(8)

data class Character(
    val id: String = newId(),
    val name: String = "",
    val desc: String = "",
    val photo: String = "",
    val sketch: String = ""
)

data class TextItem(
    val id: String = newId(),
    val kind: String = "guion",
    val title: String = "",
    val text: String = ""
)

data class Scene(
    val id: String = newId(),
    val title: String = "",
    val summary: String = ""
)

data class Relation(
    val id: String = newId(),
    val a: String = "",
    val b: String = "",
    val type: String = "amigos",
    val sceneId: String = ""
)

data class Track(
    val id: String = newId(),
    val kind: String = "personaje",
    val name: String = ""
)

data class Block(
    val id: String = newId(),
    val trackId: String = "",
    val start: Float = 0f,
    val length: Float = 3f,
    val label: String = "",
    val extra: String = ""
)

data class Asset(
    val id: String = newId(),
    val name: String = "",
    val path: String = "",
    val kind: String = "audio"
)

data class PoseKey(val time: Float, val angles: List<Float>)

inline fun <T> MutableList<T>.update(pred: (T) -> Boolean, f: (T) -> T) {
    val i = indexOfFirst(pred)
    if (i >= 0) this[i] = f(this[i])
}

object Store {
    val characters = mutableStateListOf<Character>()
    val texts = mutableStateListOf<TextItem>()
    val scenes = mutableStateListOf<Scene>()
    val relations = mutableStateListOf<Relation>()
    val tracks = mutableStateListOf<Track>()
    val blocks = mutableStateListOf<Block>()
    val assets = mutableStateListOf<Asset>()
    val keys = mutableStateListOf<PoseKey>()

    private fun file(ctx: Context) = File(ctx.filesDir, "angell3d_proyecto.json")

    fun toJson(): String {
        val o = JSONObject()
        o.put("characters", JSONArray().also { a ->
            characters.forEach { c ->
                a.put(JSONObject().put("id", c.id).put("name", c.name).put("desc", c.desc)
                    .put("photo", c.photo).put("sketch", c.sketch))
            }
        })
        o.put("texts", JSONArray().also { a ->
            texts.forEach { t ->
                a.put(JSONObject().put("id", t.id).put("kind", t.kind).put("title", t.title).put("text", t.text))
            }
        })
        o.put("scenes", JSONArray().also { a ->
            scenes.forEach { s ->
                a.put(JSONObject().put("id", s.id).put("title", s.title).put("summary", s.summary))
            }
        })
        o.put("relations", JSONArray().also { a ->
            relations.forEach { r ->
                a.put(JSONObject().put("id", r.id).put("a", r.a).put("b", r.b)
                    .put("type", r.type).put("sceneId", r.sceneId))
            }
        })
        o.put("tracks", JSONArray().also { a ->
            tracks.forEach { t ->
                a.put(JSONObject().put("id", t.id).put("kind", t.kind).put("name", t.name))
            }
        })
        o.put("blocks", JSONArray().also { a ->
            blocks.forEach { b ->
                a.put(JSONObject().put("id", b.id).put("trackId", b.trackId)
                    .put("start", b.start.toDouble()).put("length", b.length.toDouble())
                    .put("label", b.label).put("extra", b.extra))
            }
        })
        o.put("assets", JSONArray().also { a ->
            assets.forEach { s ->
                a.put(JSONObject().put("id", s.id).put("name", s.name).put("path", s.path).put("kind", s.kind))
            }
        })
        o.put("keys", JSONArray().also { a ->
            keys.forEach { k ->
                val arr = JSONArray()
                k.angles.forEach { arr.put(it.toDouble()) }
                a.put(JSONObject().put("time", k.time.toDouble()).put("angles", arr))
            }
        })
        return o.toString()
    }

    private fun JSONObject.list(name: String): List<JSONObject> {
        val a = optJSONArray(name) ?: return emptyList()
        return (0 until a.length()).map { a.getJSONObject(it) }
    }

    fun load(ctx: Context) {
        val f = file(ctx)
        var loaded = false
        if (f.exists()) {
            try {
                val o = JSONObject(f.readText())
                characters.clear(); texts.clear(); scenes.clear(); relations.clear()
                tracks.clear(); blocks.clear(); assets.clear(); keys.clear()
                o.list("characters").forEach {
                    characters.add(Character(it.getString("id"), it.optString("name"), it.optString("desc"),
                        it.optString("photo"), it.optString("sketch")))
                }
                o.list("texts").forEach {
                    texts.add(TextItem(it.getString("id"), it.optString("kind"), it.optString("title"), it.optString("text")))
                }
                o.list("scenes").forEach {
                    scenes.add(Scene(it.getString("id"), it.optString("title"), it.optString("summary")))
                }
                o.list("relations").forEach {
                    relations.add(Relation(it.getString("id"), it.optString("a"), it.optString("b"),
                        it.optString("type"), it.optString("sceneId")))
                }
                o.list("tracks").forEach {
                    tracks.add(Track(it.getString("id"), it.optString("kind"), it.optString("name")))
                }
                o.list("blocks").forEach {
                    blocks.add(Block(it.getString("id"), it.optString("trackId"),
                        it.optDouble("start", 0.0).toFloat(), it.optDouble("length", 3.0).toFloat(),
                        it.optString("label"), it.optString("extra")))
                }
                o.list("assets").forEach {
                    assets.add(Asset(it.getString("id"), it.optString("name"), it.optString("path"), it.optString("kind")))
                }
                o.list("keys").forEach {
                    val arr = it.getJSONArray("angles")
                    keys.add(PoseKey(it.optDouble("time", 0.0).toFloat(),
                        (0 until arr.length()).map { i -> arr.getDouble(i).toFloat() }))
                }
                loaded = true
            } catch (e: Exception) {
                loaded = false
            }
        }
        if (!loaded || tracks.isEmpty()) {
            tracks.clear()
            tracks.add(Track(kind = "camara", name = "Cámara"))
            tracks.add(Track(kind = "escenario", name = "Escenario"))
            tracks.add(Track(kind = "personaje", name = "Personaje 1"))
            tracks.add(Track(kind = "audio", name = "Audio"))
            tracks.add(Track(kind = "titulo", name = "Títulos"))
        }
    }

    fun write(ctx: Context, json: String) {
        try {
            val f = file(ctx)
            val tmp = File(ctx.filesDir, "angell3d_proyecto.tmp")
            tmp.writeText(json)
            tmp.copyTo(f, overwrite = true)
        } catch (e: Exception) {
        }
    }
}
