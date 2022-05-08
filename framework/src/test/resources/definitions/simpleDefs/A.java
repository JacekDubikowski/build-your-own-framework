package io.jd.framework.definitions;

import jakarta.inject.Singleton;

@Singleton
public class A {
    private final B b;
    private final C c;

    public A(B b, C c) {
        this.b = b;
        this.c = c;
    }
}
