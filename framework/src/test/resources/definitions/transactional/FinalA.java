package io.jd.framework.definitions;

import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

@Singleton
public final class FinalA {

    @Transactional
    void sava() {}

}
