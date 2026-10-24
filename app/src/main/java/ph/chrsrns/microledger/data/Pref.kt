package ph.chrsrns.microledger.data

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import org.json.JSONArray

// One declaration per preference: key + default + typed read/write/
// LiveData. PreferencesDataSource members delegate to these; key
// literals and defaults are not restated elsewhere (V336/V363).
class Pref<T>(
    val key: String,
    val default: T,
    private val read: SharedPreferences.(String, T) -> T,
    private val write: SharedPreferences.(String, T) -> Unit,
    private val live: SharedPreferences.(String, T) -> LiveData<T>,
) {
    fun read(prefs: SharedPreferences): T = prefs.read(key, default)

    fun write(
        prefs: SharedPreferences,
        value: T,
    ) = prefs.write(key, value)

    fun liveData(prefs: SharedPreferences): LiveData<T> = prefs.live(key, default)
}

internal fun stringPref(
    key: String,
    default: String,
) = Pref(
    key = key,
    default = default,
    read = { k, d -> getString(k, d)!! },
    write = { k, v -> edit { putString(k, v) } },
    live = { k, d -> stringLiveData(k, d).map { it!! } },
)

internal fun booleanPref(
    key: String,
    default: Boolean,
) = Pref(
    key = key,
    default = default,
    read = { k, d -> getBoolean(k, d) },
    write = { k, v -> edit { putBoolean(k, v) } },
    live = { k, d -> booleanLiveData(k, d) },
)

internal fun intPref(
    key: String,
    default: Int,
) = Pref(
    key = key,
    default = default,
    read = { k, d -> getInt(k, d) },
    write = { k, v -> edit { putInt(k, v) } },
    live = { k, d -> intLiveData(k, d) },
)

internal fun stringListPref(
    key: String,
    defaultItems: List<String>,
) = Pref(
    key = key,
    default = defaultItems,
    read = { k, d -> parseJsonStringList(getString(k, serializeStringList(d))!!) },
    write = { k, v -> edit { putString(k, serializeStringList(v)) } },
    live = { k, d -> stringLiveData(k, serializeStringList(d)).map { parseJsonStringList(it!!) } },
)

internal fun parseJsonStringList(json: String): List<String> {
    val array = JSONArray(json)
    return (0 until array.length()).map { array.getString(it) }
}

internal fun serializeStringList(list: List<String>): String {
    val array = JSONArray()
    list.forEach { array.put(it) }
    return array.toString()
}
