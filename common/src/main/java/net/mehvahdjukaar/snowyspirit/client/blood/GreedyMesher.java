package net.mehvahdjukaar.snowyspirit.client.blood;

/**
 * Greedy meshing over a boolean grid: merges the {@code true} cells of an NxN mask into a small set
 * of maximal axis-aligned rectangles, so a subdivided face emits a handful of quads instead of one
 * per cell.
 */
final class GreedyMesher {

    private GreedyMesher() {}

    @FunctionalInterface
    interface RectConsumer {
        /** A merged rectangle covering grid cells [i, i+w) x [j, j+h). */
        void accept(int i, int j, int w, int h);
    }

    static void mesh(boolean[][] mask, int n, RectConsumer out) {
        boolean[][] used = new boolean[n][n];
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                if (!mask[i][j] || used[i][j]) continue;
                int w = 1;
                while (i + w < n && mask[i + w][j] && !used[i + w][j]) w++;
                int h = 1;
                boolean grow = true;
                while (grow && j + h < n) {
                    for (int k = 0; k < w; k++) {
                        if (!mask[i + k][j + h] || used[i + k][j + h]) {
                            grow = false;
                            break;
                        }
                    }
                    if (grow) h++;
                }
                for (int dj = 0; dj < h; dj++) {
                    for (int di = 0; di < w; di++) used[i + di][j + dj] = true;
                }
                out.accept(i, j, w, h);
            }
        }
    }
}
