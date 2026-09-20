package com.googlecode.javaewah.aggregation;

import com.googlecode.javaewah.BitmapStorage;

/*
 * Copyright 2009-2016, Daniel Lemire, Cliff Moon, David McIntosh, Robert Becho, Google Inc., Veronika Zenz, Owen Kaser, Gregory Ssi-Yan-Kai, Rory Graves
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * {@link WordSink} forwarding to a 64-bit {@link BitmapStorage}.
 */
public final class BitmapStorageSink64 implements WordSink {

    private final BitmapStorage storage;

    /**
     * @param storage delegate storage
     */
    public BitmapStorageSink64(final BitmapStorage storage) {
        this.storage = storage;
    }

    @Override
    public void addWord(final long word) {
        this.storage.addWord(word);
    }

    @Override
    public void addStreamOfEmptyWords(final boolean v, final long number) {
        this.storage.addStreamOfEmptyWords(v, number);
    }

    /**
     * @return the wrapped storage
     */
    public BitmapStorage storage() {
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
