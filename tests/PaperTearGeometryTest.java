package com.dtinh.lichviet;

public final class PaperTearGeometryTest {
    public static void main(String[] args) {
        float[] vertices = new float[PaperTearGeometry.VERTICES * 2];
        int[] shades = new int[PaperTearGeometry.VERTICES];
        int frames = 0;
        for (float width : new float[]{240, 385, 720}) {
            for (float height : new float[]{400, 780, 1200}) {
                PaperTearGeometry.fill(width, height, 0, 0, vertices, shades);
                for (int y = 0; y <= PaperTearGeometry.ROWS; y++) {
                    for (int x = 0; x <= PaperTearGeometry.COLUMNS; x++) {
                        int i = (y * (PaperTearGeometry.COLUMNS + 1) + x) * 2;
                        if (Math.abs(vertices[i] - width * x / PaperTearGeometry.COLUMNS) > .001f
                                || Math.abs(vertices[i + 1] - height * y / PaperTearGeometry.ROWS) > .001f)
                            throw new AssertionError("Flat sheet endpoint");
                    }
                }
                for (int f = 0; f <= 100; f++) {
                    PaperTearGeometry.fill(width, height, f / 100f, width * .25f, vertices, shades);
                    for (int i = 0; i < shades.length; i++) {
                        if (!Float.isFinite(vertices[i * 2]) || !Float.isFinite(vertices[i * 2 + 1]))
                            throw new AssertionError("Nonfinite mesh");
                        if ((shades[i] & 255) < 196 || (shades[i] >>> 24) != 255)
                            throw new AssertionError("Invalid shading");
                        if (f == 100 && vertices[i * 2 + 1] <= height)
                            throw new AssertionError("Detached sheet must leave the page");
                    }
                    for (int col = 0; col <= PaperTearGeometry.COLUMNS; col++) {
                        for (int row = 1; row <= PaperTearGeometry.ROWS; row++) {
                            int current = (row * (PaperTearGeometry.COLUMNS + 1) + col) * 2 + 1;
                            int previous = current - (PaperTearGeometry.COLUMNS + 1) * 2;
                            if (vertices[current] <= vertices[previous])
                                throw new AssertionError("Continuous sheet must not fold through itself");
                        }
                    }
                    frames++;
                }
            }
        }
        System.out.println("Paper tear: " + frames + " frames, flat/curl/fall endpoints and finite mesh passed");
    }
}
