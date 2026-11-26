package com.dtinh.lichviet;

/** Native design tokens measured from Miuix 0.9.4; no UI library dependency. */
final class MiuixMetrics {
    static final float CARD_RADIUS = 16f;
    static final float BUTTON_RADIUS = 16f;
    static final float DIALOG_RADIUS = 32f;
    static final float POPUP_RADIUS = 16f;
    static final float BUTTON_MIN_HEIGHT = 40f;
    static final float CONTENT_INSET = 16f;
    static final float BODY_TEXT = 17f;
    static final float SECONDARY_TEXT = 16f;
    static final float SWITCH_WIDTH = 49f;
    static final float SWITCH_HEIGHT = 28f;
    static final float SWITCH_THUMB = 20f;
    // SquirclePath.kt: extension 1.1, Bézier control 0.643.
    static float cornerTile(float width, float height, float radius) {
        return Math.max(0f, Math.min(radius * 1.1f, Math.min(width, height) * .5f));
    }
    static float cornerHandle(float tile) { return tile * (1f - .643f); }
    private MiuixMetrics() {}
}
