package com.dtinh.lichviet

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.glass.glass
import top.yukonga.miuix.kmp.material.*
import kotlin.math.PI
import kotlin.math.pow

/** Full-resolution capture of the unshaded scene, as in the validated demo. */
@Composable
@RequiresApi(33)
internal fun rememberCalendarGlassBackdrop(draw: ContentDrawScope.() -> Unit = { drawContent() }): LayerBackdrop {
    val density = LocalDensity.current.density
    val capture = rememberGraphicsLayer()
    val backdrop = rememberLayerBackdrop(graphicsLayer = capture, onDraw = draw)
    val blur = remember(density) {
        val radiusPx = GlassStyles.BionicMediumThin.blur.small * density / GlassDefaults.SourceDensity
        val sigma = radiusPx * 0.53f
        val androidRadius = ((sigma - 0.5f) / 0.57735f).coerceAtLeast(0.1f)
        RenderEffect.createBlurEffect(androidRadius, androidRadius, Shader.TileMode.CLAMP)
            .asComposeRenderEffect()
    }
    SideEffect { capture.renderEffect = blur }
    return backdrop
}

@Composable
@RequiresApi(33)
internal fun CalendarGlassSurface(
    modifier: Modifier,
    backdrop: Backdrop,
    pressed: Boolean,
    opacity: Int,
    frost: Color,
    finishShader: RuntimeShader? = null,
    radius: Dp = 33.dp,
    styleOverride: GlassStyle? = null,
    roundedRectangle: Boolean = false,
    transparencyRange: Float = 0.78f,
) {
    val density = LocalDensity.current.density
    val shape = remember(radius, roundedRectangle) {
        if (roundedRectangle) GlassShape(radius, smoothing = 0f) else GlassShape(radius)
    }
    val style = styleOverride ?: remember(density) { measuredTile(false, density) }
    val response = if (pressed) 0.20 else 0.35
    val press = animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = if (pressed) 1f else 0.95f,
            stiffness = (2 * PI / response).pow(2).toFloat(),
            visibilityThreshold = 0.002f,
        ),
        label = "Calendar glass press light",
    )
    val scale = animateFloatAsState(
        targetValue = if (pressed) 0.945f else 1f,
        animationSpec = spring(dampingRatio = 0.95f, stiffness = 340f, visibilityThreshold = 0.0005f),
        label = "Calendar glass size",
    )
    val shader = finishShader ?: remember { newCalendarGlassShader() }
    val adjustment = (opacity - 50) / 50f
    Box(
        modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            val halfWidth = size.width * 0.5f
            val halfHeight = size.height * 0.5f
            shader.setFloatUniform("center", halfWidth, halfHeight)
            shader.setFloatUniform("edgeRadius", minOf(halfWidth, halfHeight, radius.toPx()))
            shader.setFloatUniform("maskRadius", (halfWidth + halfHeight) * 0.5f + 180f)
            shader.setFloatUniform("progress", press.value.coerceIn(0f, 1f))
            shader.setFloatUniform("roundedRectangle", if (roundedRectangle) 1f else 0f)
            shader.setFloatUniform("coverageGain", 1f + adjustment.coerceAtMost(0f) * transparencyRange)
            shader.setFloatUniform("frostAmount", adjustment.coerceAtLeast(0f) * 0.68f)
            shader.setFloatUniform("frostColor", frost.red, frost.green, frost.blue)
            renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "material").asComposeRenderEffect()
        }.glass(backdrop = backdrop, shape = shape, style = style, noiseCoefficient = 0f),
    )
}

private val GlassNeutralTint = Color(0xff686c74)

internal fun calendarPanelStyle(base: GlassStyle): GlassStyle =
    base.copy(
        blend = base.blend.copy(darker = 0.17f),
        inner = base.inner.copy(tint = GlassNeutralTint, tintStrength = 0.16f,
            colorMix = 0.37f, colorWhite = 0.12f),
        refract = GlassRefract(3.1f),
    )

internal fun measuredTile(active: Boolean, density: Float): GlassStyle {
    // miuix-glass scales these geometry inputs using density / SourceDensity.
    val unit = GlassDefaults.SourceDensity / density
    return GlassStyle(
        blend = GlassBlend(
            curveA = if (active) 1.24f else 0.67f,
            curveB = if (active) 0.64f else 0.16f,
            curveC = if (active) 0.24f else 0.09f,
            curveD = 0f,
            amount = if (active) 0.32f else 0.24f,
            saturation = if (active) 2.6f else 1.4f,
            brightness = -0.02f,
            darker = if (active) 0.42f else 0.16f,
            darkerStart = 0.6f,
            darkerEnd = 1f,
        ),
        inner = GlassInner(
            bottom = if (active) 0.05f else 0.03f,
            tint = if (active) Color.White else GlassNeutralTint,
            tintStrength = if (active) 1.4f else 0.10f,
            colorWhite = if (active) 0.15f else 0.14f,
            colorMix = if (active) 0.90f else 0.30f,
            colorPow = if (active) 0.40f else 1f,
            alpha = 1f,
        ),
        edge = GlassEdge(72f * unit, 3.8f, 80f * unit, 800f * unit),
        reflect = GlassReflect(1.2f, 1f),
        light = GlassLight(-0.4f, 0.6f, -0.8f, if (active) 3f else 1.4f,
            if (active) 2f else 0.7f, 0.8f),
        refract = GlassRefract(4f),
        background = GlassBackground(2f, 0f, 0f, 0f),
        // Blur the full-resolution capture before the optical pass; do not downscale this tile.
        blur = GlassStyles.BionicMediumThin.blur.copy(small = 0f),
    )
}

