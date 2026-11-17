package com.explorer.launcher

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.View

object Gfx {
    fun blend(a: Int, b: Int, t: Float): Int = Color.rgb(
        (Color.red(a) * (1 - t) + Color.red(b) * t).toInt(),
        (Color.green(a) * (1 - t) + Color.green(b) * t).toInt(),
        (Color.blue(a) * (1 - t) + Color.blue(b) * t).toInt())

    /** Varsayılan duvar kağıdı: vurgu rengine göre çizilir, dosya gerekmez. */
    fun defaultWall(acc: Int): Bitmap {
        val w = 960; val h = 480
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565); val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG); val dk = 0xFF0A0D07.toInt()
        p.color = blend(dk, acc, 0.10f); c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.color = blend(dk, acc, 0.40f); c.drawCircle(w * 0.78f, h * 0.30f, h * 0.11f, p)
        fun hill(col: Int, y0: Float, y1: Float, y2: Float) {
            val pa = Path(); pa.moveTo(0f, h * y0); pa.lineTo(w * 0.28f, h * y1); pa.lineTo(w * 0.5f, h * y2)
            pa.lineTo(w * 0.75f, h * y1); pa.lineTo(w.toFloat(), h * y0); pa.lineTo(w.toFloat(), h.toFloat()); pa.lineTo(0f, h.toFloat()); pa.close()
            p.color = col; c.drawPath(pa, p)
        }
        hill(blend(dk, acc, 0.07f), 0.72f, 0.50f, 0.66f)
        hill(blend(dk, acc, 0.03f), 0.88f, 0.74f, 0.84f)
        return b
    }

    /** Bir kez küçük boyutta bulanıklaştırır; çizimde büyütülür. Canlı blur yok, telefonu yormaz. */
    fun blurSmall(src: Bitmap): Bitmap {
        val w = 80; val h = (src.height * w / src.width).coerceAtLeast(8)
        val s = Bitmap.createScaledBitmap(src, w, h, true)
        val px = IntArray(w * h); s.getPixels(px, 0, w, 0, 0, w, h)
        repeat(3) { boxBlur(px, w, h, 3) }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); out.setPixels(px, 0, w, 0, 0, w, h)
        return out
    }

    private fun boxBlur(px: IntArray, w: Int, h: Int, r: Int) {
        val tmp = IntArray(px.size)
        for (y in 0 until h) for (x in 0 until w) {
            var rr = 0; var g = 0; var bb = 0; var n = 0
            for (k in -r..r) { val c = px[y * w + (x + k).coerceIn(0, w - 1)]; rr += (c shr 16) and 255; g += (c shr 8) and 255; bb += c and 255; n++ }
            tmp[y * w + x] = Color.rgb(rr / n, g / n, bb / n)
        }
        for (y in 0 until h) for (x in 0 until w) {
            var rr = 0; var g = 0; var bb = 0; var n = 0
            for (k in -r..r) { val c = tmp[(y + k).coerceIn(0, h - 1) * w + x]; rr += (c shr 16) and 255; g += (c shr 8) and 255; bb += c and 255; n++ }
            px[y * w + x] = Color.rgb(rr / n, g / n, bb / n)
        }
    }
}

/** Buzlu cam: bir kez hazırlanıp saklanır (cache). Çizim sırasında tek drawBitmap, canlı blur yok. */
class GlassDrawable(private val host: View, private val root: View, private val bmp: Bitmap?,
                    private val tint: Int, private val radii: FloatArray) : Drawable() {
    private val bp = Paint(Paint.FILTER_BITMAP_FLAG)
    private val fp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val m = Matrix()
    private val a = IntArray(2); private val b = IntArray(2)
    private var cache: Bitmap? = null
    private var cx = Int.MIN_VALUE; private var cy = 0; private var cw = 0; private var ch = 0; private var rw = 0; private var rh = 0

    private fun build(w: Int, h: Int, x: Int, y: Int) {
        cache?.recycle()
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val path = Path(); path.addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radii, Path.Direction.CW)
        c.clipPath(path)
        if (bmp != null) {
            val rW = root.width.toFloat(); val rH = root.height.toFloat()
            val s = maxOf(rW / bmp.width, rH / bmp.height)
            m.setScale(s, s); m.postTranslate((rW - bmp.width * s) / 2f, (rH - bmp.height * s) / 2f)
            m.postTranslate(-x.toFloat(), -y.toFloat())
            c.drawBitmap(bmp, m, bp)
        }
        fp.color = tint; c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), fp)
        cache = out; cx = x; cy = y; cw = w; ch = h; rw = root.width; rh = root.height
    }

    override fun draw(c: Canvas) {
        val w = bounds.width(); val h = bounds.height()
        if (w <= 0 || h <= 0 || root.width <= 0) return
        host.getLocationInWindow(a); root.getLocationInWindow(b)
        val x = a[0] - b[0]; val y = a[1] - b[1]
        if (cache == null || x != cx || y != cy || w != cw || h != ch || rw != root.width || rh != root.height) build(w, h, x, y)
        c.drawBitmap(cache!!, bounds.left.toFloat(), bounds.top.toFloat(), null)
    }
    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(cf: ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}

/** Başlat düğmesi: 4 kare logo. */
class StartLogo(ctx: Context) : View(ctx) {
    var accent = Color.WHITE
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        p.color = accent
        val s = minOf(width, height) * 0.6f; val g = s * 0.1f; val q = (s - g) / 2f
        val x0 = (width - s) / 2f; val y0 = (height - s) / 2f
        for (i in 0..1) for (j in 0..1) {
            val l = x0 + i * (q + g); val t = y0 + j * (q + g)
            c.drawRoundRect(l, t, l + q, t + q, q * 0.15f, q * 0.15f, p)
        }
    }
}

/** Mac tarzı klasör simgesi: yarı saydam kare + içinde 2x2 küçük ikon. */
class FolderIcon(ctx: Context) : View(ctx) {
    var bitmaps: List<Bitmap> = emptyList()
    var tint = Color.GRAY
    private val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val r = RectF(); private val src = Rect(); private val dst = RectF()
    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        p.color = tint; r.set(0f, 0f, w, h); c.drawRoundRect(r, w * 0.24f, w * 0.24f, p)
        val pad = w * 0.12f; val g = w * 0.06f; val s = (w - 2 * pad - g) / 2f
        for ((i, b) in bitmaps.withIndex()) {
            if (i > 3) break
            val l = pad + (i % 2) * (s + g); val t = pad + (i / 2) * (s + g)
            src.set(0, 0, b.width, b.height); dst.set(l, t, l + s, t + s); p.color = Color.WHITE
            c.drawBitmap(b, src, dst, p)
        }
    }
}
