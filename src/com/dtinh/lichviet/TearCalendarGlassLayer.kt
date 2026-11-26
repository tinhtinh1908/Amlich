package com.dtinh.lichviet

import android.graphics.Canvas as AndroidCanvas
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.layerBackdrop

/** Draws glass beneath the existing tear-calendar views and their touch targets. */
@RequiresApi(33)
internal class TearCalendarGlassLayer(
    private val calendar: MinimalCalendarView,
    paper: View,
    today: View,
    settings: View,
    weekday: View,
    note: View,
    hours: Array<View>,
) : FrameLayout(calendar.context) {
    private data class Region(val view: View, val corner: Float? = null, val sheet: Boolean = false)
    private data class Tile(
        val region: Int,
        val x: Int, val y: Int, val w: Int, val h: Int,
        val pressed: Boolean,
    )

    private val regions = buildList {
        add(Region(paper, MiuixMetrics.CARD_RADIUS, sheet = true))
        add(Region(today))
        add(Region(settings))
        add(Region(weekday))
        add(Region(note))
        for (hour in hours) add(Region(hour, 6f))
    }
    private var tiles by mutableStateOf(emptyList<Tile>())
    private var surface by mutableIntStateOf(0)
    private var opacity by mutableIntStateOf(ThemeManager.DEFAULT_GLASS_OPACITY)
    private var sceneRevision by mutableIntStateOf(0)
    private var observer: ViewTreeObserver? = null
    private var boundsDirty = true
    private var lastPressState = -1
    private val onLayoutChange = ViewTreeObserver.OnGlobalLayoutListener { boundsDirty = true }
    private val updateBounds = ViewTreeObserver.OnPreDrawListener {
        if (isShown) {
            var pressState = 0
            regions.forEachIndexed { index, region ->
                if (region.view.isPressed) pressState = pressState or (1 shl index)
            }
            if (!boundsDirty && pressState == lastPressState) return@OnPreDrawListener true
            boundsDirty = false
            lastPressState = pressState
            tiles = buildList {
                regions.forEachIndexed { index, region ->
                    val view = region.view
                    if (view.isShown && view.width > 0 && view.height > 0) {
                        add(Tile(index, view.left, view.top, view.width, view.height, view.isPressed))
                    }
                }
            }
        } else {
            if (tiles.isNotEmpty()) tiles = emptyList()
            boundsDirty = true
        }
        true
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        addView(ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { Glass() }
        }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        boundsDirty = true
        observer = viewTreeObserver.also {
            it.addOnPreDrawListener(updateBounds)
            it.addOnGlobalLayoutListener(onLayoutChange)
        }
    }

    override fun onDetachedFromWindow() {
        observer?.takeIf { it.isAlive }?.apply {
            removeOnPreDrawListener(updateBounds)
            removeOnGlobalLayoutListener(onLayoutChange)
        }
        observer = null
        super.onDetachedFromWindow()
    }

    fun updateOpacity(value: Int) {
        opacity = value
    }

    fun updateBackdrop(surface: Int) {
        this.surface = surface
        sceneRevision++
    }

    private fun drawBackdrop(canvas: AndroidCanvas, revision: Int) {
        if (revision >= 0 && canvas.isHardwareAccelerated) {
            calendar.drawCalendarBackdrop(canvas, false)
        }
    }

    @Composable
    private fun Glass() {
        val density = LocalDensity.current
        val source = rememberCalendarGlassBackdrop { drawBackdrop(drawContext.canvas.nativeCanvas, sceneRevision) }
        val control = remember(density.density) { measuredTile(false, density.density) }
        val tint = Color(surface)
        val panel = remember(control) { calendarPanelStyle(control) }
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize().layerBackdrop(source)) {}
            for (tile in tiles) key(tile.region) {
                val region = regions[tile.region]
                val radius = region.corner?.dp ?: with(density) { (minOf(tile.w, tile.h) * 0.5f).toDp() }
                val width = with(density) { tile.w.toDp() }
                val height = with(density) { tile.h.toDp() }
                CalendarGlassSurface(
                    modifier = Modifier.offset { IntOffset(tile.x, tile.y) }.requiredSize(width, height),
                    backdrop = source,
                    pressed = tile.pressed,
                    opacity = opacity,
                    frost = tint,
                    radius = radius,
                    styleOverride = if (region.sheet) panel else control,
                    roundedRectangle = region.corner != null,
                    transparencyRange = if (region.sheet) 0.42f else 0.78f,
                )
            }
        }
    }
}