@RequiresApi(33)
internal fun newCalendarGlassShader() = RuntimeShader(GLASS_FINISH)

private const val GLASS_FINISH = """
    uniform shader material;
    uniform float2 center;
    uniform float edgeRadius;
    uniform float maskRadius;
    uniform float progress;
    uniform float roundedRectangle;
    uniform float coverageGain;
    uniform float frostAmount;
    uniform float3 frostColor;

    // Area of a square pixel covered by the tangent to the circular silhouette.
    float pixelCoverage(float distance, float2 normal) {
        float major = max(abs(normal.x), abs(normal.y));
        float minor = min(abs(normal.x), abs(normal.y));
        float u = 0.5 * (major + minor) - distance;
        if (minor < 0.001) return clamp(u / major, 0.0, 1.0);
        if (u <= 0.0) return 0.0;
        if (u < minor) return u * u / (2.0 * major * minor);
        if (u <= major) return (u - 0.5 * minor) / major;
        float remaining = max(major + minor - u, 0.0);
        return 1.0 - remaining * remaining / (2.0 * major * minor);
    }

    half4 main(float2 p) {
        float2 fromCenter = p - center;
        float lengthPx = length(fromCenter);
        // The reference circle has no straight segment. Extend that circle
        // horizontally to fit the calendar label and keep its curved end caps.
        float straight = max(center.x - center.y, 0.0);
        float2 fromCap = fromCenter - float2(clamp(fromCenter.x, -straight, straight), 0.0);
        float capLength = straight > 0.0 ? length(fromCap) : lengthPx;
        float distance = capLength - edgeRadius;
        float2 cornerPosition = abs(fromCenter) - center + edgeRadius;
        float2 cornerOffset = max(cornerPosition, 0.0);
        float cornerLength = length(cornerOffset);
        if (roundedRectangle > 0.5) {
            distance = cornerLength + min(max(cornerPosition.x, cornerPosition.y), 0.0) - edgeRadius;
        }
        if (distance >= 0.71) return half4(0.0);

        half4 body = material.eval(p);
        if (distance > -9.0) {
            float2 normal;
            if (roundedRectangle > 0.5) {
                float2 axis = cornerPosition.x > cornerPosition.y ? float2(1, 0) : float2(0, 1);
                normal = sign(fromCenter) * (cornerLength > 0.001 ? cornerOffset / cornerLength : axis);
            } else {
                normal = fromCap / max(capLength, 0.001);
            }
            float coverage = pixelCoverage(distance, normal);
            float facing = abs(dot(normal, float2(0.7071, 0.7071)));
            float arc = pow(facing, 3.0);
            float taper = 1.0 - smoothstep(0.0, 9.0, -distance);
            float inset = max(distance + 1.0, 0.0);
            half4 nearEdge = material.eval(p - normal * inset);
            half4 inner = material.eval(p - normal * (distance + 10.0));
            float strength = taper * mix(0.72, 0.18, arc);
            float3 rim = mix(float3(nearEdge.rgb) / max(float(nearEdge.a), 0.001),
                float3(inner.rgb) / max(float(inner.a), 0.001), strength);
            float rimLight = (1.0 - smoothstep(0.25, 2.6, -distance)) * mix(0.10, 0.30, arc);
            rim = mix(rim, float3(1.0), rimLight);
            body = half4(rim * coverage, coverage);
        }
        if (frostAmount > 0.0) {
            body.rgb = half3(mix(float3(body.rgb), frostColor * body.a, frostAmount));
        }
        if (coverageGain < 1.0) body *= coverageGain;
        if (progress <= 0.002) return body;
        float r = lengthPx / maskRadius;
        float fall = 1.3 * progress;
        float spot = exp(-r * r * fall) * 1.4 * progress;
        float peak = 1.0 - pow(max(0.5 - 0.65 * r * fall, 0.0), 3.0);
        float ringA = 1.0 - smoothstep(0.0, 1.0, abs((r - 0.73) * 2.0));
        float ringB = 1.0 - smoothstep(0.0, 1.0, abs((r - 0.49) * 5.0));
        float sum = spot + (peak + ringA + ringB) * (0.08 * progress);
        float mask = clamp(sum * sum, 0.0, 1.0) * max(1.0 - pow(r, 8.0), 0.0);
        float amount = smoothstep(0.0, 1.0, mask * mask * 0.75);
        // The light brightens the glass body first; avoid clipping it to white at the rim.
        amount *= mix(0.65, 1.0, smoothstep(0.0, 12.0, -distance));
        float3 rgb = float3(body.rgb) / max(float(body.a), 0.001);
        float lum = dot(rgb, float3(0.2126, 0.7152, 0.0722));
        float tone = smoothstep(0.0, 0.5, lum);
        float offset = mix(1.0 - lum, lum, tone);
        float gain = 1.0 + amount * mix(0.75, 0.4, tone);
        return half4(clamp((rgb + offset) * gain - offset, 0.0, 1.0) * body.a, body.a);
    }
"""
