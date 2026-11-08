package com.seth.worldbound.worldgen;

import java.util.concurrent.atomic.AtomicInteger;

/** Counters for /earth trees (tree density testing). */
public final class TreeStats {
    private TreeStats() {}
    public static final AtomicInteger SEEN = new AtomicInteger(), THINNED = new AtomicInteger(), EXTRA_TRY = new AtomicInteger(), EXTRA_OK = new AtomicInteger(), NOCOVER = new AtomicInteger(), FAILED = new AtomicInteger(), HIGH = new AtomicInteger();
}
