package com.explorer.launcher

import android.content.Context
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.LinearLayout
import kotlin.math.abs

/** Hafif sayfa kaydırıcı (ViewPager yok, kütüphane yok). */
class Pager(ctx: Context) : FrameLayout(ctx) {
    private val strip = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
    var page = 0
        private set
    var count = 1
        private set
    var onPage: ((Int) -> Unit)? = null
    var onSized: (() -> Unit)? = null
    var onEdgeRight: (() -> Unit)? = null
    private var downX = 0f
    private var downY = 0f
    private var drag = false
    private var vt: VelocityTracker? = null
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop

    init { addView(strip, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)) }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { super.onSizeChanged(w, h, ow, oh); onSized?.invoke() }

    fun setPages(pages: List<View>) {
        strip.removeAllViews()
        for (p in pages) strip.addView(p, LinearLayout.LayoutParams(width, height))
        strip.layoutParams = LayoutParams(width * pages.size, LayoutParams.MATCH_PARENT)
        count = pages.size; page = page.coerceIn(0, count - 1)
        strip.translationX = -page * width.toFloat()
        onPage?.invoke(page)
    }

    fun goPage(p: Int) {
        page = p.coerceIn(0, count - 1)
        strip.animate().translationX(-page * width.toFloat()).setDuration(200).start()
        onPage?.invoke(page)
    }

    private fun start(e: MotionEvent) {
        downX = e.x; downY = e.y; drag = false
        vt?.recycle(); vt = VelocityTracker.obtain(); vt!!.addMovement(e)
        strip.animate().cancel()
    }

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> start(e)
            MotionEvent.ACTION_MOVE -> {
                vt?.addMovement(e)
                if (!drag && abs(e.x - downX) > slop && abs(e.x - downX) > abs(e.y - downY)) { drag = true; parent?.requestDisallowInterceptTouchEvent(true) }
            }
        }
        return drag
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> start(e)
            MotionEvent.ACTION_MOVE -> {
                vt?.addMovement(e)
                val dx = e.x - downX
                var t = -page * width + dx
                if ((page == 0 && dx > 0) || (page == count - 1 && dx < 0)) t = -page * width + dx * 0.3f
                strip.translationX = t
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                vt?.addMovement(e); vt?.computeCurrentVelocity(1000)
                val vx = vt?.xVelocity ?: 0f; val dx = e.x - downX
                var np = page
                if (dx < -width / 5f || vx < -700f) np = page + 1 else if (dx > width / 5f || vx > 700f) np = page - 1
                vt?.recycle(); vt = null; drag = false
                if (np < 0) { goPage(0); if (e.actionMasked == MotionEvent.ACTION_UP) onEdgeRight?.invoke() } else goPage(np)
            }
        }
        return true
    }
}
