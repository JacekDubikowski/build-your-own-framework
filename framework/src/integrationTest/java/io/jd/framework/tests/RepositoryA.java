package io.jd.framework.tests;

import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

@Singleton
public class RepositoryA {

    @Transactional
    void voidMethod() {
    }

    @Transactional
    int intMethod() {
        return 1;
    }
}
