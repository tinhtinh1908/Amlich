package com.dtinh.lichviet;

/** One continuous sheet: hand-driven curl, release, then gravity. No particles or allocations. */
final class PaperTearGeometry {
    static final int COLUMNS = 20, ROWS = 36;
    static final int VERTICES = (COLUMNS + 1) * (ROWS + 1);
    static void fill(float width, float height, float progress, float drift,
                     float[] vertices, int[] shades) {
        float p = Math.max(0, Math.min(1, progress));
        float pull = smooth(Math.min(1, p / .55f));
        float release = Math.max(0, (p - .42f) / .58f);
        float curl = pull * (1 - .45f * smooth(release));
        float fold = height * (1 - .42f * curl);
        float radius = Math.max(1, height * .34f);
        float fall = height * 2.15f * release * release;
        float sideways = Math.max(-width * .4f, Math.min(width * .4f, drift));
        int index = 0;
        for (int row = 0; row <= ROWS; row++) {
            float y = height * row / ROWS;
            float angle = Math.max(0, (y - fold) / radius);
            float lift = radius * (1 - (float) Math.cos(angle));
            float bentY = angle == 0 ? y : fold + radius * (float) Math.sin(angle);
            int channel = Math.round(255 * (1 - .16f * (float) Math.sin(angle)));
            for (int col = 0; col <= COLUMNS; col++) {
                float x = width * col / COLUMNS;
                float perspective = 1 - lift / Math.max(1, height) * .16f;
                vertices[index * 2] = width / 2 + (x - width / 2) * perspective
                        + sideways * .28f * pull * (y / Math.max(1, height))
                        + sideways * .5f * release;
                vertices[index * 2 + 1] = bentY + fall
                        + height * .025f * pull * (x / Math.max(1, width))
                        * (1 - y / Math.max(1, height));
                shades[index++] = 0xff000000 | channel << 16 | channel << 8 | channel;
            }
        }
    }
    private static float smooth(float t) { return t * t * (3 - 2 * t); }
    private PaperTearGeometry() {}
}
