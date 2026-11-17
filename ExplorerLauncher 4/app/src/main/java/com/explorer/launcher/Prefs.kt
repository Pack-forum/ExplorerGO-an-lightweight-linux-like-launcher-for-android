package com.explorer.launcher

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import java.util.Calendar

object P {
    lateinit var sp: SharedPreferences
    fun init(c: Context) {
        sp = c.getSharedPreferences("explorer", 0)
        // Eski sürümde "desk" anahtarı hem ayar (Boolean) hem liste (String) olarak kullanılıyordu -> çökme. Taşı.
        val v = sp.all["desk"]
        if (v is String) sp.edit().putString("deskItems", v).remove("desk").apply()
    }
    fun i(k: String, d: Int): Int = try { sp.getInt(k, d) } catch (e: ClassCastException) { sp.edit().remove(k).apply(); d }
    fun b(k: String, d: Boolean): Boolean = try { sp.getBoolean(k, d) } catch (e: ClassCastException) { sp.edit().remove(k).apply(); d }
    fun s(k: String, d: String): String = try { sp.getString(k, d) ?: d } catch (e: ClassCastException) { sp.edit().remove(k).apply(); d }
    fun put(k: String, v: Any) {
        val e = sp.edit()
        when (v) { is Int -> e.putInt(k, v); is Boolean -> e.putBoolean(k, v); is String -> e.putString(k, v) }
        e.apply()
    }
    val accentNames = arrayOf("Mavi", "Turuncu", "Yeşil", "Kırmızı", "Mor", "Sarı", "Turkuaz", "Beyaz")
    private val accents = arrayOf("#00B0FF", "#FF6D00", "#00E676", "#FF1744", "#D500F9", "#FFD600", "#1DE9B6", "#FFFFFF")
    fun accent(): Int = Color.parseColor(accents[i("accent", 0).coerceIn(0, 7)])
    fun dark(): Boolean = when (i("theme", 0)) {
        0 -> true
        1 -> false
        else -> { val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY); h >= i("nightFrom", 19) || h < i("nightTo", 7) }
    }
    fun bg(): Int = if (dark()) 0xFF0B0E14.toInt() else 0xFFF1F3F7.toInt()
    fun fg(): Int = if (dark()) Color.WHITE else 0xFF10131A.toInt()
    fun surf(): Int = if (dark()) 0xFF1B2233.toInt() else Color.WHITE
}
