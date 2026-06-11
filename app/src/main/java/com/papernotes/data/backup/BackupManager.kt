package com.papernotes.data.backup

import android.content.Context
import android.net.Uri
import com.papernotes.data.repository.NoteRepository
import com.papernotes.domain.model.MoodCategory
import com.papernotes.domain.model.Note
import com.papernotes.domain.model.NoteType
import com.papernotes.domain.model.PaperStyle
import com.papernotes.util.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Sicherung & Wiederherstellung: exportiert alle (nicht gelöschten) Notizen samt Verknüpfungen
 * und angehängten Fotos in eine ZIP-Datei (`notes.json` + `photos/…`) und importiert sie wieder.
 * Import ist **nicht-destruktiv**: Notizen werden mit neuen IDs ergänzt, nichts wird gelöscht.
 */
object BackupManager {

    private const val VERSION = 1
    private const val NOTES_ENTRY = "notes.json"
    private const val PHOTO_DIR = "photos/"

    /** Schreibt die Sicherung nach [uri]. Gibt die Anzahl gesicherter Notizen zurück. */
    suspend fun export(context: Context, repository: NoteRepository, uri: Uri): Int =
        withContext(Dispatchers.IO) {
            val notes = repository.notesForBackup()
            val links = repository.allLinks()

            val root = JSONObject().apply {
                put("version", VERSION)
                put("notes", JSONArray().apply { notes.forEach { put(it.toJson()) } })
                put("links", JSONArray().apply {
                    links.forEach { put(JSONObject().put("aId", it.aId).put("bId", it.bId)) }
                })
            }

            context.contentResolver.openOutputStream(uri)?.use { out ->
                ZipOutputStream(out.buffered()).use { zip ->
                    zip.putNextEntry(ZipEntry(NOTES_ENTRY))
                    zip.write(root.toString().toByteArray(Charsets.UTF_8))
                    zip.closeEntry()

                    notes.mapNotNull { it.photoPath }.distinct().forEach { name ->
                        val file = PhotoStore.file(context, name)
                        if (file.exists()) {
                            zip.putNextEntry(ZipEntry(PHOTO_DIR + name))
                            file.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            } ?: error("Kein Schreibzugriff")
            notes.size
        }

    /** Liest die Sicherung aus [uri] und ergänzt die Notizen. Gibt die Anzahl importierter zurück. */
    suspend fun import(context: Context, repository: NoteRepository, uri: Uri): Int =
        withContext(Dispatchers.IO) {
            var notesJson: String? = null
            val photos = HashMap<String, ByteArray>()

            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input.buffered()).use { zip ->
                    var entry: ZipEntry? = zip.nextEntry
                    while (entry != null) {
                        when {
                            entry.name == NOTES_ENTRY -> notesJson = zip.readBytes().toString(Charsets.UTF_8)
                            entry.name.startsWith(PHOTO_DIR) && !entry.isDirectory ->
                                photos[entry.name.removePrefix(PHOTO_DIR)] = zip.readBytes()
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: error("Kein Lesezugriff")

            val root = JSONObject(notesJson ?: error("Keine gültige Sicherung"))
            val notesArr = root.getJSONArray("notes")

            // 1. Pass: Notizen einfügen, IDs neu vergeben, Fotos wiederherstellen.
            val idMap = HashMap<Long, Long>()
            val clipOf = HashMap<Long, Long>() // neueId -> alteClipId
            for (i in 0 until notesArr.length()) {
                val obj = notesArr.getJSONObject(i)
                val oldId = obj.getLong("id")
                var note = noteFromJson(obj)
                note.photoPath?.let { name ->
                    photos[name]?.let { bytes ->
                        val newName = "${UUID.randomUUID()}.jpg"
                        PhotoStore.file(context, newName).outputStream().use { it.write(bytes) }
                        note = note.copy(photoPath = newName)
                    } ?: run { note = note.copy(photoPath = null) }
                }
                val oldClip = if (obj.isNull("clipId")) null else obj.getLong("clipId")
                val newId = repository.insertRaw(note.copy(id = 0L, clipId = null))
                idMap[oldId] = newId
                if (oldClip != null) clipOf[newId] = oldClip
            }

            // 2. Pass: Stapel (clipId) und rote Fäden auf die neuen IDs umbiegen.
            clipOf.forEach { (newId, oldClip) ->
                idMap[oldClip]?.let { repository.setClip(newId, it) }
            }
            val linksArr = root.optJSONArray("links")
            if (linksArr != null) {
                for (i in 0 until linksArr.length()) {
                    val l = linksArr.getJSONObject(i)
                    val a = idMap[l.getLong("aId")]
                    val b = idMap[l.getLong("bId")]
                    if (a != null && b != null) repository.linkNotes(a, b)
                }
            }
            notesArr.length()
        }

    private fun Note.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("body", body)
        put("type", type.name)
        put("mood", mood.name)
        put("dogEarFolded", dogEarFolded)
        put("pinned", pinned)
        put("archived", archived)
        put("sealed", sealed)
        put("invisibleInk", invisibleInk)
        putOpt("deletedAt", deletedAt)
        putOpt("reminderAt", reminderAt)
        putOpt("expiresAt", expiresAt)
        put("backText", backText)
        putOpt("clipId", clipId)
        putOpt("countdownAt", countdownAt)
        putOpt("photoPath", photoPath)
        put("paper", paper.name)
        put("tags", tags)
        put("position", position)
        put("highlights", highlights)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    private fun noteFromJson(o: JSONObject): Note = Note(
        id = o.getLong("id"),
        title = o.optString("title"),
        body = o.optString("body"),
        type = NoteType.fromName(o.optString("type")),
        mood = MoodCategory.fromName(o.optString("mood")),
        dogEarFolded = o.optBoolean("dogEarFolded"),
        pinned = o.optBoolean("pinned"),
        archived = o.optBoolean("archived"),
        sealed = o.optBoolean("sealed"),
        invisibleInk = o.optBoolean("invisibleInk"),
        deletedAt = o.optLongOrNull("deletedAt"),
        reminderAt = o.optLongOrNull("reminderAt"),
        expiresAt = o.optLongOrNull("expiresAt"),
        backText = o.optString("backText"),
        clipId = o.optLongOrNull("clipId"),
        countdownAt = o.optLongOrNull("countdownAt"),
        photoPath = o.optStringOrNull("photoPath"),
        paper = PaperStyle.fromName(o.optString("paper")),
        tags = o.optString("tags"),
        position = o.optLong("position"),
        highlights = o.optString("highlights"),
        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
        updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
    )

    private fun JSONObject.putOpt(key: String, value: Any?) {
        if (value == null) put(key, JSONObject.NULL) else put(key, value)
    }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (!has(key) || isNull(key)) null else getLong(key)

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (!has(key) || isNull(key)) null else getString(key)
}
