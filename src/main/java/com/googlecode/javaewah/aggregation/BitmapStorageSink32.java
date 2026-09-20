package com.googlecode.javaewah.aggregation;

import com.googlecode.javaewah32.BitmapStorage32;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * {@link WordSink} forwarding to a 32-bit {@link BitmapStorage32}. The low 32
 * bits of each word are written as an {@code int}.
 */
public final class BitmapStorageSink32 implements WordSink {

    private final BitmapStorage32 storage;

    /**
     * @param storage delegate storage
     */
    public BitmapStorageSink32(final BitmapStorage32 storage) {
        this.storage = storage;
    }

    @Override
    public void addWord(final long word) {
        this.storage.addWord((int) word);
    }

    @Override
    public void addStreamOfEmptyWords(final boolean v, final long number) {
        this.storage.addStreamOfEmptyWords(v, (int) number);
    }

    /**
     * @return the wrapped storage
     */
    public BitmapStorage32 storage() {
        return this.storage;
    }

    @Override
    public void clear() {
        this.storage.clear();
    }

    @Override
    public void setSizeInBitsWithinLastWord(final int size) {
        this.storage.setSizeInBitsWithinLastWord(size);
    }
}
