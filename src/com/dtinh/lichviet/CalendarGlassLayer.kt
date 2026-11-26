package com.dtinh.lichviet

import android.content.Context
import android.graphics.Rect
import android.graphics.RuntimeShader
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.layerBackdrop

/** Decorative GPU layers behind the original View touch targets and the calendar lettering. */
@RequiresApi(33)
internal class CalendarGlassLayer(
    context: Context,
    private val calendar: CalendarTabsView,
    private val month: CalendarMonthView,
    private val yearCards: LinearLayout,
    today: View,
    settings: View,
    monthTab: View,
    yearTab: View,
    yearPicker: View,
) : FrameLayout(context) {
    private data class Tile(
        val id: Int, val x: Int, val y: Int, val w: Int, val h: Int,
        val pressed: Boolean = false,
        val panel: Boolean = false, val selected: Boolean = false,
    )

    private data class Tone(val backdrop: Int, val surface: Int)

    private val buttons = listOf(today, settings, monthTab, yearTab, yearPicker)
    private val grid = Rect()
    private val detail = Rect()
    private val note = Rect()
    private var tiles by mutableStateOf(emptyList<Tile>())
    private var tone by mutableStateOf(Tone(0, 0))
    private var sceneRevision by mutableIntStateOf(0)
    private var opacity by mutableIntStateOf(ThemeManager.DEFAULT_GLASS_OPACITY)
    private var showingMonth = true
    private var boundsDirty = true
    private var lastControlState = -1
    private val onLayoutChange = ViewTreeObserver.OnGlobalLayoutListener { boundsDirty = true }
    private val onScrollChange = ViewTreeObserver.OnScrollChangedListener { boundsDirty = true }
    private var boundsObserver: ViewTreeObserver? = null
    private val updateTiles = ViewTreeObserver.OnPreDrawListener {
        if (isShown) {
            var controlState = if (month.isNotePressed) 1 else 0
            buttons.forEachIndexed { index, button ->
                if (button.isPressed) controlState = controlState or (2 shl (index * 2))
                if (button.isSelected) controlState = controlState or (4 shl (index * 2))
            }
            if (!boundsDirty && controlState == lastControlState) return@OnPreDrawListener true
            boundsDirty = false
            lastControlState = controlState
            val root = parent as View
            tiles = buildList {
                if (showingMonth && month.isShown) {
                    month.copyGlassPanelBounds(grid, detail)
                    for ((index, bounds) in listOf(grid, detail).withIndex()) {
                        if (!bounds.isEmpty) {
                            add(Tile(buttons.size + index, bounds.left, bounds.top, bounds.width(), bounds.height(),
                                panel = true))
                        }
                    }
                    month.copyNoteBounds(note)
                    if (!note.isEmpty) {
                        add(Tile(buttons.size + 2, note.left, note.top, note.width(), note.height(),
                            pressed = month.isNotePressed))
                    }
                } else if (yearCards.isShown) {
                    var id = buttons.size + 3
                    for (rowIndex in 0 until yearCards.childCount) {
                        val row = yearCards.getChildAt(rowIndex) as LinearLayout
                        for (column in 0 until row.childCount) {
                            positionedTile(root, row.getChildAt(column), id++, panel = true)?.let(::add)
                        }
                    }
                }
                for ((i, button) in buttons.withIndex()) {
                    positionedTile(root, button, i, selected = button.isSelected)?.let(::add)
                }
            }
        } else {
            if (tiles.isNotEmpty()) tiles = emptyList()
            boundsDirty = true
        }
        true
    }

    private fun positionedTile(root: View, view: View, id: Int, panel: Boolean = false,
                               selected: Boolean = false): Tile? {
        if (!view.isShown || view.width == 0 || view.height == 0) return null
        var x = view.left
        var y = view.top
        var ancestor = view.parent
        while (ancestor is View && ancestor !== root) {
            x += ancestor.left - ancestor.scrollX
            y += ancestor.top - ancestor.scrollY
            ancestor = ancestor.parent
        }
        return Tile(id, x - left, y - top, view.width, view.height, view.isPressed, panel, selected)
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        addView(ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { Material() }
        }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        boundsDirty = true
        boundsObserver = viewTreeObserver.also {
            it.addOnPreDrawListener(updateTiles)
            it.addOnGlobalLayoutListener(onLayoutChange)
            it.addOnScrollChangedListener(onScrollChange)
        }
    }

    override fun onDetachedFromWindow() {
        boundsObserver?.takeIf { it.isAlive }?.apply {
            removeOnPreDrawListener(updateTiles)
            removeOnGlobalLayoutListener(onLayoutChange)
            removeOnScrollChangedListener(onScrollChange)
        }
        boundsObserver = null
        super.onDetachedFromWindow()
    }

    fun setMonthVisible(visible: Boolean) {
        if (showingMonth != visible) {
            showingMonth = visible
            boundsDirty = true
        }
    }

    fun updateOpacity(percent: Int) {
        opacity = percent
    }

    fun updatePalette(backdrop: Int, surface: Int) {
        val next = Tone(backdrop, surface)
        if (tone != next) tone = next
    }

    fun invalidateBackdrop() {
        sceneRevision++
    }

    private fun DrawScope.drawBackdrop(palette: Tone) {
        val revision = sceneRevision
        val canvas = drawContext.canvas.nativeCanvas
        if (revision >= 0 && size.width > 0f && size.height > 0f) {
            if (canvas.isHardwareAccelerated) {
                val saved = canvas.save()
                canvas.translate(-left.toFloat(), -top.toFloat())
                calendar.drawCalendarBackdrop(canvas, false)
                canvas.restoreToCount(saved)
            } else {
                drawRect(Color(palette.backdrop))
            }
        }
    }

    @Composable
    private fun Material() {
        val density = LocalDensity.current
        val palette = tone
        val drawScene: ContentDrawScope.() -> Unit = remember(palette.backdrop) { { drawBackdrop(palette) } }
        val source = rememberCalendarGlassBackdrop(drawScene)
        val off = remember(density.density) { measuredTile(false, density.density) }
        val on = remember(density.density) { measuredTile(true, density.density) }
        val panel = remember(off) { calendarPanelStyle(off) }
        val finishShaders = remember { mutableMapOf<Int, RuntimeShader>() }
        val yearCardsStart = buttons.size + 3
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize().layerBackdrop(source)) {}
            // All year cards sample the same already-blurred scene in one pass.
            if (tiles.any { it.id >= yearCardsStart }) {
                Box(
                    Modifier.fillMaxSize().graphicsLayer {
                        alpha = 1f - ((50 - opacity).coerceAtLeast(0) / 50f) * 0.42f
                    }.drawWithCache {
                        val path = Path()
                        val radius = CornerRadius(MiuixMetrics.CARD_RADIUS.dp.toPx())
                        tiles.forEach { tile ->
                            if (tile.id >= yearCardsStart) {
                                path.addRoundRect(RoundRect(ComposeRect(tile.x.toFloat(), tile.y.toFloat(),
                                    (tile.x + tile.w).toFloat(), (tile.y + tile.h).toFloat()), radius))
                            }
                        }
                        val frost = (opacity - 50).coerceAtLeast(0) / 50f * 0.68f
                        onDrawBehind {
                            clipPath(path) {
                                drawLayer(source.graphicsLayer)
                                drawRect(Color(0x38686c74))
                                if (frost > 0f) drawRect(Color(palette.surface).copy(alpha = frost))
                            }
                            drawPath(path, Color.White.copy(alpha = 0.20f), style = Stroke(0.75.dp.toPx()))
                        }
                    },
                )
            }
            for (tile in tiles) {
                if (tile.id >= yearCardsStart) continue
                key(tile.id) {
                    val style = if (tile.panel) panel else if (tile.selected) on else off
                    val frost = if (tile.selected) Color(0xffe3edf8) else Color(palette.surface)
                    val radius = if (tile.panel) {
                        MiuixMetrics.CARD_RADIUS.dp
                    } else {
                        with(density) { (minOf(tile.w, tile.h) * 0.5f).toDp() }
                    }
                    CalendarGlassSurface(
                        modifier = tileModifier(tile),
                        backdrop = source,
                        pressed = tile.pressed,
                        opacity = opacity,
                        frost = frost,
                        finishShader = finishShaders.getOrPut(tile.id) { newCalendarGlassShader() },
                        radius = radius,
                        styleOverride = style,
                        roundedRectangle = tile.panel,
                        transparencyRange = if (tile.panel || tile.selected) 0.42f else 0.78f,
                    )
                }
            }
        }
    }

    @Composable
    private fun tileModifier(tile: Tile): Modifier {
        val density = LocalDensity.current
        val width = with(density) { tile.w.toDp() }
        val height = with(density) { tile.h.toDp() }
        return Modifier.offset { IntOffset(tile.x, tile.y) }.requiredSize(width, height)
    }
}
