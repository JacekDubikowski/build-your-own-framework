package io.jd.framework.definitions;

import jakarta.inject.Singleton;

@Singleton
public class TwoConstructors {
    private final int b;
    private final String c;

    public TwoConstructors(int b, String c) {
        this.b = b;
        this.c = c;
    }

    public TwoConstructors(int b) {
        super(b, "second constructor oh no!");
    }
}
