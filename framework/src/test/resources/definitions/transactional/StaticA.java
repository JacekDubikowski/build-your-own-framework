package io.jd.framework.definitions;

import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

@Singleton
public class StaticA {

    @Transactional
    static void save() {
    }

}
