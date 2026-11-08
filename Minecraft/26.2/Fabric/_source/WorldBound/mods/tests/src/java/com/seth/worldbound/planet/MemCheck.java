package com.seth.worldbound.planet;

public final class MemCheck {
    public static void main(String[] a) throws Exception {
        Runtime rt = Runtime.getRuntime();
        System.gc(); Thread.sleep(300);
        long before = rt.totalMemory() - rt.freeMemory();
        Planet p = Planet.get(Long.parseLong(a[0]), null);
        for (int i = 0; i < 3; i++) { System.gc(); Thread.sleep(300); }
        long after = rt.totalMemory() - rt.freeMemory();
        System.out.printf("planet retained ~%d MB (rivers %d)%n", (after - before) >> 20, p.source.rivers.count);
        // a few z10 tiles more
        for (int t = 0; t < 4; t++) p.source.load(com.seth.worldbound.data.Tiling.LAYER_ELEV, 10, 800 + t, 300);
        for (int i = 0; i < 3; i++) { System.gc(); Thread.sleep(300); }
        System.out.printf("after 4 z10 tiles ~%d MB%n", ((rt.totalMemory() - rt.freeMemory()) - before) >> 20);
    }
}
