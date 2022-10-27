package io.jd.framework.definitions;

import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

@Singleton
public class A {

    @Transactional
    void save() {}

    @Transactional
    int read() {
        return 1;
    }

    @Transactional
    <T> T readGeneric() {
        return null;
    }

}
