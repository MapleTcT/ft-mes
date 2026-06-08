/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.lang.Nullable
 */
package com.supcon.supfusion.framework.cloud.common.supports;

import java.io.Writer;
import org.springframework.lang.Nullable;

public class FastStringWriter
extends Writer {
    private StringBuilder builder;

    public FastStringWriter() {
        this.builder = new StringBuilder(64);
    }

    public FastStringWriter(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Negative builderfer size");
        }
        this.builder = new StringBuilder(capacity);
    }

    public FastStringWriter(@Nullable StringBuilder builder) {
        this.builder = builder != null ? builder : new StringBuilder(64);
    }

    public StringBuilder getBuilder() {
        return this.builder;
    }

    @Override
    public void write(int c) {
        this.builder.append((char)c);
    }

    @Override
    public void write(char[] cbuilder, int off, int len) {
        if (off < 0 || off > cbuilder.length || len < 0 || off + len > cbuilder.length || off + len < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return;
        }
        this.builder.append(cbuilder, off, len);
    }

    @Override
    public void write(String str) {
        this.builder.append(str);
    }

    @Override
    public void write(String str, int off, int len) {
        this.builder.append(str.substring(off, off + len));
    }

    @Override
    public FastStringWriter append(CharSequence csq) {
        if (csq == null) {
            this.write("null");
        } else {
            this.write(csq.toString());
        }
        return this;
    }

    @Override
    public FastStringWriter append(CharSequence csq, int start, int end) {
        CharSequence cs = csq == null ? "null" : csq;
        this.write(cs.subSequence(start, end).toString());
        return this;
    }

    @Override
    public FastStringWriter append(char c) {
        this.write(c);
        return this;
    }

    public String toString() {
        return this.builder.toString();
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
        this.builder.setLength(0);
        this.builder.trimToSize();
    }
}

